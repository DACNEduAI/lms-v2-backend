package edu.ai.lms.core.history.app.service;

import edu.ai.lms.core.history.adapter.in.http.response.PaginatedHistoryResponse;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.LoadActivityLogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHistoryServiceTest {

    @Mock
    private LoadActivityLogPort loadActivityLogPort;

    @Mock
    private edu.ai.lms.core.history.app.port.out.LoadCurrentUserPort loadCurrentUserPort;

    @InjectMocks
    private GetHistoryService getHistoryService;

    @Test
    void shouldReturnPaginatedHistoryCorrectly() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;
        long totalElements = 25; 

        ActivityLog log1 = ActivityLog.builder().actionType("CREATE").build();
        ActivityLog log2 = ActivityLog.builder().actionType("UPDATE").build();

        when(loadCurrentUserPort.getCurrentUserId()).thenReturn(java.util.Optional.of(userId));
        when(loadActivityLogPort.countByUserId(userId)).thenReturn(totalElements);
        when(loadActivityLogPort.findByUserId(userId, 0, size)).thenReturn(List.of(log1, log2));

        // 2. Act
        PaginatedHistoryResponse response = getHistoryService.getMyHistory(page, size);

        // 3. Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages()); // 25 elements / 10 size = 3 pages
        assertFalse(response.isLast()); // Trang 0 chưa phải là trang cuối (isLast = false)
    }
}
