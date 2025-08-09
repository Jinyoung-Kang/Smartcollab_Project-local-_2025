package com.smartcollab.local.repository;

import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.ShareLink;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * ShareLink 엔티티를 위한 Repository 인터페이스.
 */
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    // 고유 URL로 공유 링크를 찾는 메서드
    Optional<ShareLink> findByUrl(String url);

    // 특정 사용자에게 공유된 모든 링크를 찾는 메서드
    List<ShareLink> findBySharedWithUser(User user);

    // 특정 파일과 연결된 모든 공유 링크를 찾는 메서드
    List<ShareLink> findByFile(FileEntity file);

    // 특정 소유자의 모든 공유 링크를 삭제하는 메서드
    void deleteAllByOwner(User owner);
}