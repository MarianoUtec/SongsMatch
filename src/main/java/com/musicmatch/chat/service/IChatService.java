package com.musicmatch.chat.service;

import com.musicmatch.chat.dto.request.SendMessageRequest;
import com.musicmatch.chat.dto.response.ConversationResponse;
import com.musicmatch.chat.dto.response.MessageResponse;

import java.util.List;

public interface IChatService {
    ConversationResponse getOrCreateConversation(Long otherUserId);
    List<ConversationResponse> getMyConversations();
    MessageResponse sendMessage(Long conversationId, SendMessageRequest request);
    List<MessageResponse> getMessages(Long conversationId);
}
