package com.example.websocketchat;

import com.example.websocketchat.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    @BeforeEach
    public void setup() {
        this.wsUrl = "ws://localhost:" + port + "/ws";
        
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void testChatMessageExchange() throws ExecutionException, InterruptedException, TimeoutException {
        // Set up test data
        final String username = "testUser";
        final String messageContent = "Hello, WebSocket!";
        
        // Create container to store the response
        final CompletableFuture<ChatMessage> completableFuture = new CompletableFuture<>();

        // Connect to WebSocket server
        StompSession session = stompClient
                .connect(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        // Subscribe to receive messages
        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((ChatMessage) payload);
            }
        });
        
        // Create a new chat message to send
        ChatMessage expectedMessage = new ChatMessage(username, messageContent, null);
        
        // Send the message
        session.send("/app/chat.sendMessage", expectedMessage);
        
        // Wait for response
        ChatMessage receivedMessage = completableFuture.get(5, TimeUnit.SECONDS);
        
        // Verify received message
        assertNotNull(receivedMessage);
        assertEquals(username, receivedMessage.getUser());
        assertEquals(messageContent, receivedMessage.getMessage());
        assertNotNull(receivedMessage.getTimestamp()); // Timestamp should be set by the server
    }
}
