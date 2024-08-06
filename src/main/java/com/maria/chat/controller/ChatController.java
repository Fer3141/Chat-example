package com.maria.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.maria.chat.model.Message;
import com.maria.chat.model.User;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    private List<User> connectedUsers;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void init() {
        connectedUsers = new ArrayList<>();
    }

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public Message send(Message message) throws Exception {
        if (message.getText().length() > 200) {
            throw new IllegalArgumentException("El mensaje no puede tener más de 200 caracteres.");
        }
        return message;
    }

    @MessageMapping("/addUser")
    @SendTo("/topic/users")
    public List<User> addUser(User user, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        connectedUsers.add(user);
        headerAccessor.getSessionAttributes().put("username", user.getName());
        return connectedUsers;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/chat")
    public String chat(@RequestParam("username") String username, Model model) {
        model.addAttribute("username", username);
        return "chat-principal";
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String username = (String) StompHeaderAccessor.wrap(event.getMessage()).getSessionAttributes().get("username");
        if (username != null) {
            connectedUsers = connectedUsers.stream()
                    .filter(user -> !user.getName().equals(username))
                    .collect(Collectors.toList());
            messagingTemplate.convertAndSend("/topic/users", connectedUsers);
        }
    }
}
