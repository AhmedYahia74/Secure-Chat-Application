import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { EncryptionService } from './encryption.service';
import { Message, User } from './models';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new BehaviorSubject<Message[]>([]);
  private connectionStatus = new BehaviorSubject<string>('Disconnected');
  private usersSubject = new BehaviorSubject<User[]>([]);
  private currentUser: string = '';
  private currentReceiver: string = '';
  private isConnecting = false;
  private reconnectTimeout: any;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private personalMessageSubscription: any = null;

  constructor(private encryptionService: EncryptionService) {
    this.connect();
  }

  private connect(): void {
    if (this.isConnecting || this.stompClient?.connected) {
      return;
    }

    try {
      this.isConnecting = true;
      const socket = new SockJS('http://localhost:8080/chat');

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'heart-beat': '10000,10000'
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.connectionStatus.next('Connected');

          this.stompClient?.subscribe('/topic/users', (message: IMessage) => {
            let users: User[];
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                users = JSON.parse(text);
              } else {
                users = JSON.parse(message.body);
              }
              const filteredUsers = users.filter(user => user.username !== this.currentUser);
              this.usersSubject.next(filteredUsers);
            } catch (error) {
              console.error('Error processing user list:', error);
            }
          });

          this.stompClient?.subscribe('/topic/public-keys', (message: IMessage) => {
            let userData;
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                userData = JSON.parse(text);
              } else {
                userData = JSON.parse(message.body);
              }
              if (userData.username !== this.currentUser) {
                this.encryptionService.generateSharedKey(userData.publicKey, userData.username)
                  .catch(error => console.error('Error generating shared key:', error));
              }
            } catch (error) {
              console.error('Error processing public key update:', error);
            }
          });

          this.stompClient?.subscribe('/topic/messages', (message: IMessage) => {
            let msg: Message;
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                msg = JSON.parse(text);
              } else {
                const parsedMsg = typeof message.body === 'string' ? JSON.parse(message.body) : message.body;
                msg = {
                  ...parsedMsg,
                  encryptedContent: typeof parsedMsg.encryptedContent === 'string'
                    ? JSON.parse(parsedMsg.encryptedContent)
                    : parsedMsg.encryptedContent,
                  iv: typeof parsedMsg.iv === 'string'
                    ? JSON.parse(parsedMsg.iv)
                    : parsedMsg.iv,
                  senderPublicKey: typeof parsedMsg.senderPublicKey === 'string'
                    ? JSON.parse(parsedMsg.senderPublicKey)
                    : parsedMsg.senderPublicKey,
                  timestamp: new Date(parsedMsg.timestamp),
                  isUser: parsedMsg.isUser || false
                };
              }

              if (msg.receiver === this.currentUser) {
                const senderPublicKey = typeof msg.senderPublicKey === 'string'
                  ? JSON.parse(msg.senderPublicKey)
                  : msg.senderPublicKey;

                this.encryptionService.generateSharedKey(senderPublicKey, msg.sender)
                  .then(async () => {
                    const sharedKey = this.encryptionService.getSharedKey(msg.sender);
                    if (!sharedKey) {
                      throw new Error('Shared key not found for sender');
                    }

                    let encryptedContent: Uint8Array;
                    let iv: Uint8Array;

                    try {
                      encryptedContent = new Uint8Array(
                        typeof msg.encryptedContent === 'string'
                          ? JSON.parse(msg.encryptedContent)
                          : msg.encryptedContent
                      );
                      iv = new Uint8Array(
                        typeof msg.iv === 'string'
                          ? JSON.parse(msg.iv)
                          : msg.iv
                      );
                    } catch (error) {
                      throw new Error('Invalid message data format');
                    }

                    try {
                      const decryptedContent = await this.encryptionService.decryptMessage(
                        encryptedContent,
                        iv,
                        sharedKey
                      );

                      const decryptedMsg = {
                        ...msg,
                        content: decryptedContent,
                        timestamp: new Date(),
                        isUser: false
                      };

                      const currentMessages = this.messageSubject.value;
                      const updatedMessages = [...currentMessages, decryptedMsg];
                      this.messageSubject.next(updatedMessages);
                    } catch (error) {
                      console.error('Error decrypting message:', error);
                      throw error;
                    }
                  })
                  .catch(error => {
                    console.error('Error in message processing:', error);
                    this.connectionStatus.next('Error');
                  });
              }
            } catch (error) {
              console.error('Error processing broadcast message:', error);
            }
          });
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.handleConnectionError();
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.handleConnectionError();
        },
        onWebSocketClose: () => {
          this.handleConnectionError();
        }
      });

      this.stompClient.activate();
    } catch (e) {
      console.error('Connection error:', e);
      this.handleConnectionError();
    }
  }

  private handleConnectionError(): void {
    this.isConnecting = false;
    this.connectionStatus.next('Error');

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      this.scheduleReconnect();
    } else {
      this.connectionStatus.next('Failed to connect');
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    this.reconnectTimeout = setTimeout(() => {
      this.connect();
    }, 5000);
  }

  registerUser(username: string): void {
    if (!this.stompClient?.connected) {
      throw new Error('Not connected to WebSocket');
    }

    const publicKey = this.encryptionService.getPublicKey();
    this.currentUser = username;

    this.stompClient.publish({
      destination: '/app/register',
      body: JSON.stringify({
        username,
        publicKey: JSON.stringify(publicKey)
      }),
      headers: { 'content-type': 'application/json' }
    });

    if (this.personalMessageSubscription) {
      this.personalMessageSubscription.unsubscribe();
    }
  }

  connectToUser(username: string): void {
    if (!this.stompClient?.connected) {
      throw new Error('Not connected to WebSocket');
    }

    this.currentReceiver = username;

    this.stompClient.publish({
      destination: '/app/request-public-key',
      body: JSON.stringify({ username }),
      headers: { 'content-type': 'application/json' }
    });
  }

  sendMessage(content: string): void {
    if (!this.stompClient?.connected) {
      throw new Error('Not connected to WebSocket');
    }

    if (!this.currentReceiver) {
      throw new Error('No receiver selected');
    }

    try {
      const sharedKey = this.encryptionService.getSharedKey(this.currentReceiver);
      if (!sharedKey) {
        throw new Error('Shared key not found for receiver');
      }

      this.encryptionService.encryptMessage(content, sharedKey)
        .then(async ({ iv, ciphertext }) => {
          const msg = {
            sender: this.currentUser,
            receiver: this.currentReceiver,
            content: content.trim(),
            encryptedContent: Array.from(ciphertext),
            iv: Array.from(iv),
            senderPublicKey: JSON.stringify(this.encryptionService.getPublicKey()),
            timestamp: new Date(),
            isUser: true
          };

          this.stompClient?.publish({
            destination: '/app/chat',
            body: JSON.stringify(msg),
            headers: { 'content-type': 'application/json' }
          });

          const currentMessages = this.messageSubject.value;
          const updatedMessages = [...currentMessages, msg];
          this.messageSubject.next(updatedMessages);
        })
        .catch(error => {
          console.error('Error encrypting message:', error);
          this.connectionStatus.next('Error');
        });
    } catch (error) {
      console.error('Error sending message:', error);
      this.connectionStatus.next('Error');
    }
  }

  getMessages(): Observable<Message[]> {
    return this.messageSubject.asObservable();
  }

  getUsers(): Observable<User[]> {
    return this.usersSubject.asObservable();
  }

  getConnectionStatus(): Observable<string> {
    return this.connectionStatus.asObservable();
  }

  getCurrentReceiver(): string {
    return this.currentReceiver;
  }

  disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }
} 