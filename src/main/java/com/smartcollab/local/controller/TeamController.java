package com.smartcollab.local.controller;

import com.smartcollab.local.dto.CreateTeamRequestDto;
import com.smartcollab.local.dto.InviteTeamMemberRequestDto;
import com.smartcollab.local.dto.TeamMemberResponseDto;
import com.smartcollab.local.dto.TeamResponseDto;
import com.smartcollab.local.dto.UpdatePermissionRequestDto;
import com.smartcollab.local.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing teams, including creation, invitations, and member permissions.
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * API to create a new team.
     * @param requestDto DTO with the new team's name.
     * @param userDetails The currently authenticated user.
     * @return A success message.
     */
    @PostMapping
    public ResponseEntity<String> createTeam(@Valid @RequestBody CreateTeamRequestDto requestDto,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        teamService.createTeam(requestDto, userDetails);
        return ResponseEntity.ok("팀이 성공적으로 생성되었습니다.");
    }

    /**
     * API to invite a new member to a team.
     * @param teamId The ID of the team.
     * @param requestDto DTO with the username of the user to invite.
     * @param userDetails The currently authenticated user performing the invitation.
     * @return A success message.
     */
    @PostMapping("/{teamId}/invite")
    public ResponseEntity<String> inviteMember(@PathVariable Long teamId,
                                               @Valid @RequestBody InviteTeamMemberRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        teamService.inviteMember(teamId, requestDto, userDetails);
        return ResponseEntity.ok("팀원에게 초대 메시지를 보냈습니다.");
    }

    /**
     * API to get all members of a specific team.
     * @param teamId The ID of the team.
     * @return A list of team members.
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponseDto>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberResponseDto> members = teamService.getTeamMembers(teamId)
                .stream()
                .map(TeamMemberResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    /**
     * API to update a team member's permissions.
     * @param teamId The ID of the team.
     * @param memberId The ID of the member whose permissions are to be updated.
     * @param requestDto DTO with the new permission settings.
     * @param userDetails The currently authenticated user (must be team leader).
     * @return A success message.
     */
    @PutMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<String> updateMemberPermissions(@PathVariable Long teamId,
                                                          @PathVariable Long memberId,
                                                          @RequestBody UpdatePermissionRequestDto requestDto,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        teamService.updateMemberPermissions(teamId, memberId, requestDto, userDetails);
        return ResponseEntity.ok("팀원 권한이 수정되었습니다.");
    }

    /**
     * API to get all teams the current user is a member of.
     * @param userDetails The currently authenticated user.
     * @return A list of teams.
     */
    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponseDto>> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        List<TeamResponseDto> myTeams = teamService.getMyTeams(userDetails.getUsername())
                .stream()
                .map(TeamResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(myTeams);
    }

    /**
     * API to remove a member from a team.
     * @param teamId The ID of the team.
     * @param memberId The ID of the member to remove.
     * @param userDetails The currently authenticated user (must be team leader).
     * @return A success message.
     */
    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<String> removeMember(@PathVariable Long teamId,
                                               @PathVariable Long memberId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        teamService.removeMember(teamId, memberId, userDetails);
        return ResponseEntity.ok("팀원이 추방되었습니다.");
    }

    /**
     * API for a user to leave a team.
     * @param teamId The ID of the team to leave.
     * @param userDetails The currently authenticated user.
     * @return A success message.
     */
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<String> leaveTeam(@PathVariable Long teamId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        teamService.leaveTeam(teamId, userDetails);
        return ResponseEntity.ok("팀에서 탈퇴했습니다.");
    }

    @PostMapping("/{teamId}/delegate/{memberId}")
    public ResponseEntity<String> delegateLeadership(@PathVariable Long teamId,
                                                     @PathVariable Long memberId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        teamService.delegateLeadership(teamId, memberId, userDetails);
        return ResponseEntity.ok("팀장이 위임되었습니다.");
    }

    /**
     * API for a team leader to delete a team.
     * This will delete all associated folders and files.
     * @param teamId The ID of the team to delete.
     * @param userDetails The currently authenticated user (must be team leader).
     * @return A success message.
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<String> deleteTeam(@PathVariable Long teamId,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        teamService.deleteTeam(teamId, userDetails);
        return ResponseEntity.ok("팀이 성공적으로 삭제되었습니다.");
    }
}