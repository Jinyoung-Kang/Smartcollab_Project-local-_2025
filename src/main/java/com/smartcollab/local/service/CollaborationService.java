package com.smartcollab.local.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 협업 세션의 사용자 상태를 관리하는 서비스.
 * (메모리 기반으로 동작하므로 기존 코드와 동일)
 */
@Service
public class CollaborationService {

    private final Map<Long, Set<String>> activeUsersByTeam = new ConcurrentHashMap<>();

    public void userJoined(Long teamId, String username) {
        activeUsersByTeam.computeIfAbsent(teamId, k -> ConcurrentHashMap.newKeySet()).add(username);
    }

    public void userLeft(Long teamId, String username) {
        if (activeUsersByTeam.containsKey(teamId)) {
            activeUsersByTeam.get(teamId).remove(username);
            if (activeUsersByTeam.get(teamId).isEmpty()) {
                activeUsersByTeam.remove(teamId);
            }
        }
    }

    public Set<String> getActiveUsers(Long teamId) {
        return activeUsersByTeam.getOrDefault(teamId, Set.of());
    }
}
