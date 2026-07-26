package edu.ai.lms.core.chat.adapter.in.websocket.config;

import edu.ai.lms.core.auth.api.AuthenticatedPrincipal;
import edu.ai.lms.core.auth.api.TokenAuthenticationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final TokenAuthenticationApi tokenAuthenticationApi;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    AuthenticatedPrincipal principal = tokenAuthenticationApi.authenticate(jwt);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal.userId().toString(),
                            null,
                            principal.authorities().stream().map(SimpleGrantedAuthority::new).toList()
                    );
                    accessor.setUser(authToken);
                    log.debug("STOMP connection authenticated for user ID: {}", principal.userId());
                } catch (Exception e) {
                    log.error("Failed to authenticate STOMP connection for token: {}", jwt, e);
                    throw new IllegalArgumentException("Invalid token: " + e.getMessage());
                }
            } else {
                log.warn("Missing or invalid Authorization header in STOMP CONNECT. Received: {}", authHeader);
                throw new IllegalArgumentException("Missing token");
            }
        }
        
        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            log.debug("STOMP SEND command detected for destination: {}", accessor.getDestination());
        }

        return message;
    }

}
