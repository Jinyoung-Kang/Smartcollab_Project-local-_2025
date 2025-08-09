package com.smartcollab.local.controller;

import com.smartcollab.local.dto.LoginRequestDto;
import com.smartcollab.local.dto.LoginResponseDto;
import com.smartcollab.local.dto.SignUpRequestDto;
import com.smartcollab.local.dto.UserResponseDto;
import com.smartcollab.local.service.AuthService;
import com.smartcollab.local.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user authentication (signup, login) and fetching user information.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * API endpoint for user registration.
     * @param signUpRequestDto DTO containing signup information.
     * @return A success message.
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        authService.signUp(signUpRequestDto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    /**
     * API endpoint for user login.
     * @param loginRequestDto DTO containing login credentials.
     * @return A response containing the JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        LoginResponseDto response = authService.login(loginRequestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint to get the current logged-in user's information.
     * @param userDetails User details extracted from the JWT.
     * @return A DTO with user information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto userInfo = userService.getCurrentUserInfo(userDetails);
        return ResponseEntity.ok(userInfo);
    }
}
