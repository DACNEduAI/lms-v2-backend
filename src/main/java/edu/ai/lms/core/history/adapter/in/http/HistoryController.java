package edu.ai.lms.core.history.adapter.in.http;

import edu.ai.lms.core.history.adapter.in.http.response.PaginatedHistoryResponse;
import edu.ai.lms.core.history.app.port.in.usecase.GetHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Activity History API")
public class HistoryController {

    private final GetHistoryUseCase getHistoryUseCase;

    @GetMapping("/me")
    @Operation(summary = "Get current user activity history")
    public ResponseEntity<PaginatedHistoryResponse> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getHistoryUseCase.getMyHistory(page, size));
    }
}