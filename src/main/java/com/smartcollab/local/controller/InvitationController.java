package com.smartcollab.local.controller;

import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.UserRepository;
import com.smartcollab.local.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final UserRepository userRepository;

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<String> acceptInvitation(@PathVariable Long invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        invitationService.acceptInvitation(invitationId, user);
        return ResponseEntity.ok("초대를 수락했습니다.");
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<String> rejectInvitation(@PathVariable Long invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        invitationService.rejectInvitation(invitationId, user);
        return ResponseEntity.ok("초대를 거절했습니다.");
    }
}