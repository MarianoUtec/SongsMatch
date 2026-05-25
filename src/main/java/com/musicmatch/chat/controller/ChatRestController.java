package com.musicmatch.chat.controller;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import com.musicmatch.chat.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatRestController {

    private final IChatService chatService;

    // REST: Get or create conversation with a user
    @PostMapping("/with/{userId}")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(chatService.getOrCreateConversation(userId));
    }

    // REST: Get all my conversations
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getMyConversations() {
        return ResponseEntity.ok(chatService.getMyConversations());
    }

    // REST: Get messages for a conversation
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable("id") Long conversationId) {
        return ResponseEntity.ok(chatService.getMessages(conversationId));
    }

    // REST: Send message via HTTP (also broadcasts via WebSocket)
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable("id") Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.sendMessage(conversationId, request));
    }
}
