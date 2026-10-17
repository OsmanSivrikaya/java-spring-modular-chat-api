package com.example.modularchatapi.modules.chat.controller;

import com.example.modularchatapi.common.exception.ApiException;
import com.example.modularchatapi.modules.chat.dto.ChatDtos.SocketSendMessage;
import com.example.modularchatapi.modules.chat.dto.ChatDtos.SocketTyping;
import com.example.modularchatapi.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

// Benim SignalR Hub'ım bu. Hub'da public metot yazınca client çağırabiliyordu,
// burada @MessageMapping yazıyorum. Client "/app/chat.send" adresine mesaj yollayınca send() çalışıyor.
// Principal: bağlanan kullanıcı, JwtStompInterceptor'da set ettiğim kişi (Context.User gibi).
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(@Payload SocketSendMessage message, Principal principal) {
        if (message.conversationId() == null) {
            throw ApiException.badRequest("conversationId zorunlu");
        }
        chatService.sendMessage(chatService.requireUser(principal.getName()), message.conversationId(), message.content());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload SocketTyping event, Principal principal) {
        if (event.conversationId() == null) return;
        chatService.sendTyping(chatService.requireUser(principal.getName()), event.conversationId(), event.typing());
    }

    // Hatalar sadece hatayı yapan kullanıcıya gider: /user/queue/errors
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> handleError(Exception ex) {
        return Map.of("error", ex instanceof ApiException ? ex.getMessage() : "Beklenmeyen hata");
    }
}
