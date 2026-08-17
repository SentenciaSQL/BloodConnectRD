package com.bloodconnect.conversation.repository;

import com.bloodconnect.conversation.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    @Query("""
            select m from ConversationMessage m
            join fetch m.sender
            where m.conversation.id = :conversationId
            order by m.createdAt asc, m.id asc
            """)
    List<ConversationMessage> findThread(@Param("conversationId") Long conversationId);

    @Query("""
            select count(m) from ConversationMessage m
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and (:lastReadAt is null or m.createdAt > :lastReadAt)
            """)
    long countUnread(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("lastReadAt") Instant lastReadAt
    );
}
