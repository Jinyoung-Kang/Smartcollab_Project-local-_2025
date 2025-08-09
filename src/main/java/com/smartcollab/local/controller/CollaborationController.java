package com.smartcollab.local.controller;

import com.smartcollab.local.dto.ChatMessageDto;
import com.smartcollab.local.dto.CursorPositionDto;
import com.smartcollab.local.dto.EditorChangeDto;
import com.smartcollab.local.service.CollaborationService;
import com.smartcollab.local.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Set;

/**
 * WebSocket을 통한 실시간 협업(채팅, 편집) 메시지를 처리하는 Controller.
 */
@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CollaborationService collaborationService;
    private final TeamService teamService; // TeamService로 교체

    /**
     * 채팅 메시지를 DB에 저장하고 브로드캐스팅합니다.
     * @param teamId 팀 ID
     * @param chatMessage 채팅 메시지 DTO
     */
    @MessageMapping("/chat.sendMessage/{teamId}")
    public void sendMessage(@DestinationVariable Long teamId, @Payload ChatMessageDto chatMessage) {
        // TeamService를 통해 메시지 저장 및 브로드캐스팅 처리
        teamService.saveAndBroadcastMessage(teamId, chatMessage);
    }

    /**
     * 사용자가 채팅방에 참여했을 때 처리
     * @param teamId 팀 ID
     * @param chatMessage 참여 메시지 DTO
     * @param headerAccessor 세션 정보 접근자
     */
    @MessageMapping("/chat.addUser/{teamId}")
    public void addUser(@DestinationVariable Long teamId, @Payload ChatMessageDto chatMessage,
                        SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        // 웹소켓 세션에 사용자 정보 저장
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("teamId", teamId);

        collaborationService.userJoined(teamId, username);

        // 업데이트된 활성 사용자 목록 브로드캐스팅
        Set<String> activeUsers = collaborationService.getActiveUsers(teamId);
        messagingTemplate.convertAndSend("/topic/activeUsers/" + teamId, activeUsers);
    }

    /**
     * 실시간 에디터 내용 변경사항 처리
     */
    @MessageMapping("/editor.change/{teamId}")
    public void handleEditorChange(@DestinationVariable Long teamId, @Payload EditorChangeDto change) {
        messagingTemplate.convertAndSend("/topic/editor/" + teamId, change);
    }

    /**
     * 실시간 커서 위치 변경사항 처리
     */
    @MessageMapping("/editor.cursorMove/{teamId}")
    public void handleCursorMove(@DestinationVariable Long teamId, @Payload CursorPositionDto cursorPosition) {
        messagingTemplate.convertAndSend("/topic/cursor/" + teamId, cursorPosition);
    }
}