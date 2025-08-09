package com.smartcollab.local.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 및 STOMP 메시징 설정 클래스.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 시작할 엔드포인트.
        // "/ws" 경로로 SockJS를 통해 연결을 허용합니다.
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000") // 프론트엔드 주소 허용
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix.
        // 예: /app/chat.sendMessage
        registry.setApplicationDestinationPrefixes("/app");

        // 서버가 클라이언트에게 메시지를 브로드캐스팅할 때 사용하는 prefix.
        // /topic으로 시작하는 경로를 구독하는 클라이언트에게 메시지를 전달합니다.
        registry.enableSimpleBroker("/topic");
    }
}
