package edu.ai.lms.core.history.app.port.in.usecase;

import edu.ai.lms.core.history.adapter.in.http.response.PaginatedHistoryResponse;

public interface GetHistoryUseCase {
    PaginatedHistoryResponse getMyHistory(int page, int size);
}