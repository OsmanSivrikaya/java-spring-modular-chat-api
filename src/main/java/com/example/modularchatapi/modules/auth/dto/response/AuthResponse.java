package com.example.modularchatapi.modules.auth.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        String email,
        String username
) {
    public static AuthResponse of(String token, String email, String username) {
        return new AuthResponse(token, "Bearer", email, username);
    }
}