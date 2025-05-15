import { Component, OnInit, OnDestroy, PLATFORM_ID, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from './chat.service';
import { Subscription } from 'rxjs';
import { Message, User } from './models';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent implements OnInit, OnDestroy {
  messages: Message[] = [];
  users: User[] = [];
  username: string = '';
  content: string = '';
  status: string = 'Disconnected';
  error: string = '';
  showChat: boolean = false;
  selectedUser: string = '';

  private messagesSubscription: Subscription | null = null;
  private statusSubscription: Subscription | null = null;
  private usersSubscription: Subscription | null = null;

  constructor(
    private chatService: ChatService,
    @Inject(PLATFORM_ID) private platformId: Object,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.messagesSubscription = this.chatService.getMessages().subscribe({
      next: (conversations) => {
        if (this.selectedUser) {
          this.chatService.getConversationMessages(this.username, this.selectedUser)
            .subscribe(messages => {
              this.messages = messages.sort((a, b) =>
                new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
              );
              this.cdr.detectChanges();
              this.scrollToBottom();
            });
        }
      },
      error: (error) => {
        console.error('Error in messages subscription:', error);
        this.error = 'Error receiving messages';
      }
    });

    this.statusSubscription = this.chatService.getConnectionStatus().subscribe(
      status => {
        this.status = status;
        if (status === 'Error') {
          this.error = 'Connection error. Attempting to reconnect...';
        } else if (status === 'Connected') {
          this.error = '';
        }
        this.cdr.detectChanges();
      }
    );

    this.usersSubscription = this.chatService.getUsers().subscribe(
      users => {
        this.users = users;
        this.cdr.detectChanges();
      }
    );
  }

  private scrollToBottom(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        const messagesDiv = document.querySelector('.chat-messages');
        if (messagesDiv) {
          messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
      });
    }
  }

  startChat(): void {
    if (!this.username.trim()) {
      this.error = 'Please enter a username';
      return;
    }
    try {
      console.log('Starting chat with username:', this.username);
      this.chatService.registerUser(this.username);
      this.showChat = true;
      this.error = '';
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Failed to register user:', error);
      this.error = 'Failed to register user';
    }
  }

  connectToUser(username: string): void {
    try {
      console.log('Connecting to user:', username);
      this.chatService.connectToUser(username);
      this.selectedUser = username;
      this.error = '';

      // Load conversation messages when selecting a user
      this.chatService.getConversationMessages(this.username, username)
        .subscribe(messages => {
          this.messages = messages.sort((a, b) =>
            new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
          );
          this.cdr.detectChanges();
          this.scrollToBottom();
        });
    } catch (error) {
      console.error('Failed to connect to user:', error);
      this.error = 'Failed to connect to user';
    }
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
    if (!this.selectedUser) {
      this.error = 'Please select a user to chat with';
      return;
    }

    try {
      console.log('Sending message to:', this.selectedUser);
      this.chatService.sendMessage(this.content);
      this.content = '';
      this.error = '';
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Failed to send message:', error);
      this.error = 'Failed to send message';
    }
  }

  ngOnDestroy(): void {
    console.log('Destroying chat component');
    if (this.messagesSubscription) {
      this.messagesSubscription.unsubscribe();
    }
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
    }
    if (this.usersSubscription) {
      this.usersSubscription.unsubscribe();
    }
  }
}
