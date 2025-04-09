package com.example.websocketchat;

import com.example.websocketchat.model.UserJoinEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserJoinEventTest {

    @Test
    void testNoArgsConstructor() {
        UserJoinEvent event = new UserJoinEvent();
        assertNull(event.getUsername());
        assertNull(event.getType());
        assertNull(event.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        String username = "alice";
        String type = "JOIN";
        String timestamp = "2023-05-01T12:00:00";

        UserJoinEvent event = new UserJoinEvent(username, type, timestamp);

        assertEquals(username, event.getUsername());
        assertEquals(type, event.getType());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    void testGettersAndSetters() {
        UserJoinEvent event = new UserJoinEvent();

        String username = "bob";
        String type = "LEAVE";
        String timestamp = "2023-05-01T14:30:00";

        event.setUsername(username);
        event.setType(type);
        event.setTimestamp(timestamp);

        assertEquals(username, event.getUsername());
        assertEquals(type, event.getType());
        assertEquals(timestamp, event.getTimestamp());
    }
}
