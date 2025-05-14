import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new BehaviorSubject<{ sender: string; content: string }[]>([]);
  private connectionStatus = new BehaviorSubject<string>('Disconnected');
  private isConnecting = false;
  private reconnectTimeout: any;

  constructor() {
    this.connect();
  }

  private connect(): void {
    if (this.isConnecting || this.stompClient?.connected) {
      return;
    }

    try {
      this.isConnecting = true;
      console.log('Attempting to connect to WebSocket...');
      
      this.stompClient = new Client({
        brokerURL: 'ws://localhost:8080/chat',
        debug: (str) => {
          console.log('STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Connected to WebSocket');
          this.isConnecting = false;
          this.connectionStatus.next('Connected');
          this.stompClient?.subscribe('/topic/messages', (message: IMessage) => {
            console.log('Received message:', message);
            // decrypt
            const msg = JSON.parse(message.body);
            const currentMessages = this.messageSubject.value;
            this.messageSubject.next([...currentMessages, msg]);
          });
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.isConnecting = false;
          this.connectionStatus.next('Error');
          this.scheduleReconnect();
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.isConnecting = false;
          this.connectionStatus.next('Error');
          this.scheduleReconnect();
        },
        onWebSocketClose: () => {
          console.log('WebSocket connection closed');
          this.isConnecting = false;
          this.connectionStatus.next('Disconnected');
          this.scheduleReconnect();
        }
      });

      console.log('Activating STOMP client...');
      this.stompClient.activate();
    } catch (e) {
      console.error('Connection error:', e);
      this.isConnecting = false;
      this.connectionStatus.next('Error');
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    this.reconnectTimeout = setTimeout(() => {
      console.log('Attempting to reconnect...');
      this.connect();
    }, 5000);
  }

  sendMessage(sender: string, content: string): void {
    if (!this.stompClient?.connected) {
      console.error('Not connected to WebSocket');
      this.connectionStatus.next('Error');
      return;
    }
    // encrypt
    const msg = {
      sender: sender,
      content: content.trim()
    };
    
    try {
      this.stompClient.publish({
        destination: '/app/chat',
        body: JSON.stringify(msg),
        headers: {
          'content-type': 'application/json'
        }
      });
      console.log('Message sent successfully');
    } catch (error) {
      console.error('Error sending message:', error);
      this.connectionStatus.next('Error');
    }
  }

  getMessages(): Observable<{ sender: string; content: string }[]> {
    return this.messageSubject.asObservable();
  }

  getConnectionStatus(): Observable<string> {
    return this.connectionStatus.asObservable();
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