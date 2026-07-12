package edu.ai.lms.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${ai.rag.url:http://localhost:8000}")
    private String aiRagUrl;

    @Value("${ai.quiz.url:http://localhost:8000}")
    private String aiQuizUrl;

    @Bean
    public WebClient aiRagWebClient(WebClient.Builder webClientBuilder) {
        return createWebClient(webClientBuilder, aiRagUrl);
    }

    @Bean
    public WebClient aiQuizWebClient(WebClient.Builder webClientBuilder) {
        return createWebClient(webClientBuilder, aiQuizUrl);
    }

    private WebClient createWebClient(WebClient.Builder webClientBuilder, String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(300))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(300, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(300, TimeUnit.SECONDS)));

        return webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
