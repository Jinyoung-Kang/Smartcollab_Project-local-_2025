package com.smartcollab.local.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MoveRequestDto {
    private List<String> itemUniqueKeys; // e.g., ["file-1", "folder-3"]
    private Long destinationFolderId;
}