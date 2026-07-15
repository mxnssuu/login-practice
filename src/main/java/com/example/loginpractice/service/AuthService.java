package com.example.loginpractice.service;

import com.example.loginpractice.dto.SignupRequest;
import com.example.loginpractice.entity.AppUser;
import com.example.loginpractice.entity.Role;
import com.example.loginpractice.repository.AppUserRepository;
import com.example.loginpractice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID signup(SignupRequest req) {
        String email = req.getEmail().toLowerCase();   // CITEXT 대체

        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        Role role = roleRepository.findByCode("TRAINEE")
                .orElseThrow(() -> new IllegalStateException("역할이 없습니다"));

        AppUser user = new AppUser(
                null,
                email,
                passwordEncoder.encode(req.getPassword()),   // 암호화
                req.getName(),
                req.getPhone(),
                role
        );

        return appUserRepository.save(user).getUserId();
    }
}