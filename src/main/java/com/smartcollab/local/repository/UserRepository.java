package com.smartcollab.local.repository;

import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * User 엔티티를 위한 Repository 인터페이스.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 사용자 이름으로 사용자를 찾는 메서드 (로그인 시 사용)
    Optional<User> findByUsername(String username);

    // 해당 사용자 이름이 존재하는지 확인하는 메서드 (회원가입 시 중복 체크)
    boolean existsByUsername(String username);

    // 해당 이메일이 존재하는지 확인하는 메서드 (회원가입 시 중복 체크)
    boolean existsByEmail(String email);
}
