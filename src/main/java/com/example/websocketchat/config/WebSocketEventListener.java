package com.example.websocketchat.config;

import com.example.websocketchat.controller.ChatController; // 用于访问常量和静态集合
import com.example.websocketchat.model.UserJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate; // 用于发送消息

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String username = (String) sessionAttributes.get(ChatController.SESSION_USERNAME_KEY);

            if (username != null) {
                logger.info("🔌 User disconnected: {}", username);
                boolean removedUser = ChatController.onlineUsers.remove(username);
                ChatController.lastActiveTime.remove(username);

                if (removedUser) {
                    // 广播用户离开事件
                    UserJoinEvent leaveEvent = new UserJoinEvent(username, ChatController.USER_EVENT_TYPE_LEAVE, Instant.now());
                    messagingTemplate.convertAndSend(ChatController.TOPIC_USER_ACTIVITY, leaveEvent);
                    // 广播更新后的在线用户列表
                    messagingTemplate.convertAndSend(ChatController.TOPIC_ONLINE_USERS, new HashSet<>(ChatController.onlineUsers));
                    logger.info("📢 Broadcasted user leave and updated online users list after {} disconnected.", username);
                } else {
                    logger.warn("User {} was not in onlineUsers set during disconnect handling.", username);
                }
            } else {
                logger.warn("Username not found in session attributes during disconnect. Session ID: {}", headerAccessor.getSessionId());
            }
        } else {
            logger.warn("Session attributes are null for disconnect event. Session ID: {}", headerAccessor.getSessionId());
        }
    }
}