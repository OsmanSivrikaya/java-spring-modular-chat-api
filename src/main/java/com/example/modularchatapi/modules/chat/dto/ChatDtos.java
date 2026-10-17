package com.example.modularchatapi.modules.chat.dto;

import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.chat.domain.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

// Chat modülünün tüm request/response kayıtları
public final class ChatDtos {
    private ChatDtos() {}

    public record CreateDirectConversationRequest(
            @NotNull(message = "Kullanıcı id boş olamaz") Long userId
    ) {}

    public record CreateGroupConversationRequest(
            @NotBlank(message = "Grup adı boş olamaz")
            @Size(max = 100, message = "Grup adı en fazla 100 karakter olabilir")
            String name,
            @NotEmpty(message = "En az bir üye seçilmeli")
            List<Long> memberIds
    ) {}

    public record SendMessageRequest(
            @NotBlank(message = "Mesaj boş olamaz")
            @Size(max = 4000, message = "Mesaj en fazla 4000 karakter olabilir")
            String content
    ) {}

    // WebSocket üzerinden gönderilen mesaj (/app/chat.send)
    public record SocketSendMessage(Long conversationId, String content) {}

    // WebSocket üzerinden "yazıyor..." bilgisi (/app/chat.typing)
    public record SocketTyping(Long conversationId, boolean typing) {}

    public record TypingEvent(Long conversationId, UserSummaryDto user, boolean typing) {}

    public record MessageResponse(
            Long id,
            Long conversationId,
            UserSummaryDto sender,
            String content,
            Instant sentAt
    ) {}

    public record ConversationResponse(
            Long id,
            String name,
            ConversationType type,
            List<UserSummaryDto> participants,
            MessageResponse lastMessage,
            Instant updatedAt
    ) {}

    public record PresenceEvent(UserSummaryDto user, boolean online) {}
}
