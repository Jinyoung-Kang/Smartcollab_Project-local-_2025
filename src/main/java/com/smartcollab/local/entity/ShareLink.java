package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 파일 공유 링크의 정보를 저장하는 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "share_links")
public class ShareLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    // 공유 대상 파일
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    // 링크를 생성한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 특정 사용자에게만 공유된 경우 (공개 링크는 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWithUser;

    @Column(nullable = false, unique = true)
    private String url; // 고유한 링크 주소

    private LocalDateTime expiresAt; // 만료 시간

    private Integer downloadLimit; // 다운로드 횟수 제한

    private int downloadCount = 0; // 현재 다운로드 횟수

    private String password; // 링크 비밀번호

    @Builder
    public ShareLink(FileEntity file, User owner, User sharedWithUser, String url, LocalDateTime expiresAt, Integer downloadLimit, String password) {
        this.file = file;
        this.owner = owner;
        this.sharedWithUser = sharedWithUser;
        this.url = url;
        this.expiresAt = expiresAt;
        this.downloadLimit = downloadLimit;
        this.password = password;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}