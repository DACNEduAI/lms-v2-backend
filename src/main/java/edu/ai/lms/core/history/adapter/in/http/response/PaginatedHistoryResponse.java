package edu.ai.lms.core.history.adapter.in.http.response;

import edu.ai.lms.core.history.app.domain.ActivityLog;
import java.util.List;
import lombok.Data;

@Data
public class PaginatedHistoryResponse {
    private List<ActivityLog> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}