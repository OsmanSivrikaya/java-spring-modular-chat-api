package com.example.modularchatapi.modules.chat.controller;

import com.example.modularchatapi.modules.auth.api.UserQueryApi;
import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.chat.websocket.PresenceTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceTracker presenceTracker;
    private final UserQueryApi userQueryApi;

    @GetMapping("/online")
    public List<UserSummaryDto> online() {
        return presenceTracker.onlineEmails().stream()
                .map(userQueryApi::findByEmail)
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
