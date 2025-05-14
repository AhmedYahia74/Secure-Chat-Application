export interface User {
  username: string;
  publicKey: JsonWebKey | string;  // JsonWebKey or string
}

export interface Message {
  sender: string;
  receiver: string;
  content: string;
  encryptedContent: number[] | string;  // Array of numbers or string
  iv: number[] | string;               // Array of numbers or string
  senderPublicKey: JsonWebKey | string;  // JsonWebKey or string
  timestamp: Date;
  isUser?: boolean;
}

export interface ConnectionRequest {
  username: string;
  publicKey: JsonWebKey;
}

export interface ConnectionResponse {
  username: string;
  publicKey: JsonWebKey;
} 