package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 특정 파일 버전에 대한 전자 서명 정보를 저장하는 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "signatures")
public class Signature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long signId;

    // 서명된 파일
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    // 서명된 파일의 특정 버전
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private FileVersion fileVersion;

    // 서명한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User signer;

    @Column(nullable = false)
    private LocalDateTime signedAt;

    @Column(nullable = false)
    private boolean isValid = true; // 파일 수정 시 false로 변경

    @Builder
    public Signature(FileEntity file, FileVersion fileVersion, User signer) {
        this.file = file;
        this.fileVersion = fileVersion;
        this.signer = signer;
        this.signedAt = LocalDateTime.now();
    }

    public void invalidate() {
        this.isValid = false;
    }
}
