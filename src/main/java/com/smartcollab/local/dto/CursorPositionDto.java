package com.smartcollab.local.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPositionDto {
    private Long teamId;
    private String editor;
    private int position;
}
