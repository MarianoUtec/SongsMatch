package com.musicmatch.chat.service;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import com.musicmatch.chat.domain.Conversation;
import com.musicmatch.chat.domain.Message;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ForbiddenException;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.chat.repository.ConversationRepository;
import com.musicmatch.chat.repository.MessageRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService implements IChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(@NonNull Long otherUserId) {
        User me = securityHelper.getCurrentUser();
        Long myId = Objects.requireNonNull(me.getId());
        User other = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", otherUserId));

        Conversation conversation = Objects.requireNonNull(conversationRepository
            .findBetweenUsers(myId, otherUserId)
            .orElseGet(() -> conversationRepository.save(
                Objects.requireNonNull(Conversation.builder().userOne(me).userTwo(other).build())
            )));

        return toResponse(conversation, myId);
    }

    @Override
    public List<ConversationResponse> getMyConversations() {
        User me = securityHelper.getCurrentUser();
        Long myId = Objects.requireNonNull(me.getId());
        return conversationRepository.findAllByUserId(myId)
            .stream().map(c -> toResponse(Objects.requireNonNull(c), myId)).toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(@NonNull Long conversationId, SendMessageRequest request) {
        User me = securityHelper.getCurrentUser();
        Long myId = Objects.requireNonNull(me.getId());
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateUserInConversation(Objects.requireNonNull(conversation), myId);

        Message draftMessage = Objects.requireNonNull(Message.builder()
            .content(request.content())
            .conversation(conversation)
            .sender(me)
            .build());
        Message message = Objects.requireNonNull(messageRepository.save(draftMessage));

        MessageResponse response = Objects.requireNonNull(toMessageResponse(message));
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, Objects.requireNonNull(response));
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Objects.requireNonNull(response));

        Long recipientId = conversation.getUserOne().getId().equals(myId)
            ? conversation.getUserTwo().getId()
            : conversation.getUserOne().getId();
        String destination = Objects.requireNonNull(recipientId).toString();
        messagingTemplate.convertAndSendToUser(Objects.requireNonNull(destination), "/queue/messages", Objects.requireNonNull(response));

        log.info("Message sent in conversation {} by user {}", conversationId, myId);
        return response;
    }

    @Override
    @Transactional
    public List<MessageResponse> getMessages(@NonNull Long conversationId) {
        User me = securityHelper.getCurrentUser();
        Long myId = Objects.requireNonNull(me.getId());
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateUserInConversation(Objects.requireNonNull(conversation), myId);
        messageRepository.markAllAsRead(conversationId, myId);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
            .stream().map(this::toMessageResponse).toList();
    }

    private void validateUserInConversation(@NonNull Conversation conversation, @NonNull Long userId) {
        boolean belongs = conversation.getUserOne().getId().equals(userId)
            || conversation.getUserTwo().getId().equals(userId);
        if (!belongs) throw new ForbiddenException("You are not part of this conversation");
    }

    private ConversationResponse toResponse(@NonNull Conversation c, @NonNull Long myId) {
        User other = c.getUserOne().getId().equals(myId) ? c.getUserTwo() : c.getUserOne();
        List<MessageResponse> messages = messageRepository
            .findByConversationIdOrderBySentAtAsc(c.getId())
            .stream().map(this::toMessageResponse).toList();
        long unread = messageRepository
            .countByConversationIdAndIsReadFalseAndSenderIdNot(c.getId(), myId);
        return new ConversationResponse(
            c.getId(), other.getId(), other.getName(), messages, unread, c.getCreatedAt()
        );
    }

    private MessageResponse toMessageResponse(@NonNull Message m) {
        return new MessageResponse(
            m.getId(), m.getConversation().getId(),
            m.getSender().getId(), m.getSender().getName(),
            m.getContent(), m.getIsRead(), m.getSentAt()
        );
    }
}
