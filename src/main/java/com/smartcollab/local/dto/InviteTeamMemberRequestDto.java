package com.smartcollab.local.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteTeamMemberRequestDto {
    @NotBlank
    private String username;
}
