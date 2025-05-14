package com.college.chat_server.service;

import com.college.chat_server.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    public void removeUser(User user) {
        users.remove(user.getUsername());
    }

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public User getUser(String username) {
        return users.get(username);
    }
} 