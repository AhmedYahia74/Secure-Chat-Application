import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class EncryptionService {
  private keyPair: CryptoKeyPair | null = null;
  private sharedKeys: Map<string, CryptoKey> = new Map();
  private publicKeyJwk: JsonWebKey | null = null;

  constructor() {
    this.initializeKeyPair();
  }

  private async initializeKeyPair(): Promise<void> {
    this.keyPair = await this.generateECDHKeyPair();
    this.publicKeyJwk = await this.exportPublicKey(this.keyPair.publicKey);
  }

  // 1. Generate ECDH key pair (public and private keys)
  async generateECDHKeyPair(): Promise<CryptoKeyPair> {
    return await crypto.subtle.generateKey(
      {
        name: 'ECDH',
        namedCurve: 'P-256', // ECDH P-256 curve
      },
      true, // extractable = true, meaning we can export the key
      ['deriveKey'] // Only allow deriving keys with this key
    );
  }

  // 2. Export the public key to send to others
  async exportPublicKey(publicKey: CryptoKey): Promise<JsonWebKey> {
    return await crypto.subtle.exportKey('jwk', publicKey); // Export the public key as JSON
  }

  // 3. Import another user's public key from JSON (to derive shared secret)
  async importPublicKey(jwk: JsonWebKey): Promise<CryptoKey> {
    console.log('Importing public key:', jwk);
    return await crypto.subtle.importKey(
      'jwk',
      jwk,
      { name: 'ECDH', namedCurve: 'P-256' },
      true,
      []
    );
  }

  // 4. Derive a shared AES key using your private key and their public key
  async deriveSharedKey(
    myPrivateKey: CryptoKey,
    theirPublicKey: CryptoKey
  ): Promise<CryptoKey> {
    console.log('Deriving shared key with:', {
      hasPrivateKey: !!myPrivateKey,
      hasPublicKey: !!theirPublicKey,
      privateKeyType: myPrivateKey?.type,
      publicKeyType: theirPublicKey?.type,
      privateKeyAlgorithm: myPrivateKey?.algorithm,
      publicKeyAlgorithm: theirPublicKey?.algorithm
    });

    try {
      const sharedKey = await crypto.subtle.deriveKey(
        {
          name: 'ECDH',
          public: theirPublicKey,
        },
        myPrivateKey,
        {
          name: 'AES-GCM',
          length: 256,
        },
        false,
        ['encrypt', 'decrypt']
      );

      console.log('Successfully derived shared key:', {
        type: sharedKey.type,
        algorithm: sharedKey.algorithm,
        extractable: sharedKey.extractable,
        usages: sharedKey.usages
      });

      return sharedKey;
    } catch (error) {
      console.error('Error deriving shared key:', error);
      throw error;
    }
  }

  // 5. Encrypt a message with the shared AES key and an IV (Initialization Vector)
  async encryptMessage(
    plainText: string,
    sharedKey: CryptoKey
  ): Promise<{ iv: Uint8Array; ciphertext: Uint8Array }> {
    console.log('Encrypting message:', {
      plainTextLength: plainText.length,
      hasSharedKey: !!sharedKey
    });
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encoded = new TextEncoder().encode(plainText);

    const encrypted = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      sharedKey,
      encoded
    );

    console.log('Encryption result:', {
      ivLength: iv.length,
      ciphertextLength: encrypted.byteLength
    });

    return {
      iv,
      ciphertext: new Uint8Array(encrypted),
    };
  }

  // 6. Decrypt a message with the shared AES key and the IV (Initialization Vector)
  async decryptMessage(
    encrypted: Uint8Array,
    iv: Uint8Array,
    sharedKey: CryptoKey
  ): Promise<string> {
    console.log('Decrypting message:', {
      encryptedLength: encrypted.length,
      ivLength: iv.length,
      hasSharedKey: !!sharedKey,
      encryptedType: encrypted.constructor.name,
      ivType: iv.constructor.name,
      sharedKeyType: sharedKey.constructor.name
    });

    if (!encrypted || encrypted.length === 0) {
      throw new Error('Encrypted data is empty or invalid');
    }

    if (!iv || iv.length !== 12) {
      throw new Error(`Invalid IV length: ${iv?.length}, expected 12`);
    }

    if (!sharedKey) {
      throw new Error('Shared key is missing');
    }

    try {
      const decrypted = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv },
        sharedKey,
        encrypted
      );

      console.log('Decryption successful, decoded length:', decrypted.byteLength);
      return new TextDecoder().decode(decrypted);
    } catch (error) {
      console.error('Decryption error details:', {
        error,
        encryptedType: encrypted.constructor.name,
        ivType: iv.constructor.name,
        sharedKeyType: sharedKey.constructor.name,
        encryptedLength: encrypted.length,
        ivLength: iv.length
      });
      throw error;
    }
  }

  // New methods for key management
  getPublicKey(): JsonWebKey {
    if (!this.publicKeyJwk) {
      throw new Error('Public key not initialized');
    }
    return this.publicKeyJwk;
  }

  async generateSharedKey(publicKeyJwk: JsonWebKey, username: string): Promise<void> {
    if (!this.keyPair?.privateKey) {
      throw new Error('Private key not initialized');
    }

    try {
      const theirPublicKey = await this.importPublicKey(publicKeyJwk);
      const sharedKey = await this.deriveSharedKey(this.keyPair.privateKey, theirPublicKey);
      this.sharedKeys.set(username, sharedKey);
    } catch (error) {
      console.error('Error generating shared key:', error);
      throw error;
    }
  }

  getSharedKey(username: string): CryptoKey | undefined {
    return this.sharedKeys.get(username);
  }
}
