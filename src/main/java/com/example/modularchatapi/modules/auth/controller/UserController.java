package com.example.modularchatapi.modules.auth.controller;

import com.example.modularchatapi.modules.auth.domain.User;
import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // .NET'teki User.Claims karşılığı: giriş yapmış kullanıcı
    @GetMapping("/me")
    public UserSummaryDto me(@AuthenticationPrincipal User user) {
        return UserService.toSummary(user);
    }

    @GetMapping("/search")
    public List<UserSummaryDto> search(@RequestParam String q) {
        return userService.search(q);
    }
}
