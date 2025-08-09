package com.smartcollab.local.repository;

import com.smartcollab.local.entity.ChatMessage;
import com.smartcollab.local.entity.Team;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByTeamOrderByCreatedAtAsc(Team team);
    void deleteByTeam(Team team);
    List<ChatMessage> findBySender(User sender);
}