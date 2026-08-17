package com.bloodconnect.conversation.repository;

import com.bloodconnect.common.enums.MessageStatus;
import com.bloodconnect.conversation.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    long countByConversationIdAndSenderIdNotAndStatusNot(
            Long conversationId,
            Long senderId,
            MessageStatus status
    );

    @Query("""
            select count(m) from ConversationMessage m
            join m.conversation c
            where m.sender.id <> :userId
              and m.status <> :read
              and (c.owner.id = :userId or c.donor.id = :userId)
            """)
    long countUnreadForUser(@Param("userId") Long userId, @Param("read") MessageStatus read);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationMessage m
            set m.status = :delivered, m.deliveredAt = :now
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and m.status = :sent
            """)
    int markIncomingDelivered(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("now") Instant now,
            @Param("sent") MessageStatus sent,
            @Param("delivered") MessageStatus delivered
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationMessage m
            set m.status = :read,
                m.readAt = :now,
                m.deliveredAt = coalesce(m.deliveredAt, :now)
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and m.status <> :read
            """)
    int markIncomingRead(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("now") Instant now,
            @Param("read") MessageStatus read
    );
}
