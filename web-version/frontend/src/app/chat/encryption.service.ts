import { Injectable } from '@angular/core';
import * as CryptoJS from 'crypto-js';
import { Buffer } from 'buffer';

@Injectable({
  providedIn: 'root'
})
export class EncryptionService {
  private privateKey: string = '';
  private publicKey: string = '';
  private sharedKeys: Map<string, string> = new Map();

  constructor() {
    this.generateKeyPair();
  }

  private generateKeyPair(): void {
    // Generate a random private key
    this.privateKey = CryptoJS.lib.WordArray.random(32).toString();
    // In a real implementation, this would use proper DH key generation
    this.publicKey = CryptoJS.SHA256(this.privateKey).toString();
  }

  getPublicKey(): string {
    return this.publicKey;
  }

  generateSharedKey(peerPublicKey: string, peerUsername: string): void {
    // In a real implementation, this would use proper DH key exchange
    const sharedKey = CryptoJS.SHA256(this.privateKey + peerPublicKey).toString();
    this.sharedKeys.set(peerUsername, sharedKey);
  }

  encryptMessage(message: string, peerUsername: string): string {
    const sharedKey = this.sharedKeys.get(peerUsername);
    if (!sharedKey) {
      throw new Error('No shared key found for peer');
    }
    return CryptoJS.AES.encrypt(message, sharedKey).toString();
  }

  decryptMessage(encryptedMessage: string, peerUsername: string): string {
    const sharedKey = this.sharedKeys.get(peerUsername);
    if (!sharedKey) {
      throw new Error('No shared key found for peer');
    }
    const bytes = CryptoJS.AES.decrypt(encryptedMessage, sharedKey);
    return bytes.toString(CryptoJS.enc.Utf8);
  }
} 