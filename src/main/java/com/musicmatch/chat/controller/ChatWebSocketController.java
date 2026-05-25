package com.musicmatch.chat.controller;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.service.IChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final IChatService chatService;

    // WebSocket: Real-time message endpoint
    // Client sends to: /app/chat/{conversationId}
    @MessageMapping("/chat/{conversationId}")
    public void handleWebSocketMessage(
            @DestinationVariable @NonNull Long conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        if (principal == null) {
            log.error("Unauthorized attempt to send message via WebSocket to conversation {}", conversationId);
            throw new IllegalArgumentException("User must be authenticated");
        }
        chatService.sendMessage(conversationId, request);
    }
}
