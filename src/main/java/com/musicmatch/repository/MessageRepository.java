package com.musicmatch.repository;

import com.musicmatch.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long senderId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE " +
           "m.conversation.id = :conversationId AND m.sender.id != :userId")
    void markAllAsRead(Long conversationId, Long userId);
}
