package edu.ai.lms.core.chat.adapter.in.websocket.config;

import edu.ai.lms.core.auth.adapter.out.security.JwtUtil;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String userEmail = jwtUtil.extractEmail(jwt);
                    String userRole = jwtUtil.extractRole(jwt);
                    
                    if (userEmail != null) {
                        UserDetails userDetails = loadUserByEmailAndRole(userEmail, userRole);
                        if (jwtUtil.validateToken(jwt, userDetails)) {
                            // Extract UUID from UserDetails if possible.
                            // In this system, UserAuth implements UserDetails. The Principal's name should be the UUID for @SendToUser mapping to work easily.
                            // Assuming UserAuth's getUsername returns email, we might need a custom Principal or just use the UserDetails.
                            // Wait, UserAuth implements UserDetails. So userDetails is UserAuth.
                            edu.ai.lms.core.auth.app.domain.UserAuth userAuth = (edu.ai.lms.core.auth.app.domain.UserAuth) userDetails;
                            
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userAuth.getId().toString(), // Use UUID string as principal name
                                    null,
                                    userDetails.getAuthorities()
                            );
                            accessor.setUser(authToken);
                            log.info("STOMP connection authenticated for user ID: {}", userAuth.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to authenticate STOMP connection", e);
                    throw new IllegalArgumentException("Invalid token");
                }
            } else {
                log.warn("Missing or invalid Authorization header in STOMP CONNECT");
                throw new IllegalArgumentException("Missing token");
            }
        }
        return message;
    }

    private UserDetails loadUserByEmailAndRole(String email, String role) {
        if ("TEACHER".equals(role)) {
            return loadTeacherPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Teacher not found: " + email));
        } else {
            return loadStudentPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Student not found: " + email));
        }
    }
}
