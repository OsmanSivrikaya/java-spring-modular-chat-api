package com.example.modularchatapi.modules.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Bir sohbete gönderilmiş tek bir mesaj.
 * <p>
 * Sohbete ve gönderene entity ilişkisiyle değil, sadece id ile bağlıdır.
 * Mesaj geçmişi hızlı gelsin diye (conversation_id, sent_at) üzerinde index var.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages", indexes = @Index(name = "idx_messages_conversation_sent", columnList = "conversation_id, sent_at"))
public class ChatMessage {

    /** Mesajın id'si, veritabanı otomatik artırır. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mesajın ait olduğu sohbetin id'si ({@link Conversation#getId()}). */
    @Column(nullable = false)
    private Long conversationId;

    /** Mesajı gönderen kullanıcının id'si. */
    @Column(nullable = false)
    private Long senderId;

    /** Mesaj metni, en fazla 4000 karakter. */
    @Column(nullable = false, length = 4000)
    private String content;

    /** Mesajın gönderildiği zaman (UTC). */
    @Column(nullable = false)
    private Instant sentAt;
}
