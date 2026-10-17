package com.example.modularchatapi.modules.chat.controller;

import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.chat.dto.ChatDtos.*;
import com.example.modularchatapi.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;

    @GetMapping
    public List<ConversationResponse> list(Principal principal) {
        return chatService.listConversations(me(principal).id());
    }

    @GetMapping("/{id}")
    public ConversationResponse get(@PathVariable Long id, Principal principal) {
        return chatService.getConversation(me(principal).id(), id);
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createDirect(@Valid @RequestBody CreateDirectConversationRequest request, Principal principal) {
        return chatService.createDirect(me(principal).id(), request.userId());
    }

    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createGroup(@Valid @RequestBody CreateGroupConversationRequest request, Principal principal) {
        return chatService.createGroup(me(principal).id(), request);
    }

    // Mesaj geçmişi (en yeniden eskiye, sayfalı)
    @GetMapping("/{id}/messages")
    public Page<MessageResponse> messages(@PathVariable Long id,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "30") int size,
                                          Principal principal) {
        return chatService.getMessages(me(principal).id(), id, page, size);
    }

    // Socket kullanamayan istemciler için REST ile mesaj gönderme (yine socket'e yayınlanır)
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request, Principal principal) {
        return chatService.sendMessage(me(principal), id, request.content());
    }

    private UserSummaryDto me(Principal principal) {
        return chatService.requireUser(principal.getName());
    }
}
