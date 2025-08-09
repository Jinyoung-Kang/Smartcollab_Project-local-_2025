package com.smartcollab.local.config;

import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 'deleted_user' 계정이 없으면 생성
        if (!userRepository.existsByUsername("deleted_user")) {
            User deletedUser = User.builder()
                    .username("deleted_user")
                    .password(passwordEncoder.encode("a_very_long_and_unusable_password")) // 아무도 로그인 못하게 임의의 값 설정
                    .name("탈퇴한 사용자")
                    .email("deleted@user.local")
                    .role(User.Role.USER)
                    .build();
            userRepository.save(deletedUser);
        }
    }
}