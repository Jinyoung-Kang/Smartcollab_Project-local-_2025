package com.smartcollab.local.dto;

import com.smartcollab.local.entity.Notification;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {
    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;
    private final Notification.NotificationType type;
    private final Long invitationId; // --- [추가] ---

    public NotificationResponseDto(Notification notification) {
        this.id = notification.getId();
        this.content = notification.getContent();
        this.createdAt = notification.getCreatedAt();
        this.type = notification.getType();
        // --- [추가] ---
        this.invitationId = (notification.getInvitation() != null) ? notification.getInvitation().getId() : null;
    }
}