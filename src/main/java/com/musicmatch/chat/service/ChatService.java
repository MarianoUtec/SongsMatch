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
import com.musicmatch.chat.service.IChatService;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public ConversationResponse getOrCreateConversation(Long otherUserId) {
        User me = securityHelper.getCurrentUser();
        User other = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", otherUserId));

        Conversation conversation = conversationRepository
            .findBetweenUsers(me.getId(), otherUserId)
            .orElseGet(() -> conversationRepository.save(
                Conversation.builder().userOne(me).userTwo(other).build()
            ));

        return toResponse(conversation, me.getId());
    }

    @Override
    public List<ConversationResponse> getMyConversations() {
        User me = securityHelper.getCurrentUser();
        return conversationRepository.findAllByUserId(me.getId())
            .stream().map(c -> toResponse(c, me.getId())).toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request) {
        User me = securityHelper.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateUserInConversation(conversation, me.getId());

        Message message = messageRepository.save(
            Message.builder()
                .content(request.content())
                .conversation(conversation)
                .sender(me)
                .build()
        );

        MessageResponse response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, response);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        Long recipientId = conversation.getUserOne().getId().equals(me.getId())
            ? conversation.getUserTwo().getId()
            : conversation.getUserOne().getId();
        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/messages", response);

        log.info("Message sent in conversation {} by user {}", conversationId, me.getId());
        return response;
    }

    @Override
    @Transactional
    public List<MessageResponse> getMessages(Long conversationId) {
        User me = securityHelper.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateUserInConversation(conversation, me.getId());
        messageRepository.markAllAsRead(conversationId, me.getId());

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
            .stream().map(this::toMessageResponse).toList();
    }

    private void validateUserInConversation(Conversation conversation, Long userId) {
        boolean belongs = conversation.getUserOne().getId().equals(userId)
            || conversation.getUserTwo().getId().equals(userId);
        if (!belongs) throw new ForbiddenException("You are not part of this conversation");
    }

    private ConversationResponse toResponse(Conversation c, Long myId) {
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

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
            m.getId(), m.getConversation().getId(),
            m.getSender().getId(), m.getSender().getName(),
            m.getContent(), m.getIsRead(), m.getSentAt()
        );
    }
}
