package com.example.modularchatapi.modules.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Bir sohbeti temsil eder. Birebir ({@link ConversationType#DIRECT}) ya da grup
 * ({@link ConversationType#GROUP}) olabilir.
 * <p>
 * Modüler monolit kuralı: Chat modülü User entity'sine ilişki (FK navigation) kurmaz,
 * sadece kullanıcı id'lerini tutar. Kullanıcı bilgisi auth modülünün UserQueryApi'sinden gelir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation {

    /** Sohbetin id'si, veritabanı otomatik artırır. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Grup adı. Grup sohbetlerinde dolu, birebir sohbetlerde {@code null}. */
    private String name;

    /** Sohbetin tipi. Veritabanına sayı değil isim olarak ("DIRECT", "GROUP") yazılır. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    /** Sohbeti oluşturan kullanıcının id'si. */
    @Column(nullable = false)
    private Long createdBy;

    /**
     * Sohbetteki kullanıcıların id'leri.
     * Ayrı bir {@code conversation_participants} tablosunda tutulur (conversation_id, user_id).
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "user_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();

    /** Sohbetin oluşturulma zamanı (UTC). */
    @Column(nullable = false)
    private Instant createdAt;

    /** Son aktivite zamanı. Her yeni mesajda güncellenir, sohbet listesi buna göre sıralanır. */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Kullanıcının bu sohbetin üyesi olup olmadığını kontrol eder.
     *
     * @param userId kontrol edilecek kullanıcının id'si
     * @return üyeyse {@code true}
     */
    public boolean hasParticipant(Long userId) {
        return participantIds.contains(userId);
    }
}
