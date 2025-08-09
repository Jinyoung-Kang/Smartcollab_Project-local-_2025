package com.smartcollab.local.service;

import com.smartcollab.local.dto.UserResponseDto;
import com.smartcollab.local.entity.Folder;
import com.smartcollab.local.entity.Invitation;
import com.smartcollab.local.entity.Notification;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InvitationRepository invitationRepository;
    private final NotificationRepository notificationRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final SignatureRepository signatureRepository;
    private final FileRepository fileRepository;
    @Lazy
    private final FolderService folderService;

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserInfo(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserResponseDto(user);
    }

    @Transactional
    public void deleteUser(String username) {
        User userToDelete = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("탈퇴할 사용자를 찾을 수 없습니다."));

        // 1. 사용자가 팀장인 팀이 있는지 확인
        boolean isTeamLeader = userToDelete.getLedTeams().stream().anyMatch(team -> team.getOwner().equals(userToDelete));
        if (isTeamLeader) {
            throw new IllegalStateException("팀장으로 있는 팀이 있어 탈퇴할 수 없습니다. 먼저 모든 팀의 팀장직을 다른 팀원에게 위임해주세요.");
        }

        // 2. '탈퇴한 사용자' 계정을 조회
        User deletedUser = userRepository.findByUsername("deleted_user")
                .orElseThrow(() -> new IllegalStateException("'deleted_user' 계정을 찾을 수 없습니다."));

        // 3. 사용자와 관련된 모든 자식 데이터를 순서대로 정리합니다.

        // 3-1. 사용자가 보내거나 받은 모든 초대를 찾습니다.
        List<Invitation> relatedInvitations = invitationRepository.findAllByInviterOrInvitee(userToDelete, userToDelete);

        // 3-2. 위 초대들을 참조하는 모든 알림을 먼저 삭제합니다.
        for (Invitation invitation : relatedInvitations) {
            notificationRepository.deleteAll(notificationRepository.findByInvitation(invitation));
        }

        // 3-3. 이제 안전하게 초대 기록을 삭제합니다.
        invitationRepository.deleteAll(relatedInvitations);

        // 3-4. 사용자의 나머지 모든 알림을 삭제합니다.
        notificationRepository.deleteAllByUser(userToDelete);

        // 3-5. 사용자가 생성한 공유 링크 및 서명 삭제
        shareLinkRepository.deleteAllByOwner(userToDelete);
        signatureRepository.deleteAllBySigner(userToDelete);

        // 4. 팀 스토리지에 남겨진 데이터의 소유권을 '탈퇴한 사용자'에게 이전
        folderRepository.findByOwnerAndTeamIsNotNull(userToDelete).forEach(folder -> folder.setOwner(deletedUser));
        fileRepository.findByOwnerAndFolder_TeamIsNotNull(userToDelete).forEach(file -> file.setOwner(deletedUser));
        chatMessageRepository.findBySender(userToDelete).forEach(message -> message.setSender(deletedUser));

        // 5. 개인 스토리지의 모든 파일/폴더 영구 삭제
        List<Folder> personalRootFolders = folderRepository.findByOwnerAndTeamIsNullAndParentFolderIsNull(userToDelete);
        for (Folder folder : personalRootFolders) {
            folderService.deleteFolderPermanently(folder.getFolderId(), userToDelete);
        }

        // 6. 마지막으로 사용자 정보를 삭제합니다.
        // User 엔티티의 @OneToMany(cascade=ALL) 설정에 의해 TeamMember 기록은 자동으로 삭제됩니다.
        userRepository.delete(userToDelete);
    }
}