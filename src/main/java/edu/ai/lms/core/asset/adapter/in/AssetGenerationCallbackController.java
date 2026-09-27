package edu.ai.lms.core.asset.adapter.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import edu.ai.lms.core.asset.app.service.AssetImageGenerationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/assets/image-generations")
@RequiredArgsConstructor
public class AssetGenerationCallbackController {
    private final AssetImageGenerationService generation;
    @Value("${asset.generation.webhook.secret:asset-generation}") private String secret;

    @PostMapping("/callback")
    public void callback(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody CompletionRequest request) {
        if (secret.isBlank() || !("Bearer " + secret).equals(authorization)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid generation callback");
        generation.complete(new AssetImageGenerationService.Completion(request.eventId(), request.jobId(), request.assetId(), request.attempt(), request.status(), request.errorMessage(), request.errorDetails(), request.assetName(), request.description(), request.usageTags(), request.tikzSource(), request.model(), request.parameters()));
    }
    public record CompletionRequest(UUID eventId, UUID jobId, UUID assetId, int attempt, String status, String errorMessage, String errorDetails, String assetName, String description, List<String> usageTags, String tikzSource, String model, Map<String, Object> parameters) {}
}
