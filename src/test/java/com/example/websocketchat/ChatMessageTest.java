package com.example.websocketchat;

import com.example.websocketchat.model.ChatMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void testNoArgsConstructor() {
        ChatMessage message = new ChatMessage();
        assertNull(message.getUser());
        assertNull(message.getMessage());
        assertNull(message.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        String user = "testUser";
        String messageText = "Hello, World!";
        String timestamp = "2023-05-01T12:00:00";

        ChatMessage message = new ChatMessage(user, messageText, timestamp);

        assertEquals(user, message.getUser());
        assertEquals(messageText, message.getMessage());
        assertEquals(timestamp, message.getTimestamp());
    }

    @Test
    void testGettersAndSetters() {
        ChatMessage message = new ChatMessage();

        String user = "john";
        String messageText = "Test message";
        String timestamp = "2023-05-01T12:34:56";

        message.setUser(user);
        message.setMessage(messageText);
        message.setTimestamp(timestamp);

        assertEquals(user, message.getUser());
        assertEquals(messageText, message.getMessage());
        assertEquals(timestamp, message.getTimestamp());
    }
}
