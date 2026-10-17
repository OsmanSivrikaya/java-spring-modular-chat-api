package com.example.modularchatapi.modules.chat.repository;

import com.example.modularchatapi.modules.chat.domain.Conversation;
import com.example.modularchatapi.modules.chat.domain.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Conversation entity'si, id tipi Long
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Metot ismiyle anlatması zor olunca @Query ile kendim yazıyorum.
    // Bu SQL değil JPQL: tablo değil entity ve alan adı kullanıyorum (Conversation, participantIds).
    // LINQ'te from c in Conversations where ... yazmak gibi düşünüyorum.
    // :userId parametresi @Param("userId") ile eşleşiyor.
    @Query("select distinct c from Conversation c join c.participantIds p where p = :userId order by c.updatedAt desc")
    List<Conversation> findAllByParticipant(@Param("userId") Long userId);

    // İki kişi arasında zaten birebir sohbet varsa yenisini açmamak için.
    // "member of" -> listede var mı (LINQ'teki Contains)
    @Query("""
            select c from Conversation c
            where c.type = :type
              and :a member of c.participantIds
              and :b member of c.participantIds
            """)
    Optional<Conversation> findDirectBetween(@Param("type") ConversationType type,
                                             @Param("a") Long userA,
                                             @Param("b") Long userB);
}
