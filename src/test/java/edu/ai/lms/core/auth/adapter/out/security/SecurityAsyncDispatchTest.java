package edu.ai.lms.core.auth.adapter.out.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.auth.api.TokenAuthenticationApi;

@WebMvcTest(controllers = SecurityAsyncDispatchTest.AsyncController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SseJwtAuthenticationFilter.class,
        SecurityErrorResponseWriter.class,
        SecurityAsyncDispatchTest.AsyncController.class,
        SecurityAsyncDispatchTest.TestBeans.class
})
public class SecurityAsyncDispatchTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void initialRequestStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/test/security/async"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void invalidBearerTokenReturnsUnauthorizedInsteadOfContinuingAnonymously() throws Exception {
        mockMvc.perform(get("/test/security/async")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void authenticatedWrongRoleReturnsForbidden() throws Exception {
        mockMvc.perform(get("/test/security/teacher")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void authenticatedAsyncRequestCanCompleteItsRedispatch() throws Exception {
        MvcResult initial = mockMvc.perform(get("/test/security/async")
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initial))
                .andExpect(status().isOk())
                .andExpect(content().string("complete"));
    }

    @RestController
    public static class AsyncController {
        @GetMapping("/test/security/async")
        Callable<String> completeAsync() {
            return () -> "complete";
        }

        @PreAuthorize("hasRole('TEACHER')")
        @GetMapping("/test/security/teacher")
        String teacherOnly() {
            return "teacher";
        }
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        TokenAuthenticationApi tokenAuthenticationApi() {
            return token -> {
                throw new IllegalArgumentException("Token authentication is not used by this test");
            };
        }
    }
}
