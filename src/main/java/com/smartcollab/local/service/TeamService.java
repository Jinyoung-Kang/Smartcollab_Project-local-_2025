package com.smartcollab.local.service;

import com.smartcollab.local.dto.ChatMessageDto;
import com.smartcollab.local.dto.CreateTeamRequestDto;
import com.smartcollab.local.dto.InviteTeamMemberRequestDto;
import com.smartcollab.local.dto.UpdatePermissionRequestDto;
import com.smartcollab.local.entity.ChatMessage;
import com.smartcollab.local.entity.Folder;
import com.smartcollab.local.entity.Invitation;
import com.smartcollab.local.entity.Notification;
import com.smartcollab.local.entity.Team;
import com.smartcollab.local.entity.TeamMember;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.ChatMessageRepository;
import com.smartcollab.local.repository.FolderRepository;
import com.smartcollab.local.repository.InvitationRepository;
import com.smartcollab.local.repository.NotificationRepository;
import com.smartcollab.local.repository.TeamMemberRepository;
import com.smartcollab.local.repository.TeamRepository;
import com.smartcollab.local.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for handling team-related business logic,
 * such as creation, member management, and permissions.
 */
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final FolderRepository folderRepository;
    @Lazy // 순환 참조 방지
    private final FolderService folderService;
    private final InvitationRepository invitationRepository;
    private final NotificationRepository notificationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Creates a new team and sets the creator as the team leader.
     * Also creates a root folder for the team.
     *
     * @param requestDto DTO containing the team name.
     * @param currentUserDetails The currently authenticated user.
     */
    @Transactional
    public void createTeam(CreateTeamRequestDto requestDto, UserDetails currentUserDetails) {
        User owner = userRepository.findByUsername(currentUserDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team newTeam = Team.builder()
                .name(requestDto.getTeamName())
                .owner(owner)
                .build();
        teamRepository.save(newTeam);

        // 팀 생성 시, 해당 팀의 루트 폴더를 함께 생성합니다.
        Folder teamRootFolder = Folder.builder()
                .name(newTeam.getName() + "의 루트 폴더") // DB 식별용 이름
                .owner(owner) // 팀 생성자를 폴더 소유자로 지정
                .team(newTeam) // 새로 생성된 팀과 연결
                .parentFolder(null) // 최상위 폴더이므로 부모는 없음
                .build();
        folderRepository.save(teamRootFolder);

        // The creator automatically becomes the team leader with all permissions.
        TeamMember teamLeader = TeamMember.builder()
                .team(newTeam)
                .user(owner)
                .isTeamLeader(true)
                .canEdit(true)
                .canDelete(true)
                .canInvite(true)
                .build();
        teamMemberRepository.save(teamLeader);
    }

    /**
     * Creates an invitation for a user to join a team and sends a notification.
     *
     * @param teamId The ID of the team.
     * @param requestDto DTO containing the username of the user to invite.
     * @param currentUserDetails The user performing the invitation.
     */
    @Transactional
    public void inviteMember(Long teamId, InviteTeamMemberRequestDto requestDto, UserDetails currentUserDetails) {
        User inviter = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        User invitee = userRepository.findByUsername(requestDto.getUsername()).orElseThrow(() -> new IllegalArgumentException("초대할 사용자를 찾을 수 없습니다."));

        TeamMember inviterMembership = team.getTeamMembers().stream()
                .filter(member -> member.getUser().getUserId().equals(inviter.getUserId()))
                .findFirst()
                .orElseThrow(() -> new SecurityException("팀 멤버가 아니므로 초대할 수 없습니다."));

        if (!inviterMembership.isCanInvite()) {
            throw new SecurityException("팀원 초대 권한이 없습니다.");
        }

        boolean isAlreadyMember = team.getTeamMembers().stream()
                .anyMatch(member -> member.getUser().getUserId().equals(invitee.getUserId()));
        if (isAlreadyMember) {
            throw new IllegalArgumentException("이미 팀에 속해있는 사용자입니다.");
        }

        // 바로 팀원으로 추가하는 대신, 초대장을 생성합니다.
        Invitation invitation = Invitation.builder()
                .team(team)
                .inviter(inviter)
                .invitee(invitee)
                .build();
        invitationRepository.save(invitation);

        // 초대받는 사람에게 알림을 보냅니다.
        String notificationContent = inviter.getName() + "님이 '" + team.getName() + "' 팀에 초대했습니다.";
        notificationService.createNotification(invitee, notificationContent, Notification.NotificationType.TEAM_INVITE, invitation);
    }

    /**
     * Retrieves all members of a specific team.
     *
     * @param teamId The ID of the team.
     * @return A list of team members.
     */
    @Transactional(readOnly = true)
    public List<TeamMember> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        return team.getTeamMembers();
    }

    /**
     * Updates the permissions of a team member. Only the team leader can do this.
     *
     * @param teamId The ID of the team.
     * @param memberId The ID of the member to update.
     * @param requestDto DTO with the new permission settings.
     * @param currentUserDetails The user performing the action.
     */
    @Transactional
    public void updateMemberPermissions(Long teamId, Long memberId, UpdatePermissionRequestDto requestDto, UserDetails currentUserDetails) {
        User currentUser = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        // Only the team owner (leader) can update permissions.
        if (!team.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("팀원 권한을 수정할 권한이 없습니다.");
        }
        TeamMember targetMember = teamMemberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("수정할 팀원을 찾을 수 없습니다."));
        if (targetMember.isTeamLeader()) {
            throw new IllegalArgumentException("팀장의 권한은 변경할 수 없습니다.");
        }
        targetMember.updatePermissions(requestDto.isCanEdit(), requestDto.isCanDelete(), requestDto.isCanInvite());
        teamMemberRepository.save(targetMember);
    }

    /**
     * Retrieves all teams that the current user is a member of.
     *
     * @param username The username of the current user.
     * @return A list of teams.
     */
    @Transactional(readOnly = true)
    public List<Team> getMyTeams(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getTeamMembers().stream()
                .map(TeamMember::getTeam)
                .collect(Collectors.toList());
    }

    /**
     * Removes a member from a team. Only the team leader can do this.
     *
     * @param teamId The ID of the team.
     * @param memberId The ID of the member to remove.
     * @param currentUserDetails The user performing the action.
     */
    @Transactional
    public void removeMember(Long teamId, Long memberId, UserDetails currentUserDetails) {
        User currentUser = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        // Only the team owner (leader) can remove members.
        if (!team.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("팀원 추방 권한이 없습니다.");
        }
        TeamMember memberToRemove = teamMemberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("추방할 팀원을 찾을 수 없습니다."));
        if (memberToRemove.isTeamLeader()) {
            throw new IllegalArgumentException("팀장은 추방할 수 없습니다.");
        }
        teamMemberRepository.delete(memberToRemove);
    }

    /**
     * Allows a user to leave a team. The team leader cannot leave.
     *
     * @param teamId The ID of the team to leave.
     * @param currentUserDetails The user leaving the team.
     */
    @Transactional
    public void leaveTeam(Long teamId, UserDetails currentUserDetails) {
        User currentUser = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        TeamMember memberToLeave = team.getTeamMembers().stream()
                .filter(member -> member.getUser().getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 팀의 멤버가 아닙니다."));
        if (memberToLeave.isTeamLeader()) {
            throw new IllegalArgumentException("팀장은 팀을 나갈 수 없습니다. 팀장 위임 또는 팀 폐쇄 기능을 이용해주세요.");
        }

        // Team 엔티티의 멤버 리스트에서도 해당 멤버를 제거하여 관계를 명확히 끊어줍니다.
        team.getTeamMembers().remove(memberToLeave);

        teamMemberRepository.delete(memberToLeave);
    }

    @Transactional
    public void delegateLeadership(Long teamId, Long newLeaderMemberId, UserDetails currentUserDetails) {
        User currentLeader = userRepository.findByUsername(currentUserDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        if (!team.getOwner().getUserId().equals(currentLeader.getUserId())) {
            throw new SecurityException("팀장만 위임할 수 있습니다.");
        }
        TeamMember memberToPromote = teamMemberRepository.findById(newLeaderMemberId)
                .orElseThrow(() -> new IllegalArgumentException("새로운 팀장을 찾을 수 없습니다."));
        TeamMember currentLeaderMembership = team.getTeamMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(currentLeader.getUserId()))
                .findFirst().orElseThrow();
        currentLeaderMembership.demoteFromLeader();
        memberToPromote.promoteToLeader();
        team.setOwner(memberToPromote.getUser());
        teamRepository.save(team);
        teamMemberRepository.save(currentLeaderMembership);
        teamMemberRepository.save(memberToPromote);
    }

    /**
     * Deletes a team and all its associated data (folders, files).
     * Only the team leader can perform this action.
     * @param teamId The ID of the team to delete.
     * @param currentUserDetails The user performing the action.
     */
    @Transactional
    public void deleteTeam(Long teamId, UserDetails currentUserDetails) {
        User currentUser = userRepository.findByUsername(currentUserDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        if (!team.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("팀장만 팀을 삭제할 수 있습니다.");
        }

        // 1. 이 팀과 관련된 모든 초대 기록을 가져옵니다.
        List<Invitation> invitations = team.getInvitations();

        // 2. 이 초대 기록들을 참조하는 모든 알림을 먼저 삭제합니다.
        for (Invitation invitation : invitations) {
            List<Notification> notifications = notificationRepository.findByInvitation(invitation);
            notificationRepository.deleteAll(notifications);
        }

        // 3. 이제 초대 기록을 삭제합니다.
        invitationRepository.deleteAll(invitations);

        // 4. 팀의 모든 폴더와 파일을 삭제합니다.
        List<Folder> rootFolders = folderRepository.findByTeamAndParentFolderIsNull(team);
        for (Folder rootFolder : rootFolders) {
            folderService.deleteFolderPermanently(rootFolder.getFolderId(), currentUser);
        }

        // 5. 마지막으로 팀을 삭제합니다.
        teamRepository.delete(team);
    }

    @Transactional
    public void saveAndBroadcastMessage(Long teamId, ChatMessageDto chatMessageDto) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        User sender = userRepository.findByUsername(chatMessageDto.getSender()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatMessage message = ChatMessage.builder()
                .team(team)
                .sender(sender)
                .content(chatMessageDto.getContent())
                .type(chatMessageDto.getType())
                .fileId(chatMessageDto.getFileId())
                .fileType(chatMessageDto.getFileType())
                .fileSize(chatMessageDto.getFileSize())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        chatMessageDto.setCreatedAt(savedMessage.getCreatedAt());

        messagingTemplate.convertAndSend(String.format("/topic/team/%s", teamId), chatMessageDto);
    }

    public void broadcastMessage(Long teamId, ChatMessageDto chatMessageDto) {
        messagingTemplate.convertAndSend(String.format("/topic/team/%s", teamId), chatMessageDto);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        return chatMessageRepository.findByTeamOrderByCreatedAtAsc(team);
    }

    @Transactional
    public void clearChatHistory(Long teamId, UserDetails currentUserDetails) {
        User user = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        if (!team.getOwner().getUserId().equals(user.getUserId())) {
            throw new SecurityException("팀장만 채팅 기록을 삭제할 수 있습니다.");
        }

        chatMessageRepository.deleteByTeam(team);
    }
}