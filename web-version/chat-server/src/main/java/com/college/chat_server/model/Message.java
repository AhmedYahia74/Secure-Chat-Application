package com.college.chat_server.model;

import lombok.Data;
import java.util.Date;

@Data
public class Message {
    private String sender;
    private String receiver;
    private String content;
    private String encryptedContent;
    private String senderPublicKey;
    private Date timestamp;
} 