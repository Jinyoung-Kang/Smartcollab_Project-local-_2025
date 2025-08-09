package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 파일의 버전별 저장 경로와 생성자를 관리하는 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file_versions")
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    // 원본 파일 (FileEntity와 다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    // 해당 버전의 파일이 저장된 실제 경로
    @Column(nullable = false)
    private String storedPath;

    // 이 버전을 생성한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id", nullable = false) // DB의 editor_id 컬럼과 매핑
    private User editor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FileVersion(FileEntity file, String storedPath, User editor) {
        this.file = file;
        this.storedPath = storedPath;
        this.editor = editor; // 빌더에 editor 추가
        this.createdAt = LocalDateTime.now();
    }

}