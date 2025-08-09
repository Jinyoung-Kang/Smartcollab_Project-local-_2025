package com.smartcollab.local.repository;

import com.smartcollab.local.entity.FileEntity;
import com.smartcollab.local.entity.Folder;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * FileEntity를 위한 Repository 인터페이스.
 */
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    // 특정 사용자의 휴지통에 있는 파일 목록을 찾는 메서드
    List<FileEntity> findByOwnerAndIsDeletedTrue(User owner);
    // 특정 폴더에 속한 (삭제되지 않은) 파일 목록을 찾는 메서드
    List<FileEntity> findByFolderAndIsDeletedFalse(Folder folder);
    // 특정 사용자가 소유한 팀 파일 목록을 찾는 메서드
    List<FileEntity> findByOwnerAndFolder_TeamIsNotNull(User owner);

    List<FileEntity> findByFolderAndOriginalNameContainingIgnoreCase(Folder folder, String query);
}