package com.musicmatch.service.interfaces;

import com.musicmatch.dto.request.SendMessageRequest;
import com.musicmatch.dto.response.ConversationResponse;
import com.musicmatch.dto.response.MessageResponse;

import java.util.List;

public interface IChatService {
    ConversationResponse getOrCreateConversation(Long otherUserId);
    List<ConversationResponse> getMyConversations();
    MessageResponse sendMessage(Long conversationId, SendMessageRequest request);
    List<MessageResponse> getMessages(Long conversationId);
}
