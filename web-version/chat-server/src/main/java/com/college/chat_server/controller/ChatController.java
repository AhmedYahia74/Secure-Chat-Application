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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Controller
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @MessageMapping("/register")
    public void registerUser(@Payload User user, SimpMessageHeaderAccessor headerAccessor) {
        try {
            logger.info("Registering user: {}", user.getUsername());
            headerAccessor.setUser(() -> user.getUsername());
            userService.addUser(user);
            
            // Broadcast user list update
            String usersJson = objectMapper.writeValueAsString(userService.getUsers());
            logger.debug("Broadcasting user list: {}", usersJson);
            messagingTemplate.convertAndSend("/topic/users", usersJson);
            
            // Broadcast public key update
            String userJson = objectMapper.writeValueAsString(user);
            logger.debug("Broadcasting public key: {}", userJson);
            messagingTemplate.convertAndSend("/topic/public-keys", userJson);
            
            logger.info("User registered successfully: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/request-public-key")
    public void handlePublicKeyRequest(@Payload User request) {
        try {
            logger.info("Handling public key request for user: {}", request.getUsername());
            User user = userService.getUser(request.getUsername());
            if (user != null) {
                String userJson = objectMapper.writeValueAsString(user);
                logger.debug("Sending public key: {}", userJson);
                messagingTemplate.convertAndSend("/topic/public-keys", userJson);
                logger.info("Public key sent for user: {}", request.getUsername());
            } else {
                logger.warn("User not found for public key request: {}", request.getUsername());
            }
        } catch (Exception e) {
            logger.error("Error handling public key request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload Message message) {
        try {
            logger.info("Broadcasting message from {} to {}", message.getSender(), message.getReceiver());
            String messageJson = objectMapper.writeValueAsString(message);
            messagingTemplate.convertAndSend("/topic/messages", messageJson);
            logger.info("Message broadcasted to /topic/messages");
        } catch (Exception e) {
            logger.error("Error broadcasting message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/disconnect")
    public void disconnectUser(@Payload User user) {
        try {
            logger.info("User disconnecting: {}", user.getUsername());
            userService.removeUser(user);
            String usersJson = objectMapper.writeValueAsString(userService.getUsers());
            logger.debug("Broadcasting updated user list: {}", usersJson);
            messagingTemplate.convertAndSend("/topic/users", usersJson);
            logger.info("User disconnected: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error disconnecting user: {}", e.getMessage(), e);
        }
    }
} 