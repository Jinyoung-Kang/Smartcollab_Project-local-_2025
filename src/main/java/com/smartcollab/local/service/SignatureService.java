package com.smartcollab.local.service;

import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.FileVersion;
import com.smartcollab.local.entity.Signature;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.FileRepository;
import com.smartcollab.local.repository.FileVersionRepository;
import com.smartcollab.local.repository.SignatureRepository;
import com.smartcollab.local.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for handling electronic signature logic.
 */
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;

    /**
     * Signs the latest version of a file.
     * @param fileId The ID of the file.
     * @param username The username of the signer.
     */
    @Transactional
    public void signLatestVersionOfFile(Long fileId, String username) {
        User signer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        // 파일의 가장 최신 버전을 가져옴
        FileVersion latestVersion = fileVersionRepository.findTopByFileOrderByVersionIdDesc(file)
                .orElseThrow(() -> new RuntimeException("서명할 파일 버전이 존재하지 않습니다."));

        // 팀장 또는 소유자만 서명 가능
        if (file.getFolder().getTeam() != null) {
            User teamOwner = file.getFolder().getTeam().getOwner();
            if (!teamOwner.getUserId().equals(signer.getUserId())) {
                throw new SecurityException("팀장만 팀 파일에 서명할 수 있습니다.");
            }
        } else {
            if (!file.getOwner().getUserId().equals(signer.getUserId())) {
                throw new SecurityException("파일 소유자만 서명할 수 있습니다.");
            }
        }

        Signature signature = Signature.builder()
                .file(file)
                .fileVersion(latestVersion)
                .signer(signer)
                .build();
        signatureRepository.save(signature);
    }

    /**
     * Invalidates all signatures for a given file.
     * This is called when a new version is saved.
     * @param file The file whose signatures to invalidate.
     */
    @Transactional
    public void invalidateSignatures(FileEntity file) {
        List<Signature> signatures = signatureRepository.findByFile(file);
        signatures.forEach(Signature::invalidate);
        signatureRepository.saveAll(signatures);
    }
}
