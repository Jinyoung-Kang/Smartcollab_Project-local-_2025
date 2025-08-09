package com.smartcollab.local.service;

import com.smartcollab.local.dto.CreateShareLinkRequestDto;
import com.smartcollab.local.dto.FileResponseDto;
import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.ShareLink;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.FileRepository;
import com.smartcollab.local.repository.ShareLinkRepository;
import com.smartcollab.local.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {
    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;

    public boolean doesLinkRequirePassword(String url) {
        ShareLink shareLink = shareLinkRepository.findByUrl(url)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 링크입니다."));
        if (shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 링크입니다.");
        }
        return StringUtils.hasText(shareLink.getPassword());
    }

    public String getOriginalFilenameFromStored(String storedName) {
        if (storedName != null && storedName.contains("_")) {
            return storedName.substring(storedName.indexOf("_") + 1);
        }
        return storedName;
    }

    @Transactional
    public String createShareLink(Long fileId, CreateShareLinkRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        if (!file.getOwner().getUserId().equals(user.getUserId())) {
            throw new SecurityException("공유 링크를 생성할 권한이 없습니다.");
        }

        User sharedWithUser = null;
        if (StringUtils.hasText(requestDto.getSharedWithUsername())) {
            sharedWithUser = userRepository.findByUsername(requestDto.getSharedWithUsername())
                    .orElseThrow(() -> new IllegalArgumentException("공유할 사용자를 찾을 수 없습니다."));
        }

        String url = UUID.randomUUID().toString();
        String encodedPassword = null;
        if (StringUtils.hasText(requestDto.getPassword())) {
            encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        }

        ShareLink shareLink = ShareLink.builder()
                .file(file)
                .owner(user)
                .sharedWithUser(sharedWithUser)
                .url(url)
                .expiresAt(requestDto.getExpiresAt())
                .downloadLimit(requestDto.getDownloadLimit())
                .password(encodedPassword)
                .build();
        shareLinkRepository.save(shareLink);
        return url;
    }

    @Transactional
    public Resource getSharedFile(String url, String password) {
        ShareLink shareLink = shareLinkRepository.findByUrl(url)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 링크입니다."));

        if (StringUtils.hasText(shareLink.getPassword())) {
            if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, shareLink.getPassword())) {
                throw new SecurityException("비밀번호가 일치하지 않습니다.");
            }
        }

        if (shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            shareLinkRepository.delete(shareLink);
            throw new RuntimeException("만료된 링크입니다.");
        }

        if (shareLink.getDownloadLimit() != null) {
            if (shareLink.getDownloadCount() >= shareLink.getDownloadLimit()) {
                throw new RuntimeException("다운로드 횟수를 초과했습니다.");
            }
            shareLink.incrementDownloadCount();
            if(shareLink.getDownloadCount() >= shareLink.getDownloadLimit()){
                shareLinkRepository.delete(shareLink);
            } else {
                shareLinkRepository.save(shareLink);
            }
        }

        return fileService.downloadFile(shareLink.getFile().getFileId());
    }

    @Transactional(readOnly = true)
    public List<FileResponseDto> getSharedWithMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return shareLinkRepository.findBySharedWithUser(user).stream()
                .map(shareLink -> new FileResponseDto(shareLink.getFile()))
                .collect(Collectors.toList());
    }
}