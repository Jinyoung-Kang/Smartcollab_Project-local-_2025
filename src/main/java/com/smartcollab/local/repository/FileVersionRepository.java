package com.smartcollab.local.repository;
import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // [추가]
import java.util.Optional;

/**
 * FileVersion 엔티티를 위한 Repository 인터페이스.
 */
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    // 특정 파일의 가장 최신 버전을 찾는 메서드
    Optional<FileVersion> findTopByFileOrderByVersionIdDesc(FileEntity file);

    // 특정 파일의 가장 최신 2개 버전을 찾는 메서드
    List<FileVersion> findTop2ByFileOrderByVersionIdDesc(FileEntity file);
}
