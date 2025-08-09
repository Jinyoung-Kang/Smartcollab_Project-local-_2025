package com.smartcollab.local.service;

import com.smartcollab.local.entity.*;
import com.smartcollab.local.repository.InvitationRepository;
import com.smartcollab.local.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    public void acceptInvitation(Long invitationId, User currentUser) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));

        if (!invitation.getInvitee().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("초대를 수락할 권한이 없습니다.");
        }
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 초대입니다.");
        }

        invitation.updateStatus(Invitation.InvitationStatus.ACCEPTED);

        TeamMember newMember = TeamMember.builder()
                .team(invitation.getTeam())
                .user(currentUser)
                .isTeamLeader(false)
                .canEdit(true)
                .canDelete(false)
                .canInvite(false)
                .build();
        teamMemberRepository.save(newMember);

        // 초대한 사람에게 수락 알림 보내기
        String content = currentUser.getName() + "님이 '" + invitation.getTeam().getName() + "' 팀 초대를 수락했습니다.";
        notificationService.createNotification(invitation.getInviter(), content, Notification.NotificationType.INVITE_ACCEPTED, null);
    }

    @Transactional
    public void rejectInvitation(Long invitationId, User currentUser) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));

        if (!invitation.getInvitee().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("초대를 거절할 권한이 없습니다.");
        }
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 초대입니다.");
        }

        invitation.updateStatus(Invitation.InvitationStatus.REJECTED);

        // 초대한 사람에게 거절 알림 보내기
        String content = currentUser.getName() + "님이 '" + invitation.getTeam().getName() + "' 팀 초대를 거절했습니다.";
        notificationService.createNotification(invitation.getInviter(), content, Notification.NotificationType.INVITE_REJECTED, null);
    }
}