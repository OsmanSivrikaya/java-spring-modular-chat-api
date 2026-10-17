package com.example.modularchatapi.modules.auth.service;

import com.example.modularchatapi.common.exception.ApiException;
import com.example.modularchatapi.common.security.JwtService;
import com.example.modularchatapi.modules.auth.domain.Role;
import com.example.modularchatapi.modules.auth.domain.User;
import com.example.modularchatapi.modules.auth.dto.request.LoginRequest;
import com.example.modularchatapi.modules.auth.dto.request.RegisterRequest;
import com.example.modularchatapi.modules.auth.dto.response.AuthResponse;
import com.example.modularchatapi.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Bu email zaten kayıtlı");
        }
        User user = User.builder()
                .username(request.username().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Şifre yanlışsa BadCredentialsException fırlatır -> GlobalExceptionHandler 401 döner
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email).orElseThrow();
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user, Map.of(
                "uid", user.getId(),
                "role", user.getRole().name()));
        return AuthResponse.of(token, user.getEmail(), user.getDisplayName());
    }
}
