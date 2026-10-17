package com.example.modularchatapi.modules.chat.repository;

import com.example.modularchatapi.modules.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<ChatMessage, Long>: 1. tip hangi entity, 2. tip onun @Id alanının tipi.
// ChatMessage'ta id Long olduğu için Long yazıyorum. UUID olsaydı UUID yazardım.
// findById(Long), deleteById(Long) gibi hazır metotlar bu tipi kullanıyor.
// Class yazmıyorum, sadece interface. Spring uygulama açılırken implementasyonu kendisi üretiyor.
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Metot isminden sorgu üretiyor:
    // findBy                  -> SELECT ... WHERE
    // ConversationId          -> conversation_id = ?  (entity'deki alan adı)
    // OrderBySentAtDesc       -> ORDER BY sent_at DESC
    // Pageable verince LIMIT/OFFSET'i, Page dönünce toplam sayıyı (COUNT) da kendisi ekliyor.
    Page<ChatMessage> findByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);

    // findFirst -> sadece ilk kaydı al (LIMIT 1). Sohbetin son mesajı için kullanıyorum.
    // Optional: kayıt yoksa null yerine boş dönüyor. C#'taki FirstOrDefault gibi.
    Optional<ChatMessage> findFirstByConversationIdOrderBySentAtDesc(Long conversationId);
}
