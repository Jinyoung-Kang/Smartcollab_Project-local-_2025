package com.smartcollab.local.repository;

import com.smartcollab.local.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Team 엔티티를 위한 Repository 인터페이스.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {
}
