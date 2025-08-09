package com.smartcollab.local.controller;

import com.smartcollab.local.dto.CreateFolderRequestDto;
import com.smartcollab.local.dto.FolderTreeDto;
import com.smartcollab.local.dto.RenameFileRequestDto;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.UserRepository;
import com.smartcollab.local.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 폴더 관리를 위한  Controller.
 */
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> createFolder(@Valid @RequestBody CreateFolderRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        folderService.createFolder(requestDto, userDetails);
        return ResponseEntity.ok("폴더가 성공적으로 생성되었습니다.");
    }

    @GetMapping("/tree")
    public ResponseEntity<List<FolderTreeDto>> getFolderTree(
            @RequestParam(required = false) Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FolderTreeDto> folderTree;
        if (teamId != null) {
            folderTree = folderService.getFolderTreeForTeam(teamId);
        } else {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            folderTree = folderService.getFolderTreeForUser(user);
        }
        return ResponseEntity.ok(folderTree);
    }

    @PutMapping("/{folderId}/rename")
    public ResponseEntity<String> renameFolder(@PathVariable Long folderId,
                                               @RequestBody RenameFileRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        folderService.renameFolder(folderId, requestDto.getNewName(), user);
        return ResponseEntity.ok("이름이 변경되었습니다.");
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<String> deleteFolder(@PathVariable Long folderId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        folderService.deleteFolderPermanently(folderId, user);
        return ResponseEntity.ok("폴더가 성공적으로 삭제되었습니다.");
    }
}