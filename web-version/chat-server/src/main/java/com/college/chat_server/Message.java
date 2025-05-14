package com.college.chat_server;

import lombok.Data;

@Data
public class Message {
    private String sender;
    private String content;
    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
