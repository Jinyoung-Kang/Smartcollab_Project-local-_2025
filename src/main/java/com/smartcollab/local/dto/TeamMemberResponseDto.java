package com.smartcollab.local.dto;

import com.smartcollab.local.entity.TeamMember;
import lombok.Getter;

@Getter
public class TeamMemberResponseDto {
    private final Long memberId;
    private final String username;
    private final String name;
    private final boolean isTeamLeader;
    private final boolean canEdit;
    private final boolean canDelete;
    private final boolean canInvite;

    public TeamMemberResponseDto(TeamMember teamMember) {
        this.memberId = teamMember.getId();
        this.username = teamMember.getUser().getUsername();
        this.name = teamMember.getUser().getName();
        this.isTeamLeader = teamMember.isTeamLeader();
        this.canEdit = teamMember.isCanEdit();
        this.canDelete = teamMember.isCanDelete();
        this.canInvite = teamMember.isCanInvite();
    }
}
