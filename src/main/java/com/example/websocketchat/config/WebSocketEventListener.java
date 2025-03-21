package com.example.websocketchat.config;

import com.example.websocketchat.controller.ChatController;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            System.out.println("🔌 用户断开连接: " + username);
            ChatController.onlineUsers.remove(username);
            ChatController.lastActiveTime.remove(username);
        }
    }
}
