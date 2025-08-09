package com.smartcollab.local.dto;

import com.smartcollab.local.entity.FileEntity;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class FileResponseDto {
    private final Long id;
    private final String name;
    private final Long size;
    private final LocalDateTime createdAt;

    public FileResponseDto(FileEntity fileEntity) {
        this.id = fileEntity.getFileId();
        this.name = fileEntity.getOriginalName();
        this.size = fileEntity.getSize();
        this.createdAt = fileEntity.getCreatedAt();
    }
}
