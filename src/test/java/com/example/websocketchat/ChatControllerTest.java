package com.example.websocketchat;

import com.example.websocketchat.controller.ChatController;
import com.example.websocketchat.model.ChatMessage;
import com.example.websocketchat.model.PrivateChatMessage;
import com.example.websocketchat.model.UserJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatController chatController;

    private String timestamp;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

        // Clear online users before each test
        ChatController.onlineUsers.clear();
        ChatController.lastActiveTime.clear();
    }

    @Test
    public void testSendMessage() {
        // Create test message
        ChatMessage message = new ChatMessage("testUser", "Hello, World!", timestamp);

        // Send the message
        ChatMessage result = chatController.sendMessage(message);

        // Verify the message was returned correctly
        assertEquals("testUser", result.getUser());
        assertEquals("Hello, World!", result.getMessage());
        assertNotNull(result.getTimestamp());

        // Verify user is marked as active
        assertTrue(ChatController.lastActiveTime.containsKey("testUser"));
    }

    @Test
    public void testSendPrivateMessage() {
        // Create private message
        PrivateChatMessage message = new PrivateChatMessage(
                "sender", "recipient", "Private message", timestamp);

        // Send the private message
        chatController.sendPrivateMessage(message);

        // Verify messaging template was called to send to recipient
        verify(messagingTemplate).convertAndSendToUser(
                eq("recipient"), eq("/private"), any(PrivateChatMessage.class));

        // Verify messaging template was called to send a copy to sender
        verify(messagingTemplate).convertAndSendToUser(
                eq("sender"), eq("/private"), any(PrivateChatMessage.class));

        // Verify sender is marked as active
        assertTrue(ChatController.lastActiveTime.containsKey("sender"));
    }

    @Test
    public void testUserJoin() {
        // Create join event
        UserJoinEvent joinEvent = new UserJoinEvent("newUser", "JOIN", timestamp);

        // Process the join event
        UserJoinEvent result = chatController.userJoin(joinEvent);

        // Verify result
        assertEquals("newUser", result.getUsername());
        assertEquals("JOIN", result.getType());

        // Verify user is added to online users
        assertTrue(ChatController.onlineUsers.contains("newUser"));

        // Verify online users list was sent
        verify(messagingTemplate).convertAndSend(Optional.ofNullable(eq("/topic/online-users")), any());
    }

    @Test
    public void testUserLeave() {
        // First add the user
        ChatController.onlineUsers.add("leavingUser");
        ChatController.lastActiveTime.put("leavingUser", java.time.Instant.now());

        // Create leave event
        UserJoinEvent leaveEvent = new UserJoinEvent("leavingUser", "LEAVE", timestamp);

        // Process the leave event
        chatController.userJoin(leaveEvent);

        // Verify user is removed from online users
        assertFalse(ChatController.onlineUsers.contains("leavingUser"));
        assertFalse(ChatController.lastActiveTime.containsKey("leavingUser"));

        // Verify online users list was sent
        verify(messagingTemplate).convertAndSend(Optional.ofNullable(eq("/topic/online-users")), any());
    }
}
