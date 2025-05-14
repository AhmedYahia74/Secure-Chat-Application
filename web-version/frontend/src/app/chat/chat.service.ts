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
          
          // Subscribe to personal messages
          this.stompClient?.subscribe(`/user/${this.currentUser}/queue/messages`, (message: IMessage) => {
            console.log('Received raw message:', message);
            const msg = JSON.parse(message.body);
            console.log('Parsed message:', msg);
            try {
              const decryptedContent = this.encryptionService.decryptMessage(
                msg.encryptedContent,
                msg.sender
              );
              console.log('Decrypted content:', decryptedContent);
              const decryptedMsg = {
                ...msg,
                content: decryptedContent,
                timestamp: new Date(),
                isUser: msg.sender === this.currentUser
              };
              console.log('Final message object:', decryptedMsg);
              const currentMessages = this.messageSubject.value;
              console.log('Current messages:', currentMessages);
              this.messageSubject.next([...currentMessages, decryptedMsg]);
              console.log('Updated messages:', this.messageSubject.value);
            } catch (error) {
              console.error('Error decrypting message:', error);
            }
          });

          // Subscribe to user list updates
          this.stompClient?.subscribe('/topic/users', (message: IMessage) => {
            console.log('Received user list update:', message);
            const users = JSON.parse(message.body);
            this.usersSubject.next(users);
          });

          // Subscribe to public key updates
          this.stompClient?.subscribe('/topic/public-keys', (message: IMessage) => {
            console.log('Received public key update:', message);
            const userData = JSON.parse(message.body);
            if (userData.username !== this.currentUser) {
              this.encryptionService.generateSharedKey(userData.publicKey, userData.username);
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
      body: JSON.stringify({ username, publicKey }),
      headers: { 'content-type': 'application/json' }
    });
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
      const encryptedContent = this.encryptionService.encryptMessage(content, this.currentReceiver);
      const msg = {
        sender: this.currentUser,
        receiver: this.currentReceiver,
        content: content.trim(),
        encryptedContent,
        senderPublicKey: this.encryptionService.getPublicKey(),
        timestamp: new Date()
      };
      
      console.log('Sending message:', msg);
      this.stompClient.publish({
        destination: '/app/chat',
        body: JSON.stringify(msg),
        headers: { 'content-type': 'application/json' }
      });
      console.log('Message sent successfully');
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