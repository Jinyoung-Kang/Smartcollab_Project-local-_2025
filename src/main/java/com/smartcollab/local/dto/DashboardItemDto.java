package com.smartcollab.local.dto;

import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.Folder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class DashboardItemDto {
    private final Long id;
    private final String name;
    private final String type;
    private final LocalDateTime modifiedAt;
    private final String ownerName; // [추가] 생성자 이름 필드
    private final Long size;
    private final String uniqueKey;

    public DashboardItemDto(Folder folder) {
        this.id = folder.getFolderId();
        this.name = folder.getName();
        this.type = "folder";
        this.modifiedAt = folder.getCreatedAt();
        this.ownerName = folder.getOwner().getName(); // [추가] 폴더 생성자 이름 설정
        this.size = null;
        this.uniqueKey = "folder-" + folder.getFolderId();
    }

    public DashboardItemDto(FileEntity file) {
        this.id = file.getFileId();
        this.name = file.getOriginalName();
        this.type = file.getOriginalName().contains(".") ? file.getOriginalName().substring(file.getOriginalName().lastIndexOf(".") + 1) : "file";
        this.modifiedAt = file.getCreatedAt();
        this.ownerName = file.getOwner().getName(); // [추가] 파일 생성자 이름 설정
        this.size = file.getSize();
        this.uniqueKey = "file-" + file.getFileId();
    }
}