package com.smartcollab.local.controller;

import com.smartcollab.local.dto.ChatMessageDto;
import com.smartcollab.local.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final TeamService teamService;

    @GetMapping("/{teamId}/history")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long teamId) {
        List<ChatMessageDto> history = teamService.getChatHistory(teamId).stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{teamId}/history")
    public ResponseEntity<Void> clearChatHistory(@PathVariable Long teamId, @AuthenticationPrincipal UserDetails userDetails) {
        teamService.clearChatHistory(teamId, userDetails);
        return ResponseEntity.noContent().build();
    }
}