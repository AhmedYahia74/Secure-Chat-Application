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
      console.log('Attempting to connect to WebSocket...');

      const socket = new SockJS('http://localhost:8080/chat');

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'heart-beat': '10000,10000'
        },
        debug: (str) => {
          console.log('STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Connected to WebSocket');
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.connectionStatus.next('Connected');

          // Subscribe to user list updates
          this.stompClient?.subscribe('/topic/users', (message: IMessage) => {
            console.log('Received user list update:', message);
            let users: User[];
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                console.log('Decoded binary user list:', text);
                users = JSON.parse(text);
              } else {
                users = JSON.parse(message.body);
              }
              // Filter out the current user from the list
              const filteredUsers = users.filter(user => user.username !== this.currentUser);
              console.log('Filtered users:', filteredUsers);
              this.usersSubject.next(filteredUsers);
            } catch (error) {
              console.error('Error processing user list:', error);
            }
          });

          // Subscribe to public key updates
          this.stompClient?.subscribe('/topic/public-keys', (message: IMessage) => {
            console.log('Received public key update:', message);
            let userData;
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                console.log('Decoded binary public key:', text);
                userData = JSON.parse(text);
              } else {
                userData = JSON.parse(message.body);
              }
              console.log('Parsed user data:', userData);
              if (userData.username !== this.currentUser) {
                this.encryptionService.generateSharedKey(userData.publicKey, userData.username)
                  .catch(error => console.error('Error generating shared key:', error));
                console.log('Generated shared key for user:', userData.username);
              }
            } catch (error) {
              console.error('Error processing public key update:', error);
            }
          });

          // Subscribe to broadcast messages
          this.stompClient?.subscribe('/topic/messages', (message: IMessage) => {
            console.log('Received raw message:', message);
            let msg: Message;
            try {
              if (message.isBinaryBody) {
                const decoder = new TextDecoder('utf-8');
                const text = decoder.decode(message.binaryBody);
                console.log('Decoded binary message:', text);
                msg = JSON.parse(text);
              } else {
                console.log('Received text message:', message.body);
                const parsedMsg = typeof message.body === 'string' ? JSON.parse(message.body) : message.body;
                console.log('Parsed message:', parsedMsg);

                // Convert backend message format to frontend format
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
              console.log('Processed message:', msg);

              // Only decrypt and display if this user is the receiver
              if (msg.receiver === this.currentUser) {
                console.log('Processing message for current user:', this.currentUser);
                // Parse the sender's public key from JSON string
                const senderPublicKey = typeof msg.senderPublicKey === 'string'
                  ? JSON.parse(msg.senderPublicKey)
                  : msg.senderPublicKey;
                console.log('Sender public key parsed:', senderPublicKey);

                this.encryptionService.generateSharedKey(senderPublicKey, msg.sender)
                  .then(async () => {
                    const sharedKey = this.encryptionService.getSharedKey(msg.sender);
                    if (!sharedKey) {
                      console.error('Shared key not found for sender:', msg.sender);
                      throw new Error('Shared key not found for sender');
                    }
                    console.log('Retrieved shared key for sender:', msg.sender);

                    // Convert string arrays back to Uint8Array
                    let encryptedContent: Uint8Array;
                    let iv: Uint8Array;

                    try {
                      console.log('Converting message data:', {
                        encryptedContentType: typeof msg.encryptedContent,
                        ivType: typeof msg.iv,
                        encryptedContent: msg.encryptedContent,
                        iv: msg.iv
                      });

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

                      console.log('Converted message data:', {
                        encryptedContentLength: encryptedContent.length,
                        ivLength: iv.length,
                        encryptedContentType: encryptedContent.constructor.name,
                        ivType: iv.constructor.name
                      });
                    } catch (error) {
                      console.error('Error converting message data:', error);
                      console.log('Message data:', {
                        encryptedContent: msg.encryptedContent,
                        iv: msg.iv
                      });
                      throw new Error('Invalid message data format');
                    }

                    console.log('Attempting to decrypt message with:', {
                      encryptedContentLength: encryptedContent.length,
                      ivLength: iv.length,
                      hasSharedKey: !!sharedKey,
                      encryptedContentType: encryptedContent.constructor.name,
                      ivType: iv.constructor.name
                    });

                    try {
                      const decryptedContent = await this.encryptionService.decryptMessage(
                        encryptedContent,
                        iv,
                        sharedKey
                      );
                      console.log('Successfully decrypted content:', decryptedContent);

                      const decryptedMsg = {
                        ...msg,
                        content: decryptedContent,
                        timestamp: new Date(),
                        isUser: false
                      };
                      console.log('Prepared decrypted message:', decryptedMsg);

                      const currentMessages = this.messageSubject.value;
                      console.log('Current messages count:', currentMessages.length);
                      console.log('Current messages:', currentMessages);

                      const updatedMessages = [...currentMessages, decryptedMsg];
                      console.log('Updated messages array:', updatedMessages);

                      this.messageSubject.next(updatedMessages);
                      console.log('Updated messages array, new count:', this.messageSubject.value.length);
                      console.log('Updated messages array content:', this.messageSubject.value);
                    } catch (error) {
                      console.error('Error decrypting message:', error);
                      console.log('Decryption failed with data:', {
                        encryptedContent: Array.from(encryptedContent),
                        iv: Array.from(iv),
                        messageType: msg.constructor.name
                      });
                      throw error;
                    }
                  })
                  .catch(error => {
                    console.error('Error in message processing:', error);
                    this.connectionStatus.next('Error');
                  });
              } else {
                console.log('Message not for current user:', {
                  messageReceiver: msg.receiver,
                  currentUser: this.currentUser
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
          console.log('WebSocket connection closed');
          this.handleConnectionError();
        }
      });

      console.log('Activating STOMP client...');
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
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      this.scheduleReconnect();
    } else {
      console.error('Max reconnection attempts reached');
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

    // Unsubscribe previous if exists
    if (this.personalMessageSubscription) {
      this.personalMessageSubscription.unsubscribe();
    }
  }

  connectToUser(username: string): void {
    if (!this.stompClient?.connected) {
      throw new Error('Not connected to WebSocket');
    }

    this.currentReceiver = username;

    // Request public key for the selected user
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
      console.log('Getting shared key for receiver:', this.currentReceiver);
      const sharedKey = this.encryptionService.getSharedKey(this.currentReceiver);
      if (!sharedKey) {
        console.error('Shared key not found for receiver:', this.currentReceiver);
        throw new Error('Shared key not found for receiver');
      }
      console.log('Found shared key for receiver:', this.currentReceiver);

      this.encryptionService.encryptMessage(content, sharedKey)
        .then(async ({ iv, ciphertext }) => {
          console.log('Message encrypted successfully:', {
            contentLength: content.length,
            ivLength: iv.length,
            ciphertextLength: ciphertext.length
          });

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

          console.log('Sending message:', msg);
          this.stompClient?.publish({
            destination: '/app/chat',
            body: JSON.stringify(msg),
            headers: { 'content-type': 'application/json' }
          });
          console.log('Message sent successfully');

          // Add message to local messages array
          const currentMessages = this.messageSubject.value;
          console.log('Current messages before adding:', currentMessages);
          const updatedMessages = [...currentMessages, msg];
          console.log('Updated messages array:', updatedMessages);
          this.messageSubject.next(updatedMessages);
          console.log('Messages after adding:', this.messageSubject.value);
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
      console.log('Disconnecting STOMP client...');
      this.stompClient.deactivate();
    }
  }
} 