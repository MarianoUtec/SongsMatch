package com.musicmatch.chat.service;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import com.musicmatch.chat.domain.Conversation;
import com.musicmatch.chat.domain.Message;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ForbiddenException;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.chat.repository.ConversationRepository;
import com.musicmatch.chat.repository.MessageRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.auth.service.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private ChatService chatService;

    private User alice;
    private User bob;
    private Conversation conversation;
    private Message message;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).name("Alice").email("alice@test.com")
            .role(Role.USER).isActive(true).build();
        bob = User.builder().id(2L).name("Bob").email("bob@test.com")
            .role(Role.USER).isActive(true).build();

        lenient().when(securityHelper.getCurrentUser()).thenReturn(alice);

        conversation = Conversation.builder()
            .id(10L).userOne(alice).userTwo(bob)
            .createdAt(LocalDateTime.now()).build();

        message = Message.builder()
            .id(100L).content("Hello Bob!").conversation(conversation)
            .sender(alice).isRead(false).sentAt(LocalDateTime.now()).build();
    }

    // ─────────────────────── getOrCreateConversation ─────────────────────

    @Test
    @DisplayName("shouldReturnExistingConversationWhenConversationAlreadyExists")
    void shouldReturnExistingConversationWhenConversationAlreadyExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(conversationRepository.findBetweenUsers(1L, 2L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L)).thenReturn(List.of());
        when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(10L, 1L)).thenReturn(0L);

        ConversationResponse response = chatService.getOrCreateConversation(2L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.otherUserName()).isEqualTo("Bob");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateNewConversationWhenNoneExistsBetweenUsers")
    void shouldCreateNewConversationWhenNoneExistsBetweenUsers() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(conversationRepository.findBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L)).thenReturn(List.of());
        when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(10L, 1L)).thenReturn(0L);

        ConversationResponse response = chatService.getOrCreateConversation(2L);

        assertThat(response.id()).isEqualTo(10L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenGetOrCreateConversationWithInvalidUser")
    void shouldThrowResourceNotFoundExceptionWhenGetOrCreateConversationWithInvalidUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getOrCreateConversation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────── getMyConversations ───────────────────────

    @Test
    @DisplayName("shouldReturnListOfConversationsWhenGetMyConversationsWithExistingConversations")
    void shouldReturnListOfConversationsWhenGetMyConversationsWithExistingConversations() {
        when(conversationRepository.findAllByUserId(1L)).thenReturn(List.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L)).thenReturn(List.of());
        when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(10L, 1L)).thenReturn(0L);

        List<ConversationResponse> responses = chatService.getMyConversations();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).otherUserName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenGetMyConversationsWithNoConversations")
    void shouldReturnEmptyListWhenGetMyConversationsWithNoConversations() {
        when(conversationRepository.findAllByUserId(1L)).thenReturn(List.of());

        List<ConversationResponse> responses = chatService.getMyConversations();

        assertThat(responses).isEmpty();
    }

    // ─────────────────────────── sendMessage ─────────────────────────────

    @Test
    @DisplayName("shouldSendMessageAndBroadcastWhenSenderBelongsToConversation")
    void shouldSendMessageAndBroadcastWhenSenderBelongsToConversation() {
        SendMessageRequest request = new SendMessageRequest("Hello Bob!");

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse response = chatService.sendMessage(10L, request);

        assertThat(response.content()).isEqualTo("Hello Bob!");
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.senderName()).isEqualTo("Alice");
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation.10"), any(MessageResponse.class));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/messages"), any(MessageResponse.class));
    }

    @Test
    @DisplayName("shouldThrowForbiddenExceptionWhenSendMessageAndUserNotInConversation")
    void shouldThrowForbiddenExceptionWhenSendMessageAndUserNotInConversation() {
        User charlie = User.builder().id(3L).name("Charlie").email("charlie@test.com")
            .role(Role.USER).isActive(true).build();
        Conversation foreignConversation = Conversation.builder()
            .id(10L).userOne(bob).userTwo(charlie)
            .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(foreignConversation));

        assertThatThrownBy(() -> chatService.sendMessage(10L, new SendMessageRequest("Hi")))
            .isInstanceOf(ForbiddenException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenSendMessageToNonExistentConversation")
    void shouldThrowResourceNotFoundExceptionWhenSendMessageToNonExistentConversation() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(999L, new SendMessageRequest("Hi")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────── getMessages ─────────────────────────────

    @Test
    @DisplayName("shouldReturnMessagesAndMarkAsReadWhenGetMessagesWithAuthorizedUser")
    void shouldReturnMessagesAndMarkAsReadWhenGetMessagesWithAuthorizedUser() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L)).thenReturn(List.of(message));

        List<MessageResponse> responses = chatService.getMessages(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("Hello Bob!");
        verify(messageRepository).markAllAsRead(10L, 1L);
    }

    @Test
    @DisplayName("shouldThrowForbiddenExceptionWhenGetMessagesAndUserNotInConversation")
    void shouldThrowForbiddenExceptionWhenGetMessagesAndUserNotInConversation() {
        User charlie = User.builder().id(3L).name("Charlie").email("charlie@test.com")
            .role(Role.USER).isActive(true).build();
        Conversation foreignConversation = Conversation.builder()
            .id(10L).userOne(bob).userTwo(charlie).createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(foreignConversation));

        assertThatThrownBy(() -> chatService.getMessages(10L))
            .isInstanceOf(ForbiddenException.class);

        verify(messageRepository, never()).markAllAsRead(anyLong(), anyLong());
    }
}
