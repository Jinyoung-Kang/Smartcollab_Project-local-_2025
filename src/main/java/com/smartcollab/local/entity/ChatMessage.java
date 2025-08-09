package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageType type;

    private Long fileId;
    private String fileType;
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageType {
        CHAT, FILE_SHARE, JOIN, LEAVE
    }

    @Builder
    public ChatMessage(Team team, User sender, String content, MessageType type, Long fileId, String fileType, Long fileSize) {
        this.team = team;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.fileId = fileId;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
}