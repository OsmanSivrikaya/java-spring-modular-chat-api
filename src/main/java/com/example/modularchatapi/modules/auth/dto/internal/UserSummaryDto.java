package com.example.modularchatapi.modules.auth.dto.internal;

public record UserSummaryDto(
        Long id,
        String username,
        String email
) {}
