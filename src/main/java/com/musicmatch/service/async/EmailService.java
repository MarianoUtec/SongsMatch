package com.musicmatch.service.async;

import com.musicmatch.entity.Song;
import com.musicmatch.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.from}")
    private String from;

    @Async("taskExecutor")
    public void sendWelcomeEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("email", user.getEmail());
            String html = templateEngine.process("emails/welcome", context);
            sendHtmlEmail(user.getEmail(), "Welcome to MusicMatch 🎵", html);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // FIX: método completo con todos los parámetros que necesita el template
    @Async("taskExecutor")
    public void sendRecommendationReadyEmail(User user, List<Song> songs,
                                              User matchedUser, Double compatibilityScore) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("songs", songs);
            context.setVariable("matchedUserName",
                matchedUser != null ? matchedUser.getName() : "a similar listener");
            context.setVariable("compatibilityScore",
                compatibilityScore != null ? Math.round(compatibilityScore) : 0);
            String html = templateEngine.process("emails/recommendation-ready", context);
            sendHtmlEmail(user.getEmail(), "Your music recommendations are ready 🎶", html);
        } catch (Exception e) {
            log.error("Failed to send recommendation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Email sent to: {}", to);
    }
}
