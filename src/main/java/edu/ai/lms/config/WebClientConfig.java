package edu.ai.lms.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${quiz.generation.ai.token:}")
    private String aiServiceToken;

    @Value("${ai.service.response-timeout-seconds:900}")
    private long responseTimeoutSeconds;

    @Bean
    public WebClient aiWebClient(WebClient.Builder webClientBuilder) {
        // Cấu hình HttpClient với Netty để thiết lập Timeout chuyên sâu
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS)));

        WebClient.Builder builder = webClientBuilder
                .baseUrl(aiServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        if (aiServiceToken != null && !aiServiceToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiServiceToken);
        } else {
            log.warn("AI_SERVICE_TOKEN is not configured; protected AI service calls will be rejected");
        }
        return builder.build();
    }
}
