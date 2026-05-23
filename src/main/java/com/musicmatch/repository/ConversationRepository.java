package com.musicmatch.repository;

import com.musicmatch.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.userOne.id = :userOneId AND c.userTwo.id = :userTwoId) OR " +
           "(c.userOne.id = :userTwoId AND c.userTwo.id = :userOneId)")
    Optional<Conversation> findBetweenUsers(Long userOneId, Long userTwoId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "c.userOne.id = :userId OR c.userTwo.id = :userId " +
           "ORDER BY c.createdAt DESC")
    List<Conversation> findAllByUserId(Long userId);
}
