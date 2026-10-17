package com.example.modularchatapi.modules.chat.service;

import com.example.modularchatapi.common.exception.ApiException;
import com.example.modularchatapi.modules.auth.api.UserQueryApi;
import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.chat.domain.ChatMessage;
import com.example.modularchatapi.modules.chat.domain.Conversation;
import com.example.modularchatapi.modules.chat.domain.ConversationType;
import com.example.modularchatapi.modules.chat.dto.ChatDtos.*;
import com.example.modularchatapi.modules.chat.repository.ChatMessageRepository;
import com.example.modularchatapi.modules.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Sohbetin bütün iş mantığı burada. REST controller da socket controller da bunu kullanıyor.
// @Transactional(readOnly = true): sınıftaki her metot varsayılan olarak okuma transaction'ında çalışsın.
// Yazma yapan metotlara ayrıca @Transactional koydum. .NET'te SaveChanges'i elle çağırıyordum,
// burada transaction bitince değişen entity'ler otomatik kaydediliyor (dirty checking).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserQueryApi userQueryApi;
    // SignalR'daki IHubContext<ChatHub> karşılığı: servis katmanından socket'e yayın yapar
    private final SimpMessagingTemplate messagingTemplate;

    public UserSummaryDto requireUser(String email) {
        return userQueryApi.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));
    }

    @Transactional
    public ConversationResponse createDirect(Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw ApiException.badRequest("Kendinizle sohbet başlatamazsınız");
        }
        userQueryApi.findById(otherUserId).orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Optional<Conversation> existing = conversationRepository.findDirectBetween(ConversationType.DIRECT, currentUserId, otherUserId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        Instant now = Instant.now();
        Conversation c = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdBy(currentUserId)
                .participantIds(new HashSet<>(Set.of(currentUserId, otherUserId)))
                .createdAt(now)
                .updatedAt(now)
                .build());
        ConversationResponse response = toResponse(c);
        notifyParticipants(c, "/queue/conversations", response);
        return response;
    }

    @Transactional
    public ConversationResponse createGroup(Long currentUserId, CreateGroupConversationRequest request) {
        Set<Long> members = new HashSet<>(request.memberIds());
        members.add(currentUserId);
        if (userQueryApi.findAllByIds(members).size() != members.size()) {
            throw ApiException.badRequest("Üyelerden bazıları bulunamadı");
        }
        Instant now = Instant.now();
        Conversation c = conversationRepository.save(Conversation.builder()
                .name(request.name().trim())
                .type(ConversationType.GROUP)
                .createdBy(currentUserId)
                .participantIds(members)
                .createdAt(now)
                .updatedAt(now)
                .build());
        ConversationResponse response = toResponse(c);
        notifyParticipants(c, "/queue/conversations", response);
        return response;
    }

    public List<ConversationResponse> listConversations(Long userId) {
        return conversationRepository.findAllByParticipant(userId).stream().map(this::toResponse).toList();
    }

    public ConversationResponse getConversation(Long userId, Long conversationId) {
        return toResponse(requireMembership(userId, conversationId));
    }

    public Page<MessageResponse> getMessages(Long userId, Long conversationId, int page, int size) {
        requireMembership(userId, conversationId);
        Page<ChatMessage> messages = messageRepository.findByConversationIdOrderBySentAtDesc(
                conversationId, PageRequest.of(page, Math.min(Math.max(size, 1), 100)));
        Map<Long, UserSummaryDto> users = loadUsers(messages.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet()));
        return messages.map(m -> toMessageResponse(m, users.get(m.getSenderId())));
    }

    // REST ve WebSocket aynı metodu kullanır: kaydet + sohbet kanalına yayınla
    @Transactional
    public MessageResponse sendMessage(UserSummaryDto sender, Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("Mesaj boş olamaz");
        }
        if (content.length() > 4000) {
            throw ApiException.badRequest("Mesaj en fazla 4000 karakter olabilir");
        }
        Conversation c = requireMembership(sender.id(), conversationId);
        Instant now = Instant.now();
        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(sender.id())
                .content(content.trim())
                .sentAt(now)
                .build());
        // save çağırmıyorum, transaction bitince Hibernate bunu kendisi update ediyor
        c.setUpdatedAt(now);

        MessageResponse response = toMessageResponse(message, sender);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
        // Sohbet ekranı açık olmayan kullanıcılar için bildirim (okunmamış rozet vb.)
        notifyParticipants(c, "/queue/notifications", response);
        return response;
    }

    public void sendTyping(UserSummaryDto user, Long conversationId, boolean typing) {
        requireMembership(user.id(), conversationId);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing",
                new TypingEvent(conversationId, user, typing));
    }

    public boolean isParticipant(Long userId, Long conversationId) {
        return conversationRepository.findById(conversationId).map(c -> c.hasParticipant(userId)).orElse(false);
    }

    private Conversation requireMembership(Long userId, Long conversationId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Sohbet bulunamadı"));
        if (!c.hasParticipant(userId)) {
            throw ApiException.forbidden("Bu sohbetin üyesi değilsiniz");
        }
        return c;
    }

    private void notifyParticipants(Conversation c, String destination, Object payload) {
        // convertAndSendToUser Principal.getName() (email) ile eşleşir
        userQueryApi.findAllByIds(c.getParticipantIds())
                .forEach(u -> messagingTemplate.convertAndSendToUser(u.email(), destination, payload));
    }

    private ConversationResponse toResponse(Conversation c) {
        Map<Long, UserSummaryDto> users = loadUsers(c.getParticipantIds());
        MessageResponse last = messageRepository.findFirstByConversationIdOrderBySentAtDesc(c.getId())
                .map(m -> toMessageResponse(m, users.get(m.getSenderId())))
                .orElse(null);
        return new ConversationResponse(c.getId(), c.getName(), c.getType(),
                List.copyOf(users.values()), last, c.getUpdatedAt());
    }

    private Map<Long, UserSummaryDto> loadUsers(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userQueryApi.findAllByIds(ids).stream()
                .collect(Collectors.toMap(UserSummaryDto::id, Function.identity()));
    }

    private static MessageResponse toMessageResponse(ChatMessage m, UserSummaryDto sender) {
        return new MessageResponse(m.getId(), m.getConversationId(), sender, m.getContent(), m.getSentAt());
    }
}
