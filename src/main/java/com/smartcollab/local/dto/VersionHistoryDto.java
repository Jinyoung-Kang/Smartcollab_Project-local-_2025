package com.smartcollab.local.dto;

import com.smartcollab.local.entity.FileVersion;
import com.smartcollab.local.entity.Signature;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VersionHistoryDto {
    private final Long versionId;
    private final LocalDateTime createdAt;
    private final List<String> signers;
    private final boolean isValid;
    private final String editorName; // 작업자 이름 필드

    public VersionHistoryDto(FileVersion version, List<Signature> signatures) {
        this.versionId = version.getVersionId();
        this.createdAt = version.getCreatedAt();

        // FileVersion 엔티티에서 editor(User) 정보를 가져와 이름을 설정
        if (version.getEditor() != null) {
            this.editorName = version.getEditor().getName();
        } else {
            this.editorName = "알 수 없음";
        }

        // 해당 버전에 대한 유효한 서명자 목록만 필터링
        this.signers = signatures.stream()
                .filter(sig -> sig.getFileVersion().getVersionId().equals(version.getVersionId()) && sig.isValid())
                .map(sig -> sig.getSigner().getName())
                .collect(Collectors.toList());

        // 해당 버전에 대한 서명 중 하나라도 유효하지 않은 것이 있다면, 이 버전의 서명은 유효하지 않음 (선택적 로직)
        this.isValid = signatures.stream()
                .filter(sig -> sig.getFileVersion().getVersionId().equals(version.getVersionId()))
                .allMatch(Signature::isValid);
    }
}