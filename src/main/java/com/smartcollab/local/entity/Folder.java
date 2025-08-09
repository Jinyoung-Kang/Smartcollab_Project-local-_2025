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
 * 파일과 다른 폴더를 담는 계층 구조의 폴더 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @Column(nullable = false)
    private String name;

    // 폴더 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 폴더가 속한 팀 (개인 폴더일 경우 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // 상위 폴더 (최상위 폴더일 경우 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    // 하위 폴더 목록
    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL)
    private List<Folder> subFolders = new ArrayList<>();

    // 폴더에 속한 파일 목록
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<FileEntity> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Folder(String name, User owner, Team team, Folder parentFolder) {
        this.name = name;
        this.owner = owner;
        this.team = team;
        this.parentFolder = parentFolder;
        this.createdAt = LocalDateTime.now();
    }

    public void setParentFolder(Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    public void changeName(String newName) {
        this.name = newName;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}