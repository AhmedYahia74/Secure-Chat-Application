import { Component, OnInit, OnDestroy, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from './chat.service';
import { Subscription } from 'rxjs';

interface Message {
  sender: string;
  content: string;
  timestamp: Date;
  isUser: boolean;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent implements OnInit, OnDestroy {
  messages: Message[] = [];
  sender: string = '';
  content: string = '';
  status: string = 'Disconnected';
  error: string = '';
  showChat: boolean = false;

  private messagesSubscription: Subscription | null = null;
  private statusSubscription: Subscription | null = null;

  constructor(
    private chatService: ChatService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.messagesSubscription = this.chatService.getMessages().subscribe(
      messages => {
        this.messages = messages.map(msg => ({
          ...msg,
          timestamp: new Date(),
          isUser: msg.sender === this.sender
        }));
        if (isPlatformBrowser(this.platformId)) {
          setTimeout(() => {
            const messagesDiv = document.querySelector('.chat-messages');
            if (messagesDiv) {
              messagesDiv.scrollTop = messagesDiv.scrollHeight;
            }
          });
        }
      }
    );

    this.statusSubscription = this.chatService.getConnectionStatus().subscribe(
      status => {
        this.status = status;
        if (status === 'Error') {
          this.error = 'Connection error. Attempting to reconnect...';
        } else if (status === 'Connected') {
          this.error = '';
        }
      }
    );
  }

  startChat(): void {
    if (!this.sender.trim()) {
      this.error = 'Please enter a username';
      return;
    }
    this.showChat = true;
    this.error = '';
  }

  sendMessage(): void {
    if (!this.content.trim()) {
      this.error = 'Message cannot be empty';
      return;
    }
    if (this.status !== 'Connected') {
      this.error = 'Not connected to chat server';
      return;
    }
    this.chatService.sendMessage(this.sender, this.content);
    this.content = '';
    this.error = '';
  }

  ngOnDestroy(): void {
    if (this.messagesSubscription) {
      this.messagesSubscription.unsubscribe();
    }
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
    }
  }
}
