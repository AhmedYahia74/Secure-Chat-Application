package com.college.chat_server.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.Date;
import java.util.List;

@Data
public class Message {
    private String sender;
    private String receiver;
    private String content;
    @JsonRawValue
    private List<Integer> encryptedContent;
    @JsonRawValue
    private List<Integer> iv;
    @JsonRawValue
    private String senderPublicKey;
    private Date timestamp;
    private Boolean isUser;
} 