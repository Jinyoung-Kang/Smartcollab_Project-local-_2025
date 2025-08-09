package com.smartcollab.local.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateShareLinkRequestDto {
    private LocalDateTime expiresAt;
    private Integer downloadLimit;
    private String password;
    private String sharedWithUsername;
}
