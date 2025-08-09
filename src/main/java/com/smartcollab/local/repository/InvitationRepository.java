package com.smartcollab.local.repository;

import com.smartcollab.local.entity.Invitation;
import com.smartcollab.local.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findAllByInviterOrInvitee(User inviter, User invitee);
}