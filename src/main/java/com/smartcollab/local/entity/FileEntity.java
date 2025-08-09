package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity for storing file metadata.
 * The actual file content is stored in the local file system.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, unique = true)
    private String storedName;

    private Long size;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // This mapping allows accessing all versions associated with this file.
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileVersion> versions = new ArrayList<>();

    @Builder
    public FileEntity(User owner, Folder folder, String originalName, String storedName, Long size) {
        this.owner = owner;
        this.folder = folder;
        this.originalName = originalName;
        this.storedName = storedName;
        this.size = size;
        this.createdAt = LocalDateTime.now();
    }

    public void moveToTrash() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restoreFromTrash() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public void changeName(String newName) {
        this.originalName = newName;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}