package com.musicmatch.chat.websocket;

import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.chat.repository.ConversationRepository;
import com.musicmatch.chat.domain.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients subscribe to /topic/... and /queue/...
        registry.enableSimpleBroker("/topic", "/queue");
        // Messages sent from clients are prefixed with /app
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        String token = null;
                        if (authHeader != null) {
                            if (authHeader.startsWith("Bearer ")) {
                                token = authHeader.substring(7);
                            } else {
                                token = authHeader;
                            }
                        } else {
                            token = accessor.getFirstNativeHeader("token");
                        }

                        if (token != null && !token.isBlank()) {
                            try {
                                String email = jwtService.extractUsername(token);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                if (jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                                    accessor.setUser(auth);
                                } else {
                                    throw new IllegalArgumentException("Invalid JWT token");
                                }
                            } catch (Exception e) {
                                log.error("WebSocket JWT auth failed: {}", e.getMessage());
                                throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                            }
                        } else {
                            throw new IllegalArgumentException("Missing or invalid Authorization/token header");
                        }
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        // Enforce authentication
                        Principal principal = accessor.getUser();
                        if (principal == null) {
                            throw new IllegalArgumentException("Unauthorized subscription attempt");
                        }
                        
                        String destination = accessor.getDestination();
                        if (destination != null) {
                            Long conversationId = extractConversationId(destination);
                            if (conversationId != null) {
                                // Extract email from Principal
                                String email = getEmailFromPrincipal(principal);
                                if (email == null) {
                                    throw new IllegalArgumentException("Could not resolve authenticated user");
                                }
                                
                                // Fetch conversation and check authorization
                                Conversation conversation = conversationRepository.findById(conversationId)
                                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
                                
                                boolean isParticipant = conversation.getUserOne().getEmail().equalsIgnoreCase(email)
                                    || conversation.getUserTwo().getEmail().equalsIgnoreCase(email);
                                
                                if (!isParticipant) {
                                    log.warn("User {} tried to subscribe to unauthorized conversation {}", email, conversationId);
                                    throw new IllegalArgumentException("You are not authorized to subscribe to this conversation");
                                }
                            }
                        }
                    } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        // Enforce authentication
                        Principal principal = accessor.getUser();
                        if (principal == null) {
                            throw new IllegalArgumentException("Unauthorized send attempt");
                        }
                        
                        String destination = accessor.getDestination();
                        if (destination != null) {
                            Long conversationId = extractConversationId(destination);
                            if (conversationId != null) {
                                String email = getEmailFromPrincipal(principal);
                                if (email == null) {
                                    throw new IllegalArgumentException("Could not resolve authenticated user");
                                }
                                
                                Conversation conversation = conversationRepository.findById(conversationId)
                                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
                                
                                boolean isParticipant = conversation.getUserOne().getEmail().equalsIgnoreCase(email)
                                    || conversation.getUserTwo().getEmail().equalsIgnoreCase(email);
                                
                                if (!isParticipant) {
                                    log.warn("User {} tried to send message to unauthorized conversation {}", email, conversationId);
                                    throw new IllegalArgumentException("You are not authorized to send messages to this conversation");
                                }
                            }
                        }
                    }
                }
                return message;
            }
        });
    }

    private Long extractConversationId(String destination) {
        if (destination == null) {
            return null;
        }
        // Match /topic/conversation.123 or /topic/conversations/123 or /app/chat/123
        Pattern pattern = Pattern.compile(".*/(?:conversation\\.|conversations/|chat/)(\\d+)");
        Matcher matcher = pattern.matcher(destination);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof UserDetails) {
                return ((UserDetails) principalObj).getUsername();
            }
            return principalObj.toString();
        }
        return principal.getName();
    }
}
