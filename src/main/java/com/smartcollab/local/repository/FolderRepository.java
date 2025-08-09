package com.smartcollab.local.repository;

import com.smartcollab.local.entity.Folder;
import com.smartcollab.local.entity.Team;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Folder 엔티티를 위한 Repository 인터페이스.
 */
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 특정 사용자의 개인 최상위 폴더 목록을 찾는 메서드 (팀 소속 X, 부모 폴더 X)
    List<Folder> findByOwnerAndTeamIsNullAndParentFolderIsNull(User owner);
    // 특정 폴더에 속한 하위 폴더 목록을 찾는 메서드
    List<Folder> findByParentFolder(Folder parentFolder);
    // 특정 팀의 최상위 폴더 목록을 찾는 메서드 (부모 폴더 X)
    List<Folder> findByTeamAndParentFolderIsNull(Team team);
    // 특정 사용자가 소유한 팀 폴더 목록을 찾는 메서드
    List<Folder> findByOwnerAndTeamIsNotNull(User owner);
}