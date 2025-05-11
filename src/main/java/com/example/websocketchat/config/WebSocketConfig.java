package com.example.websocketchat.config;

import com.example.websocketchat.controller.ChatController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:*}") // 从 application.properties 读取，默认 "*"
    private String[] allowedOrigins;

    // 为心跳检测配置任务调度器
    @Bean
    public TaskScheduler heartfeltTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /app 是应用处理消息的前缀，例如 @MessageMapping 注解的方法
        registry.setApplicationDestinationPrefixes(ChatController.APP_DESTINATION_PREFIX);
        // /topic 用于广播消息（群聊），/user 用于点对点消息（私聊）
        // 启用一个简单的基于内存的消息代理，并配置服务器发送心跳信号 (例如每10秒一次)
        // 客户端也应配置接收和发送心跳
        registry.enableSimpleBroker(ChatController.TOPIC_MESSAGES.split("/")[1], "/user") // "topic", "user"
                .setHeartbeatValue(new long[] {10000, 10000}) // server_send_interval, server_receive_interval
                .setTaskScheduler(heartfeltTaskScheduler());
        // 为私聊设置用户目标前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // /chat 是 WebSocket (或 SockJS) 的端点，客户端将连接到这里
        // 在生产环境中，应将其限制为你的前端应用的实际来源
        logger.info("Registering STOMP endpoint /chat with allowed origins: {}", String.join(", ", allowedOrigins));
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    // 添加一个静态 logger 实例，如果需要在此类中记录日志
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketConfig.class);
}