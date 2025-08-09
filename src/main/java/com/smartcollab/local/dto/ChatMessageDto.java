package com.smartcollab.local.dto;

import com.smartcollab.local.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String content;
    private String sender; // username
    private ChatMessage.MessageType type;
    private Long fileId;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(
                entity.getContent(),
                entity.getSender().getUsername(),
                entity.getType(),
                entity.getFileId(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getCreatedAt()
        );
    }
}