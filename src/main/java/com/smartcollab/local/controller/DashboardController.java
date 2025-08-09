package com.smartcollab.local.controller;

import com.smartcollab.local.dto.DashboardItemDto;
import com.smartcollab.local.dto.FolderContentDto;
import com.smartcollab.local.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/root")
    public ResponseEntity<List<DashboardItemDto>> getRootItems(@AuthenticationPrincipal UserDetails userDetails) {
        List<DashboardItemDto> items = dashboardService.getRootDashboardItems(userDetails.getUsername());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<FolderContentDto> getFolderContents(@PathVariable Long folderId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        FolderContentDto content = dashboardService.getFolderContents(folderId, userDetails.getUsername());
        return ResponseEntity.ok(content);
    }

    @GetMapping("/team/{teamId}/root")
    public ResponseEntity<FolderContentDto> getTeamRootItems(@PathVariable Long teamId,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        FolderContentDto items = dashboardService.getTeamRootItems(teamId, userDetails.getUsername());
        return ResponseEntity.ok(items);
    }
}