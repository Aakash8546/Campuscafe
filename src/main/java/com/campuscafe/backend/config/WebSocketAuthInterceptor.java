package com.campuscafe.backend.config;

import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP frames on the inbound channel to enforce JWT authentication.
 *
 * <p>On CONNECT: validates the JWT from the "Authorization" header (Bearer ...).
 * Rejects the connection if the token is missing, invalid, expired, or not an access token.
 *
 * <p>On SUBSCRIBE: validates that the authenticated user's merchantId matches the
 * merchantId encoded in the topic destination, preventing cross-merchant data leakage.
 * Public customer-display topics are exempt from this check.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            return handleConnect(message, accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return handleSubscribe(message, accessor);
        }

        return message;
    }

    // ---- CONNECT: validate JWT and set auth principal ----

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected: missing or malformed Authorization header.");
            throw new org.springframework.security.access.AccessDeniedException(
                    "WebSocket connection requires a valid Bearer token.");
        }

        String jwt = authHeader.substring(7);
        try {
            String tokenType = jwtService.extractTokenType(jwt);
            if (!"access".equals(tokenType)) {
                log.warn("WebSocket CONNECT rejected: token type '{}' is not 'access'.", tokenType);
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only access tokens are allowed for WebSocket connections.");
            }

            String email = jwtService.extractUsername(jwt);
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

            if (!jwtService.validateToken(jwt, userDetails)) {
                log.warn("WebSocket CONNECT rejected: JWT validation failed for user '{}'.", email);
                throw new org.springframework.security.access.AccessDeniedException("Invalid or expired JWT token.");
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
            log.debug("WebSocket CONNECT authenticated: user='{}', merchantId={}",
                    userDetails.getEmail(), userDetails.getMerchantId());

        } catch (org.springframework.security.access.AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("WebSocket CONNECT rejected: JWT parsing error — {}", ex.getMessage());
            throw new org.springframework.security.access.AccessDeniedException("Invalid JWT token.");
        }

        return message;
    }

    // ---- SUBSCRIBE: enforce merchant-topic isolation ----

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        // Public customer-display topics are intentionally open (no auth needed on subscribe).
        // Pattern: /topic/merchant/{id}/customer-display
        if (destination.matches("/topic/merchant/\\d+/customer-display")) {
            return message;
        }

        // All other /topic/merchant/{id}/** topics require the authenticated user to own that merchantId.
        if (destination.startsWith("/topic/merchant/")) {
            Long topicMerchantId = extractMerchantIdFromDestination(destination);
            if (topicMerchantId == null) {
                log.warn("WebSocket SUBSCRIBE rejected: could not parse merchantId from destination '{}'.", destination);
                throw new org.springframework.security.access.AccessDeniedException(
                        "Invalid subscription destination.");
            }

            UsernamePasswordAuthenticationToken auth = getAuthFromAccessor(accessor);
            if (auth == null) {
                log.warn("WebSocket SUBSCRIBE rejected: no authenticated user for destination '{}'.", destination);
                throw new org.springframework.security.access.AccessDeniedException(
                        "Authentication required to subscribe.");
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!topicMerchantId.equals(userDetails.getMerchantId())) {
                log.warn("WebSocket SUBSCRIBE rejected: user merchantId={} attempted to subscribe to merchantId={} topic '{}'.",
                        userDetails.getMerchantId(), topicMerchantId, destination);
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to subscribe to this topic.");
            }

            log.debug("WebSocket SUBSCRIBE granted: user='{}' -> '{}'", userDetails.getEmail(), destination);
        }

        return message;
    }

    // ---- Helpers ----

    private Long extractMerchantIdFromDestination(String destination) {
        try {
            // Expected format: /topic/merchant/{merchantId}/...
            String[] parts = destination.split("/");
            // parts[0]="" parts[1]="topic" parts[2]="merchant" parts[3]="{id}" parts[4]=...
            if (parts.length >= 4) {
                return Long.parseLong(parts[3]);
            }
        } catch (NumberFormatException ignored) {
            // handled by null return
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken getAuthFromAccessor(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            return auth;
        }
        // Fallback: check Spring Security context (populated during CONNECT)
        var contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth instanceof UsernamePasswordAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
