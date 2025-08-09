package com.smartcollab.local.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFolderRequestDto {
    @NotBlank(message = "Folder name cannot be empty")
    private String folderName;
    private Long parentFolderId;
    private Long teamId;
}
