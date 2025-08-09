package com.smartcollab.local.config;

import com.smartcollab.local.dto.ChatMessageDto;
import com.smartcollab.local.entity.ChatMessage;
import com.smartcollab.local.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;

/**
 * Listens for WebSocket disconnection events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDisconnectListener {
    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long teamId = (Long) headerAccessor.getSessionAttributes().get("teamId");

        if (username != null && teamId != null) {
            log.info("User Disconnected: {} from team {}", username, teamId);
            collaborationService.userLeft(teamId, username);

            Set<String> activeUsers = collaborationService.getActiveUsers(teamId);
            messagingTemplate.convertAndSend("/topic/activeUsers/" + teamId, activeUsers);
        }
    }
}