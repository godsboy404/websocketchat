package com.example.websocketchat;

import com.example.websocketchat.config.WebSocketConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebSocketConfigTest {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Test
    void testWebSocketConfigExists() {
        assertNotNull(webSocketConfig);
    }

    @Test
    void testConfigureMessageBroker() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        webSocketConfig.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/user");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    void testRegisterStompEndpoints() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        when(registry.addEndpoint("/chat")).thenReturn((StompWebSocketEndpointRegistration) mock(StompEndpointRegistry.class));

        webSocketConfig.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/chat");
    }
}
