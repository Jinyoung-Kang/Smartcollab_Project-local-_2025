package com.smartcollab.local.dto;

import com.smartcollab.local.entity.FileEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FileSearchResultDto {
    private final Long id;
    private final String name;
    private final String type;
    private final String path;
    private final LocalDateTime modifiedAt;
    private final String ownerName;
    private final String uniqueKey;

    public FileSearchResultDto(FileEntity file, String path) {
        this.id = file.getFileId();
        this.name = file.getOriginalName();
        this.type = file.getOriginalName().contains(".") ? file.getOriginalName().substring(file.getOriginalName().lastIndexOf(".") + 1) : "file";
        this.path = path;
        this.modifiedAt = file.getCreatedAt();
        this.ownerName = file.getOwner().getName();
        this.uniqueKey = "file-" + file.getFileId();
    }
}