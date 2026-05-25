package com.musicmatch.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import com.musicmatch.exceptions.ForbiddenException;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("ChatRestController Tests")
@SuppressWarnings("null")
class ChatRestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ChatService chatService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private ConversationResponse mockConversation;
    private MessageResponse mockMessage;

    @BeforeEach
    void setUp() {
        mockMessage = new MessageResponse(1L, 10L, 1L, "Alice",
            "Hello Bob!", false, LocalDateTime.now());
        mockConversation = new ConversationResponse(10L, 2L, "Bob",
            List.of(mockMessage), 0L, LocalDateTime.now());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithConversationWhenGetOrCreateConversation")
    void shouldReturn200WithConversationWhenGetOrCreateConversation() throws Exception {
        when(chatService.getOrCreateConversation(2L)).thenReturn(mockConversation);

        mockMvc.perform(post("/api/v1/conversations/with/2").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.otherUserName").value("Bob"))
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn404WhenGetOrCreateConversationWithNonExistentUser")
    void shouldReturn404WhenGetOrCreateConversationWithNonExistentUser() throws Exception {
        when(chatService.getOrCreateConversation(999L))
            .thenThrow(new ResourceNotFoundException("User", 999L));

        mockMvc.perform(post("/api/v1/conversations/with/999").with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithConversationListWhenGetMyConversations")
    void shouldReturn200WithConversationListWhenGetMyConversations() throws Exception {
        when(chatService.getMyConversations()).thenReturn(List.of(mockConversation));

        mockMvc.perform(get("/api/v1/conversations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].otherUserName").value("Bob"))
            .andExpect(jsonPath("$[0].messages[0].content").value("Hello Bob!"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithMessagesWhenGetMessagesForOwnConversation")
    void shouldReturn200WithMessagesWhenGetMessagesForOwnConversation() throws Exception {
        when(chatService.getMessages(10L)).thenReturn(List.of(mockMessage));

        mockMvc.perform(get("/api/v1/conversations/10/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Hello Bob!"))
            .andExpect(jsonPath("$[0].senderName").value("Alice"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn403WhenGetMessagesForForeignConversation")
    void shouldReturn403WhenGetMessagesForForeignConversation() throws Exception {
        when(chatService.getMessages(99L))
            .thenThrow(new ForbiddenException("You are not part of this conversation"));

        mockMvc.perform(get("/api/v1/conversations/99/messages"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn201WithMessageWhenSendMessageToOwnConversation")
    void shouldReturn201WithMessageWhenSendMessageToOwnConversation() throws Exception {
        SendMessageRequest request = new SendMessageRequest("Hello Bob!");
        when(chatService.sendMessage(eq(10L), any(SendMessageRequest.class)))
            .thenReturn(mockMessage);

        mockMvc.perform(post("/api/v1/conversations/10/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content").value("Hello Bob!"))
            .andExpect(jsonPath("$.senderId").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn400WhenSendMessageWithBlankContent")
    void shouldReturn400WhenSendMessageWithBlankContent() throws Exception {
        SendMessageRequest request = new SendMessageRequest("");

        mockMvc.perform(post("/api/v1/conversations/10/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn403WhenSendMessageToForeignConversation")
    void shouldReturn403WhenSendMessageToForeignConversation() throws Exception {
        SendMessageRequest request = new SendMessageRequest("Hacking!");
        when(chatService.sendMessage(eq(99L), any()))
            .thenThrow(new ForbiddenException("You are not part of this conversation"));

        mockMvc.perform(post("/api/v1/conversations/99/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldReturn401WhenAccessChatWithoutAuthentication")
    void shouldReturn401WhenAccessChatWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
            .andExpect(status().isUnauthorized());
    }
}
