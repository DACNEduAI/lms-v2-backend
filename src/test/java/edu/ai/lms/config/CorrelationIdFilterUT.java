package edu.ai.lms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterUT {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesSafeCorrelationIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "batch-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertEquals("batch-123", MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("batch-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "unsafe\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertNotEquals("unsafe\nvalue", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(36, response.getHeader(CorrelationIdFilter.HEADER_NAME).length());
    }
}
