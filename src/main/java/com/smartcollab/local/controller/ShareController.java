package com.smartcollab.local.controller;

import com.smartcollab.local.dto.CreateShareLinkRequestDto;
import com.smartcollab.local.dto.FileResponseDto;
import com.smartcollab.local.dto.ShareLinkInfoDto; // 새로 추가
import com.smartcollab.local.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/{fileId}")
    public ResponseEntity<String> createShareLink(@PathVariable Long fileId,
                                                  @RequestBody CreateShareLinkRequestDto requestDto,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        String url = shareService.createShareLink(fileId, requestDto, userDetails.getUsername());
        // 사용자가 접속할 페이지 주소로 변경
        String userAccessUrl = "/share/" + url;
        return ResponseEntity.ok(userAccessUrl);
    }

    // --- [ 새로 추가 ] ---
    // 링크 정보를 확인하는 API (비밀번호 필요 여부 등)
    @GetMapping("/info/{url}")
    public ResponseEntity<ShareLinkInfoDto> getShareLinkInfo(@PathVariable String url) {
        boolean requiresPassword = shareService.doesLinkRequirePassword(url);
        return ResponseEntity.ok(new ShareLinkInfoDto(requiresPassword));
    }

    @GetMapping("/download/{url}")
    public ResponseEntity<?> downloadSharedFile(@PathVariable String url,
                                                @RequestParam(required = false) String password) {
        try {
            Resource resource = shareService.getSharedFile(url, password);
            String originalName = shareService.getOriginalFilenameFromStored(resource.getFilename());

            String encodedOriginalName = URLEncoder.encode(originalName, StandardCharsets.UTF_8);
            String headerValue = "attachment; filename=\"" + encodedOriginalName + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
        } catch (SecurityException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<List<FileResponseDto>> getSharedWithMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shareService.getSharedWithMe(userDetails.getUsername()));
    }
}