package com.bloodconnect.conversation.repository;

import com.bloodconnect.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            select c from Conversation c
            join fetch c.bloodRequest
            join fetch c.owner
            join fetch c.donor
            where c.bloodRequest.id = :bloodRequestId and c.donor.id = :donorUserId
            """)
    Optional<Conversation> findByBloodRequestIdAndDonorId(
            @Param("bloodRequestId") Long bloodRequestId,
            @Param("donorUserId") Long donorUserId
    );

    @Query("""
            select c from Conversation c
            join fetch c.bloodRequest
            join fetch c.owner
            join fetch c.donor
            where c.id = :id
            """)
    Optional<Conversation> findDetailedById(@Param("id") Long id);

    @Query("""
            select c from Conversation c
            join fetch c.bloodRequest
            join fetch c.owner
            join fetch c.donor
            where c.owner.id = :userId or c.donor.id = :userId
            order by coalesce(c.lastMessageAt, c.createdAt) desc
            """)
    List<Conversation> findInbox(@Param("userId") Long userId);
}
