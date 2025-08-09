package com.smartcollab.local.repository;

import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.Signature;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Signature 엔티티를 위한 Repository 인터페이스.
 */
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    // 특정 파일에 대한 모든 서명을 찾는 메서드
    List<Signature> findByFile(FileEntity file);

    // 특정 서명자의 모든 서명을 삭제하는 메서드
    void deleteAllBySigner(User signer);
}