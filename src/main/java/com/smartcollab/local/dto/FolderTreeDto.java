package com.smartcollab.local.dto;

import com.smartcollab.local.entity.Folder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class FolderTreeDto {
    private final Long id;
    private final String name;
    private final List<FolderTreeDto> children;

    public FolderTreeDto(Folder folder) {
        this.id = folder.getFolderId();
        this.name = folder.getName();
        // 자식 폴더들을 재귀적으로 DTO로 변환
        this.children = folder.getSubFolders().stream()
                .map(FolderTreeDto::new)
                .collect(Collectors.toList());
    }
}