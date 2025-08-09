package com.smartcollab.local.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FolderContentDto {
    private final List<DashboardItemDto> items;
    private final List<BreadcrumbDto> path;
}