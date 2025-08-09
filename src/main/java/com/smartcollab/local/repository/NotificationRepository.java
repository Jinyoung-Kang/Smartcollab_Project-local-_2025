package com.smartcollab.local.repository;

import com.smartcollab.local.entity.Invitation;
import com.smartcollab.local.entity.Notification;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Notification 엔티티를 위한 Repository 인터페이스.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 사용자의 읽지 않은 알림 목록을 최신순으로 정렬하여 찾는 메서드
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // 특정 초대를 참조하는 모든 알림을 찾는 메소드
    List<Notification> findByInvitation(Invitation invitation);

    // 특정 사용자의 모든 알림을 삭제하는 메소드
    void deleteAllByUser(User user);
}