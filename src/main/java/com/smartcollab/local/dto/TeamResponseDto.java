package com.smartcollab.local.dto;

import com.smartcollab.local.entity.Team;
import lombok.Getter;

@Getter
public class TeamResponseDto {
    private final Long id;
    private final String name;
    private final int memberCount;
    private final String ownerUsername; // --- [ 새로 추가 ] ---

    public TeamResponseDto(Team team) {
        this.id = team.getTeamId();
        this.name = team.getName();
        this.memberCount = team.getTeamMembers().size();
        this.ownerUsername = team.getOwner().getUsername(); // --- [ 새로 추가 ] ---
    }
}