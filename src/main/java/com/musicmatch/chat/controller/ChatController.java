package com.musicmatch.chat.controller;

import com.musicmatch.chat.service.IChatService;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import com.musicmatch.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    // REST: Get or create conversation with a user
    @PostMapping("/conversations/with/{userId}")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(chatService.getOrCreateConversation(userId));
    }

    // REST: Get all my conversations
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getMyConversations() {
        return ResponseEntity.ok(chatService.getMyConversations());
    }

    // REST: Get messages for a conversation
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getMessages(conversationId));
    }

    // REST: Send message via HTTP (also broadcasts via WebSocket)
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.sendMessage(conversationId, request));
    }

    // WebSocket: Real-time message endpoint
    // Client sends to: /app/chat/{conversationId}
    // Server broadcasts to: /topic/conversation.{conversationId}
    @MessageMapping("/chat/{conversationId}")
    public void handleWebSocketMessage(
            @DestinationVariable Long conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        // Delegate to the same service — handles broadcast internally
        chatService.sendMessage(conversationId, request);
    }
}
