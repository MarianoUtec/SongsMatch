package com.musicmatch.event;

import com.musicmatch.service.async.EmailService;
import com.musicmatch.service.async.SvdComputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class MusicMatchEventListener {

    private final EmailService emailService;
    private final SvdComputationService svdComputationService;

    @EventListener
    @Async("taskExecutor")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome email to: {}", event.getUser().getEmail());
        emailService.sendWelcomeEmail(event.getUser());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handleRatingSubmitted(RatingSubmittedEvent event) {
        log.info("Rating submitted by user {}. Triggering SVD recomputation.", event.getUserId());
        svdComputationService.recomputeForAllUsers();
    }
}
