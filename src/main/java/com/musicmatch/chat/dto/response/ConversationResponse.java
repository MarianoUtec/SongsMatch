package com.musicmatch.chat.dto.response;
import java.time.LocalDateTime;
import java.util.List;
public record ConversationResponse(
    Long id,
    Long otherUserId,
    String otherUserName,
    List<MessageResponse> messages,
    Long unreadCount,
    LocalDateTime createdAt
) {}
