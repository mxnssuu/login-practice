package com.example.loginpractice.service;

import com.example.loginpractice.config.JwtUtil;
import com.example.loginpractice.dto.LoginRequest;
import com.example.loginpractice.dto.SignupRequest;
import com.example.loginpractice.entity.AppUser;
import com.example.loginpractice.entity.Role;
import com.example.loginpractice.exception.AuthException;
import com.example.loginpractice.repository.AppUserRepository;
import com.example.loginpractice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final JwtUtil jwtUtil;

    @Transactional
    public UUID signup(SignupRequest req) {
        String email = req.getEmail().toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "AUTH_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다");
        }

        Role role = roleRepository.findByCode("TRAINEE")
                .orElseThrow(() -> new IllegalStateException("역할이 없습니다"));

        AppUser user = new AppUser(
                null,
                email,
                passwordEncoder.encode(req.getPassword()),
                req.getName(),
                req.getPhone(),
                role
        );

        return appUserRepository.save(user).getUserId();
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest req) {
        String email = req.getEmail().toLowerCase();

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        return jwtUtil.createToken(user.getEmail(), user.getRole().getCode());
    }
}