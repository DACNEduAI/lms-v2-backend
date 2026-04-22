package edu.ai.lms.core.history.app.service;

import edu.ai.lms.core.history.adapter.in.http.response.PaginatedHistoryResponse;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.in.usecase.GetHistoryUseCase;
import edu.ai.lms.core.history.app.port.out.LoadActivityLogPort;
import edu.ai.lms.core.history.app.port.out.LoadCurrentUserPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetHistoryService implements GetHistoryUseCase {

    private final LoadActivityLogPort loadActivityLogPort;
    private final LoadCurrentUserPort loadCurrentUserPort;

    @Override
    @Transactional(readOnly = true)
    public PaginatedHistoryResponse getMyHistory(int page, int size) {
        UUID currentUserId = loadCurrentUserPort.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        long totalElements = loadActivityLogPort.countByUserId(currentUserId);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<ActivityLog> content = loadActivityLogPort.findByUserId(currentUserId, offset, size);

        PaginatedHistoryResponse response = new PaginatedHistoryResponse();
        response.setContent(content);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setLast(page >= totalPages - 1 || totalElements == 0);

        return response;
    }
}