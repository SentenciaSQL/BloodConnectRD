package com.bloodconnect.conversation.service;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.enums.MessageStatus;
import com.bloodconnect.common.enums.NotificationType;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.conversation.dto.ChatMessageDto;
import com.bloodconnect.conversation.dto.ConversationDto;
import com.bloodconnect.conversation.dto.OpenConversationRequest;
import com.bloodconnect.conversation.dto.SendMessageRequest;
import com.bloodconnect.conversation.dto.UnreadCountResponse;
import com.bloodconnect.conversation.entity.Conversation;
import com.bloodconnect.conversation.entity.ConversationMessage;
import com.bloodconnect.conversation.repository.ConversationMessageRepository;
import com.bloodconnect.conversation.repository.ConversationRepository;
import com.bloodconnect.donationresponse.repository.DonationResponseRepository;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.repository.DonorRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.notification.service.NotificationService;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final BloodRequestService bloodRequestService;
    private final DonationResponseRepository responseRepository;
    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ConversationDto openOrGet(
            Long bloodRequestId,
            UserPrincipal principal,
            OpenConversationRequest request
    ) {
        BloodRequest bloodRequest = bloodRequestService.findEntity(bloodRequestId);
        User owner = bloodRequest.getCreatedBy();
        Long donorUserId = resolveDonorUserId(owner, principal, request);
        if (donorUserId.equals(owner.getId())) {
            throw new BadRequestException("No puede abrir un chat consigo mismo");
        }
        Donor donor = donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el donante"));
        if (responseRepository.findByBloodRequestIdAndDonorIdOrderByCreatedAtDesc(
                bloodRequestId,
                donor.getId()
        ).isEmpty()) {
            throw new BadRequestException("El donante no ha ofrecido ayudar con esta solicitud");
        }
        Conversation conversation = conversationRepository
                .findByBloodRequestIdAndDonorId(bloodRequestId, donorUserId)
                .orElseGet(() -> createConversation(bloodRequest, owner, donor.getUser()));
        return toDto(conversation, principal.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> list(UserPrincipal principal) {
        return conversationRepository.findInbox(principal.getId()).stream()
                .map(conversation -> toDto(conversation, principal.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDto get(Long id, UserPrincipal principal) {
        return toDto(requireParticipant(id, principal), principal.getId());
    }

    @Transactional
    public List<ChatMessageDto> messages(Long id, UserPrincipal principal) {
        requireParticipant(id, principal);
        messageRepository.markIncomingDelivered(
                id,
                principal.getId(),
                Instant.now(),
                MessageStatus.SENT,
                MessageStatus.DELIVERED
        );
        return messageRepository.findThread(id).stream()
                .map(message -> toMessageDto(message, principal.getId()))
                .toList();
    }

    @Transactional
    public ChatMessageDto send(Long id, UserPrincipal principal, SendMessageRequest request) {
        Conversation conversation = requireParticipant(id, principal);
        String body = request.body().trim();
        User sender = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario"));
        ConversationMessage message = messageRepository.save(ConversationMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .body(body)
                .status(MessageStatus.SENT)
                .build());
        conversation.setLastMessageBody(body);
        conversation.setLastMessageAt(message.getCreatedAt());
        if (conversation.getOwner().getId().equals(sender.getId())) {
            conversation.setOwnerLastReadAt(message.getCreatedAt());
        } else if (conversation.getDonor().getId().equals(sender.getId())) {
            conversation.setDonorLastReadAt(message.getCreatedAt());
        }
        conversationRepository.save(conversation);
        User recipient = conversation.getOwner().getId().equals(sender.getId())
                ? conversation.getDonor()
                : conversation.getOwner();
        String senderName = fullName(sender);
        notificationService.create(
                recipient,
                NotificationType.CONVERSATION_MESSAGE,
                senderName + " te envió un mensaje",
                senderName + " te envió un mensaje sobre tu solicitud de sangre.",
                "CONVERSATION",
                conversation.getId()
        );
        return toMessageDto(message, principal.getId());
    }

    @Transactional
    public ConversationDto markRead(Long id, UserPrincipal principal) {
        Conversation conversation = requireParticipant(id, principal);
        Instant now = Instant.now();
        messageRepository.markIncomingRead(id, principal.getId(), now, MessageStatus.READ);
        if (conversation.getOwner().getId().equals(principal.getId())) {
            conversation.setOwnerLastReadAt(now);
        } else if (conversation.getDonor().getId().equals(principal.getId())) {
            conversation.setDonorLastReadAt(now);
        }
        return toDto(conversationRepository.save(conversation), principal.getId());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UserPrincipal principal) {
        return new UnreadCountResponse(
                messageRepository.countUnreadForUser(principal.getId(), MessageStatus.READ)
        );
    }

    private Conversation createConversation(BloodRequest bloodRequest, User owner, User donor) {
        Conversation conversation = Conversation.builder()
                .bloodRequest(bloodRequest)
                .owner(owner)
                .donor(donor)
                .build();
        try {
            return conversationRepository.saveAndFlush(conversation);
        } catch (DataIntegrityViolationException exception) {
            return conversationRepository
                    .findByBloodRequestIdAndDonorId(bloodRequest.getId(), donor.getId())
                    .orElseThrow(() -> exception);
        }
    }

    private Long resolveDonorUserId(
            User owner,
            UserPrincipal principal,
            OpenConversationRequest request
    ) {
        boolean ownerOrAdmin = principal.getRole() == Role.ADMIN
                || owner.getId().equals(principal.getId());
        if (ownerOrAdmin) {
            if (request == null || request.donorUserId() == null) {
                throw new BadRequestException("Debe indicar el donante a contactar");
            }
            return request.donorUserId();
        }
        if (request != null
                && request.donorUserId() != null
                && !request.donorUserId().equals(principal.getId())) {
            throw new AccessDeniedException("Solo puede abrir su propia conversación");
        }
        return principal.getId();
    }

    private Conversation requireParticipant(Long id, UserPrincipal principal) {
        Conversation conversation = conversationRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la conversación"));
        boolean participant = conversation.getOwner().getId().equals(principal.getId())
                || conversation.getDonor().getId().equals(principal.getId());
        if (!participant && principal.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("No puede acceder a conversaciones de otros usuarios");
        }
        return conversation;
    }

    private ConversationDto toDto(Conversation conversation, Long currentUserId) {
        User other = conversation.getOwner().getId().equals(currentUserId)
                ? conversation.getDonor()
                : conversation.getOwner();
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndStatusNot(
                conversation.getId(),
                currentUserId,
                MessageStatus.READ
        );
        BloodRequest request = conversation.getBloodRequest();
        return new ConversationDto(
                conversation.getId(),
                request.getId(),
                request.getPatientName(),
                request.getHospital(),
                request.getBloodType(),
                conversation.getOwner().getId(),
                conversation.getDonor().getId(),
                other.getId(),
                fullName(other),
                conversation.getLastMessageBody(),
                conversation.getLastMessageAt(),
                unread,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private ChatMessageDto toMessageDto(ConversationMessage message, Long currentUserId) {
        User sender = message.getSender();
        return new ChatMessageDto(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                fullName(sender),
                message.getBody(),
                sender.getId().equals(currentUserId),
                message.getStatus() == null ? MessageStatus.SENT : message.getStatus(),
                message.getDeliveredAt(),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
