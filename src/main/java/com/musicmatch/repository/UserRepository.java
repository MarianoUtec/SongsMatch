package com.musicmatch.repository;

import com.musicmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.id != :userId AND u.isActive = true")
    List<User> findAllActiveExcept(Long userId);
}
