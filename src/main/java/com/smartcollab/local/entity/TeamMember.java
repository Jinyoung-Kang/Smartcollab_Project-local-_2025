package com.smartcollab.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자와 팀의 관계를 정의하고, 멤버별 권한을 저장하는 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long id;

    // 소속된 팀 (Team과 다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // 멤버인 사용자 (User와 다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean isTeamLeader = false;

    @Column(nullable = false)
    private boolean canEdit = false;

    @Column(nullable = false)
    private boolean canDelete = false;

    @Column(nullable = false)
    private boolean canInvite = false;

    @Builder
    public TeamMember(Team team, User user, boolean isTeamLeader, boolean canEdit, boolean canDelete, boolean canInvite) {
        this.team = team;
        this.user = user;
        this.isTeamLeader = isTeamLeader;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        this.canInvite = canInvite;
    }

    public void updatePermissions(boolean canEdit, boolean canDelete, boolean canInvite) {
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        this.canInvite = canInvite;
    }

    public void promoteToLeader() {
        this.isTeamLeader = true;
        this.canEdit = true;
        this.canDelete = true;
        this.canInvite = true;
    }

    public void demoteFromLeader() {
        this.isTeamLeader = false;
        // 팀장직을 내려놓은 후의 기본 권한 설정 (여기서는 편집 권한만 유지)
        this.canDelete = false;
        this.canInvite = false;
    }


}
