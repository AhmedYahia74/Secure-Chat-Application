package com.college.chat_server.model;

import lombok.Data;

@Data
public class ConnectionRequest {
    private String from;
    private String to;
    private String publicKey;
} 