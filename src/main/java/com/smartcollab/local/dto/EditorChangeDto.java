package com.smartcollab.local.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditorChangeDto {
    private Long teamId;
    private String content;
    private String editor;
}
