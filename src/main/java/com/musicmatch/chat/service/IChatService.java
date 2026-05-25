package com.musicmatch.chat.service;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;
import org.springframework.lang.NonNull;

import java.util.List;

public interface IChatService {
    ConversationResponse getOrCreateConversation(@NonNull Long otherUserId);
    List<ConversationResponse> getMyConversations();
    MessageResponse sendMessage(@NonNull Long conversationId, SendMessageRequest request);
    List<MessageResponse> getMessages(@NonNull Long conversationId);
}
