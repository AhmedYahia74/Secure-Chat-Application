export interface User {
  username: string;
  publicKey: string;
}

export interface Message {
  sender: string;
  receiver: string;
  content: string;
  encryptedContent: string;
  senderPublicKey: string;
  timestamp: Date;
  isUser?: boolean;
}

export interface ConnectionRequest {
  from: string;
  to: string;
  publicKey: string;
}

export interface ConnectionResponse {
  accepted: boolean;
  publicKey: string;
} 