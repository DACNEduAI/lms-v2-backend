package edu.ai.lms.core.auth.adapter.out.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SseJwtAuthenticationFilter sseJwtAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * UserDetailsService implementation that loads both Students and Teachers.
     * This is used by the JwtAuthenticationFilter to load user details during token validation.
     * It first tries to load a student, and if not found, tries to load a teacher.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            // Try to find student first
            UserDetails student = loadStudentPort.findByEmail(email).orElse(null);
            if (student != null) {
                return student;
            }

            // If not found as student, try to find as teacher
            return loadTeacherPort.findByEmail(email)
                    .map(teacher -> (UserDetails) teacher)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Authentication
                        .requestMatchers("/api/auth/**").permitAll()
                        // Service-to-service callback; authenticated by HMAC in the controller.
                        .requestMatchers("/api/internal/webhooks/quiz-generation").permitAll()
                        // Swagger UI endpoints
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // Allow Spring to show actual errors
                        .requestMatchers("/error").permitAll()
                        // allow to test the chat
                        .requestMatchers("/chat-test.html").permitAll()
                        // allow WebSocket handshakes (auth handled by STOMP interceptor)
                        .requestMatchers("/ws/**", "/wss/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                    .addFilterBefore(sseJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
