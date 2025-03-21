package com.example.websocketchat.controller;

import com.example.websocketchat.model.ChatMessage;
import com.example.websocketchat.model.PrivateChatMessage;
import com.example.websocketchat.model.UserJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    private static final String BOT_NAME = "Felina";
    private static final String API_KEY = "sk-Pq5vKamk5EDpFXPf17A36cFf050c422984587f464eBb2b2e"; // ⚠️ 这里替换 API Key
    private static final String API_ENDPOINT = "https://aiproxy.bja.sealos.run/v1/chat/completions";

    private final WebClient webClient = WebClient.builder()
            .baseUrl(API_ENDPOINT)
            .defaultHeader("Authorization", "Bearer " + API_KEY) // 设置 API Key
            .defaultHeader("Content-Type", "application/json")
            .build();

    private final Random random = new Random();
    public static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    public static final Map<String, Instant> lastActiveTime = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @PostConstruct
    public void init() {
        // Clear online users when server starts
        onlineUsers.clear();
        lastActiveTime.clear();
    }

    @MessageMapping("/message")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message) {
        System.out.println("📩 用户消息: " + message.getUser() + ": " + message.getMessage());

        // Update last active time for this user
        lastActiveTime.put(message.getUser(), Instant.now());

        // **始终发送用户的消息**
        // simpMessagingTemplate.convertAndSend("/topic/messages", message);

        // **机器人 x% 概率回复**
        if (random.nextInt(100) < 0) {
            ChatMessage botMessage = generateBotReply(message.getMessage());
            simpMessagingTemplate.convertAndSend("/topic/messages", botMessage);
        }

        return message;
    }

    @MessageMapping("/private-message")
    public void sendPrivateMessage(PrivateChatMessage message) {
        System.out.println("📩 私聊消息: " + message.getSender() + " -> " + message.getRecipient() + ": " + message.getMessage());

        // Update last active time for sender
        lastActiveTime.put(message.getSender(), Instant.now());

        // Send to recipient
        simpMessagingTemplate.convertAndSendToUser(
                message.getRecipient(), "/private", message);

        // Also send a copy back to sender so they can see their own messages
        simpMessagingTemplate.convertAndSendToUser(
                message.getSender(), "/private", message);
    }

    @MessageMapping("/user-join")
    @SendTo("/topic/user-activity")
    public UserJoinEvent userJoin(UserJoinEvent joinEvent) {
        System.out.println("👤 用户" + (joinEvent.getType().equals("JOIN") ? "加入" : "离开") + ": " + joinEvent.getUsername());

        if ("JOIN".equals(joinEvent.getType())) {
            onlineUsers.add(joinEvent.getUsername());
            lastActiveTime.put(joinEvent.getUsername(), Instant.now());
        } else if ("LEAVE".equals(joinEvent.getType())) {
            onlineUsers.remove(joinEvent.getUsername());
            lastActiveTime.remove(joinEvent.getUsername());
        }

        simpMessagingTemplate.convertAndSend("/topic/online-users", new HashSet<>(onlineUsers));

        return joinEvent;
    }

    private ChatMessage generateBotReply(String userMessage) {
        String botResponse = fetchGLM4FlashResponse(userMessage);
        return new ChatMessage(BOT_NAME, botResponse, new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
    }

    // 在这里改模型！！！                         ↓↓↓
    private String fetchGLM4FlashResponse(String userMessage) {
        String requestBody = "{ \"model\": \"glm-3-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"" + userMessage + "\"}] }";

        Mono<String> responseMono = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        try {
            String response = responseMono.block(); // 等待 API 响应
            System.out.println("🔍 GLM API Response: " + response); // 记录 API 响应
            return extractGLMReply(response);
        } catch (Exception e) {
            System.err.println("⚠️ GLM-4 API 调用失败：" + e.getMessage());
            return "Oops! I'm having trouble thinking right now. 😅";
        }
    }

    private String extractGLMReply(String jsonResponse) {
        try {
            int startIndex = jsonResponse.indexOf("\"content\":\"") + 11;
            int endIndex = jsonResponse.indexOf("\"}", startIndex);
            return jsonResponse.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "I don't know what to say! 🤖";
        }
    }

    // 🔥 机器人每 7 秒自动发送一条消息
    @Scheduled(fixedRate = 7000)
    public void botAutoMessage() {
        String botRequire = "模拟一个大学生在同学群里聊天，不要打招呼，话不要太多，直接说话！";
        ChatMessage botMessage = generateBotReply(botRequire);
        System.out.println("🤖 AI 机器人自动发言: " + botMessage.getMessage());
        simpMessagingTemplate.convertAndSend("/topic/messages", botMessage);
    }

    // Send online users list periodically to handle reconnections
    @Scheduled(fixedRate = 10000)
    public void sendOnlineUsersList() {
        // Remove stale users that haven't had activity for over 1 minute
        Instant threshold = Instant.now().minusSeconds(60);
        Set<String> staleUsers = new HashSet<>();

        for (Map.Entry<String, Instant> entry : lastActiveTime.entrySet()) {
            if (entry.getValue().isBefore(threshold)) {
                staleUsers.add(entry.getKey());
            }
        }

        // Remove stale users
        for (String user : staleUsers) {
            System.out.println("🧹 Removing stale user: " + user);
            onlineUsers.remove(user);
            lastActiveTime.remove(user);
        }

        simpMessagingTemplate.convertAndSend("/topic/online-users", new HashSet<>(onlineUsers));
    }
}
