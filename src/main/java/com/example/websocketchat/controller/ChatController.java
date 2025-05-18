package com.example.websocketchat.controller;

import com.example.websocketchat.model.ChatMessage;
import com.example.websocketchat.model.PrivateChatMessage;
import com.example.websocketchat.model.UserJoinEvent;
import com.example.websocketchat.model.glm.GlmApiRequest;
import com.example.websocketchat.model.glm.GlmApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    // Constants
    private static final String BOT_NAME = "Felina";
    public static final String SESSION_USERNAME_KEY = "username"; // 在WebSocketEventListener中也可能用到
    public static final String USER_EVENT_TYPE_JOIN = "JOIN";
    public static final String USER_EVENT_TYPE_LEAVE = "LEAVE";
    public static final String USER_EVENT_TYPE_PING = "PING";

    public static final String TOPIC_MESSAGES = "/topic/messages";
    public static final String TOPIC_USER_ACTIVITY = "/topic/user-activity";
    public static final String TOPIC_ONLINE_USERS = "/topic/online-users";
    public static final String TOPIC_PRIVATE_MESSAGES = "/private"; // 注意在WebSocketConfig中配置的是 /user，这里是目标后缀

    public static final String APP_DESTINATION_PREFIX = "/app"; // 来自WebSocketConfig

    private static final String DEFAULT_BOT_ERROR_MESSAGE = "Oops! I'm having trouble thinking right now. 😅";
    private static final String DEFAULT_BOT_PARSE_ERROR_MESSAGE = "I don't know what to say! 🤖";
    private static final long USER_STALE_THRESHOLD_SECONDS = 60;
    private static final int BOT_REPLY_PROBABILITY_PERCENT = 25; // 25% 机器人回复概率

    @Value("${chat.bot.api.key}")
    private String apiKey;

    @Value("${chat.bot.api.endpoint}")
    private String apiEndpoint;

    @Value("${chat.bot.model}")
    private String botModel;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON processing
    private final Random random = new Random();

    // User presence management - 考虑未来将其提取到专门的服务中
    public static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    public static final Map<String, Instant> lastActiveTime = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(apiEndpoint)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        onlineUsers.clear();
        lastActiveTime.clear();
        logger.info("ChatController initialized. WebClient configured for endpoint: {}", apiEndpoint);
    }

    @MessageMapping("/message") // 对应前端 stompClient.send("/app/message", ...)
    @SendTo(TOPIC_MESSAGES)
    public ChatMessage sendMessage(ChatMessage message, StompHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get(SESSION_USERNAME_KEY);
        if (username == null) { // 基本的校验
            logger.warn("Message received without username in session: {}", message);
            // 可以选择不处理或返回错误，这里简单地不修改发送者
        } else {
            message.setUser(username); // 确保消息的 user 字段是会话中的用户名
        }
        message.setTimestamp(Instant.now()); // 设置服务端时间戳

        logger.info("📩 User message: {}: {}", message.getUser(), message.getMessage());
        lastActiveTime.put(message.getUser(), Instant.now());

        // 发送自动回复消息
        sendAutoReplyMessage(message);

        // 随机生成机器人回复
        if (random.nextInt(100) < BOT_REPLY_PROBABILITY_PERCENT) {
            generateBotReplyAsync(message.getMessage());
        }
        return message; // 用户消息通过 @SendTo 发送
    }

    @MessageMapping("/private-message") // /app/private-message
    public void sendPrivateMessage(PrivateChatMessage message, StompHeaderAccessor headerAccessor) {
        String senderUsername = (String) headerAccessor.getSessionAttributes().get(SESSION_USERNAME_KEY);
        if (senderUsername == null) {
            logger.warn("Private message received without sender username in session: {}", message);
            return;
        }
        message.setSender(senderUsername); // 确保发送者是会话用户
        message.setTimestamp(Instant.now());

        logger.info("📩 Private message: {} -> {}: {}", message.getSender(), message.getRecipient(), message.getMessage());
        lastActiveTime.put(message.getSender(), Instant.now());

        // 发送原始消息
        simpMessagingTemplate.convertAndSendToUser(
                message.getRecipient(), TOPIC_PRIVATE_MESSAGES, message);
        simpMessagingTemplate.convertAndSendToUser( // 也发回给发送者，以便其UI更新
                message.getSender(), TOPIC_PRIVATE_MESSAGES, message);

        // 发送自动回复
        sendPrivateAutoReply(message);
    }

    @MessageMapping("/user-join") // /app/user-join
    @SendTo(TOPIC_USER_ACTIVITY)
    public UserJoinEvent userJoin(UserJoinEvent joinEvent, StompHeaderAccessor headerAccessor) {
        String username = joinEvent.getUsername();
        // 将用户名存入WebSocket会话，以便后续使用
        headerAccessor.getSessionAttributes().put(SESSION_USERNAME_KEY, username);
        joinEvent.setTimestamp(Instant.now());

        logger.info("👤 User activity: {} {}", username, joinEvent.getType());

        switch (joinEvent.getType()) {
            case USER_EVENT_TYPE_JOIN:
                onlineUsers.add(username);
                lastActiveTime.put(username, Instant.now());
                break;
            case USER_EVENT_TYPE_LEAVE:
                onlineUsers.remove(username);
                lastActiveTime.remove(username);
                break;
            case USER_EVENT_TYPE_PING: // PING仅用于更新活跃时间
                if (onlineUsers.contains(username)) { // 只更新已加入用户的活跃时间
                    lastActiveTime.put(username, Instant.now());
                }
                // PING 事件不应广播给所有用户，所以这里不返回或返回特定类型
                // 为了简单，当前@SendTo会广播，前端应忽略PING类型的UserActivity消息的显示
                break;
            default:
                logger.warn("Unknown user event type: {}", joinEvent.getType());
                return null; // 或者不广播未知类型的事件
        }

        broadcastOnlineUsers();
        return joinEvent;
    }

    /**
     * 发送自动回复消息，确认收到用户消息（群聊）
     */
    private void sendAutoReplyMessage(ChatMessage originalMessage) {
        String replyContent = String.format("服务端响应：【%s】，消息已收到]", originalMessage.getMessage());
        ChatMessage replyMessage = new ChatMessage(BOT_NAME, replyContent, Instant.now());
        simpMessagingTemplate.convertAndSend(TOPIC_MESSAGES, replyMessage);
        logger.info("💬 Auto reply sent for message: {}", originalMessage.getMessage());
    }

    /**
     * 发送自动回复消息，确认收到用户消息（私聊）
     */
    private void sendPrivateAutoReply(PrivateChatMessage originalMessage) {
        String replyContent = String.format("服务端响应：【%s】，消息已收到]", originalMessage.getMessage());
        PrivateChatMessage replyMessage = new PrivateChatMessage();
        replyMessage.setSender(BOT_NAME);
        replyMessage.setRecipient(originalMessage.getSender());
        replyMessage.setMessage(replyContent);
        replyMessage.setTimestamp(Instant.now());

        simpMessagingTemplate.convertAndSendToUser(
                originalMessage.getSender(), TOPIC_PRIVATE_MESSAGES, replyMessage);
        logger.info("💬 Private auto reply sent to: {}", originalMessage.getSender());
    }

    private void generateBotReplyAsync(String userMessage) {
        fetchGLMResponse(userMessage)
                .doOnSuccess(botResponse -> {
                    ChatMessage botMessage = new ChatMessage(BOT_NAME, botResponse, Instant.now());
                    simpMessagingTemplate.convertAndSend(TOPIC_MESSAGES, botMessage);
                    logger.info("🤖 Bot reply sent for user message: {}", userMessage);
                })
                .doOnError(error -> logger.error("Error generating bot reply for: {}", userMessage, error))
                .subscribe();
    }

    private Mono<String> fetchGLMResponse(String userMessage) {
        GlmApiRequest.MessagePayload payload = new GlmApiRequest.MessagePayload("user", userMessage);
        GlmApiRequest requestBody = new GlmApiRequest(botModel, Collections.singletonList(payload));

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class) // 获取原始JSON字符串
                .map(this::extractGLMReplyFromJsonResponse) // 使用Jackson解析
                .onErrorResume(e -> {
                    logger.error("⚠️ GLM API call failed for message \"{}\": {}", userMessage, e.getMessage(), e);
                    return Mono.just(DEFAULT_BOT_ERROR_MESSAGE);
                });
    }

    private String extractGLMReplyFromJsonResponse(String jsonResponse) {
        try {
            GlmApiResponse response = objectMapper.readValue(jsonResponse, GlmApiResponse.class);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                GlmApiResponse.Message message = response.getChoices().get(0).getMessage();
                if (message != null && message.getContent() != null) {
                    return message.getContent();
                }
            }
            logger.warn("Could not extract content from GLM API response: {}", jsonResponse);
            return DEFAULT_BOT_PARSE_ERROR_MESSAGE;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing GLM API JSON response: {}", jsonResponse, e);
            return DEFAULT_BOT_PARSE_ERROR_MESSAGE;
        }
    }

    @Scheduled(fixedRate = 12000) // 🔥 机器人每 12 秒自动发送一条消息
    public void botAutoMessage() {
        String botPrompt = "模拟一个大学生在同学群里抛出话题！不要打招呼，话不要太多，直接发言。确保输出不要包含角色扮演的提示或任何markdown格式。";
        logger.info("🤖 Triggering bot auto message with prompt: {}", botPrompt);
        fetchGLMResponse(botPrompt)
                .doOnSuccess(botResponse -> {
                    if (!DEFAULT_BOT_ERROR_MESSAGE.equals(botResponse) && !DEFAULT_BOT_PARSE_ERROR_MESSAGE.equals(botResponse)) {
                        ChatMessage botMessage = new ChatMessage(BOT_NAME, botResponse, Instant.now());
                        logger.info("🤖 AI Bot auto message: {}", botMessage.getMessage());
                        simpMessagingTemplate.convertAndSend(TOPIC_MESSAGES, botMessage);
                    } else {
                        logger.warn("🤖 Bot auto message generation failed or returned error message, not sending.");
                    }
                })
                .doOnError(error -> logger.error("Error in scheduled bot auto message: ", error))
                .subscribe();
    }

    @Scheduled(fixedRate = 10000) // 每 10 秒检查一次并发送在线用户列表
    public void sendOnlineUsersListPeriodically() {
        Instant threshold = Instant.now().minusSeconds(USER_STALE_THRESHOLD_SECONDS);
        Set<String> staleUsers = lastActiveTime.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(threshold) && onlineUsers.contains(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!staleUsers.isEmpty()) {
            logger.info("🧹 Removing stale users: {}", staleUsers);
            staleUsers.forEach(user -> {
                onlineUsers.remove(user);
                lastActiveTime.remove(user);
                // 广播用户离开事件
                UserJoinEvent leaveEvent = new UserJoinEvent(user, USER_EVENT_TYPE_LEAVE, Instant.now());
                simpMessagingTemplate.convertAndSend(TOPIC_USER_ACTIVITY, leaveEvent);
            });
        }
        broadcastOnlineUsers();
    }

    private void broadcastOnlineUsers() {
        simpMessagingTemplate.convertAndSend(TOPIC_ONLINE_USERS, new HashSet<>(onlineUsers));
        // logger.debug("Online users broadcasted: {}", onlineUsers); // 可以用debug级别
    }
}