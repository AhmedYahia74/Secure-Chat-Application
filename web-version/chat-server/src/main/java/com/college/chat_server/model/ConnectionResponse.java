package com.college.chat_server.model;

import lombok.Data;

@Data
public class ConnectionResponse {
    private String from;
    private String to;
    private boolean accepted;
    private String publicKey;
} 