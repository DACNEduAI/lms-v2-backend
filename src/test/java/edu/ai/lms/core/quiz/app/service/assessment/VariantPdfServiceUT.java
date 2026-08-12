package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.core.quiz.adapter.in.http.AssessmentController;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.exception.VariantPdfNotFoundException;
import edu.ai.lms.core.quiz.app.service.richcontent.AssessmentRichContentService;

class VariantPdfServiceUT {
    @Test
    void selectsTheRequestedVariantAndBuildsASafeFilename() {
        AssessmentService assessments = mock(AssessmentService.class);
        VariantPdfRenderer renderer = mock(VariantPdfRenderer.class);
        VariantPdfService service = new VariantPdfService(assessments, renderer);
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AssessmentView assessment = mock(AssessmentView.class);
        VariantView variant = mock(VariantView.class);
        when(assessment.title()).thenReturn("Đề thi Vật lý");
        when(variant.id()).thenReturn(variantId);
        when(variant.code()).thenReturn("001");
        when(assessments.get(courseId, assessmentId)).thenReturn(assessment);
        when(assessments.variants(courseId, assessmentId)).thenReturn(List.of(variant));
        when(renderer.render(variant)).thenReturn(new byte[] {1, 2, 3});

        var result = service.generate(courseId, assessmentId, variantId);

        assertEquals("de-thi-vat-ly-quiz-001.pdf", result.filename());
        assertEquals(3, result.bytes().length);
    }

    @Test
    void rejectsAVariantOutsideTheAssessment() {
        AssessmentService assessments = mock(AssessmentService.class);
        VariantPdfService service = new VariantPdfService(assessments, mock(VariantPdfRenderer.class));
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        when(assessments.get(courseId, assessmentId)).thenReturn(mock(AssessmentView.class));
        when(assessments.variants(courseId, assessmentId)).thenReturn(List.of());

        assertThrows(VariantPdfNotFoundException.class,
                () -> service.generate(courseId, assessmentId, UUID.randomUUID()));
    }

    @Test
    void controllerReturnsAnAttachmentWithoutCaching() {
        AssessmentService assessments = mock(AssessmentService.class);
        VariantPdfService pdfs = mock(VariantPdfService.class);
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        when(pdfs.generate(courseId, assessmentId, variantId))
                .thenReturn(new VariantPdfService.GeneratedVariantPdf(bytes, "de-thi-quiz-001.pdf"));
        AssessmentController controller = new AssessmentController(assessments, pdfs,
                mock(AssessmentRichContentService.class), new ObjectMapper());

        var response = controller.downloadVariantPdf(courseId, assessmentId, variantId);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertArrayEquals(bytes, response.getBody());
    }
}
