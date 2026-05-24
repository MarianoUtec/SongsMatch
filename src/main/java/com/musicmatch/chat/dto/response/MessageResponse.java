package com.musicmatch.chat.dto.response;
import java.time.LocalDateTime;
public record MessageResponse(
    Long id,
    Long conversationId,
    Long senderId,
    String senderName,
    String content,
    Boolean isRead,
    LocalDateTime sentAt
) {}
