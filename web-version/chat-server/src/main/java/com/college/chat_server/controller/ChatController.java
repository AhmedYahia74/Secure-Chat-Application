package com.college.chat_server.controller;

import com.college.chat_server.model.Message;
import com.college.chat_server.model.User;
import com.college.chat_server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @MessageMapping("/register")
    public void registerUser(@Payload User user, SimpMessageHeaderAccessor headerAccessor) {
        logger.info("Registering user: {}", user.getUsername());
        headerAccessor.getSessionAttributes().put("username", user.getUsername());
        userService.addUser(user);
        // Broadcast user list update
        messagingTemplate.convertAndSend("/topic/users", userService.getUsers());
        // Broadcast public key update
        messagingTemplate.convertAndSend("/topic/public-keys", user);
        logger.info("User registered successfully: {}", user.getUsername());
    }

    @MessageMapping("/request-public-key")
    public void handlePublicKeyRequest(@Payload User request) {
        logger.info("Handling public key request for user: {}", request.getUsername());
        User user = userService.getUser(request.getUsername());
        if (user != null) {
            messagingTemplate.convertAndSend("/topic/public-keys", user);
            logger.info("Public key sent for user: {}", request.getUsername());
        } else {
            logger.warn("User not found for public key request: {}", request.getUsername());
        }
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload Message message) {
        logger.info("Processing message from {} to {}", message.getSender(), message.getReceiver());
        // Send to receiver
        messagingTemplate.convertAndSendToUser(
            message.getReceiver(),
            "/queue/messages",
            message
        );
        logger.info("Message sent successfully");
    }

    @MessageMapping("/disconnect")
    public void disconnectUser(@Payload User user) {
        logger.info("User disconnecting: {}", user.getUsername());
        userService.removeUser(user);
        messagingTemplate.convertAndSend("/topic/users", userService.getUsers());
        logger.info("User disconnected: {}", user.getUsername());
    }
} 