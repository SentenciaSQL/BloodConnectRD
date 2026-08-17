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

    long countByConversationIdAndSenderIdNot(Long conversationId, Long senderId);

    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(
            Long conversationId,
            Long senderId,
            Instant createdAt
    );
}
