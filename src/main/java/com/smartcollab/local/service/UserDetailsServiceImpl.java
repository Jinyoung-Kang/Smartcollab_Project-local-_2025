package com.smartcollab.local.service;

import com.smartcollab.local.entity.User;
import com.smartcollab.local.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security가 사용자 정보를 조회할 때 사용하는 서비스.
 * UserDetailsService 인터페이스를 구현합니다.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 사용자 이름으로 User 엔티티를 찾아서 UserDetails 객체로 변환
        return userRepository.findByUsername(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 유저를 찾을 수 없습니다: " + username));
    }

    private UserDetails createUserDetails(User user) {
        // 사용자의 역할을 기반으로 권한 생성
        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // Spring Security가 사용하는 UserDetails 객체 반환
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(grantedAuthority)
        );
    }
}
