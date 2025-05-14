package com.college.chat_server.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonRawValue;

@Data
public class User {
    private String username;
    @JsonRawValue
    private String publicKey;
} 