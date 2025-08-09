package com.smartcollab.local.controller;

import com.smartcollab.local.dto.NotificationResponseDto;
import com.smartcollab.local.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        List<NotificationResponseDto> notifications = notificationService.getUnreadNotifications(userDetails.getUsername())
                .stream()
                .map(NotificationResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAsRead(notificationId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // --- [새로 추가된 API 1: 개별 알림 삭제] ---
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId, @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.deleteNotification(notificationId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- [새로 추가된 API 2: 모든 알림 삭제] ---
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.deleteAllNotifications(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}