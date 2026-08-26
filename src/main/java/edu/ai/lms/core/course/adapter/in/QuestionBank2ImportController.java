package edu.ai.lms.core.course.adapter.in;

import static edu.ai.lms.core.course.app.service.qbank2.QBank2ImportModels.*;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.app.service.qbank2.QBank2ImportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/qbank2/imports")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBank2ImportController {
    private final QBank2ImportService service;

    @PostMapping("/preview")
    public ImportPreview preview(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        return service.preview(courseId, request);
    }

    @PostMapping
    public ResponseEntity<ImportResult> commit(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        ImportResult result = service.commit(courseId, request);
        return ResponseEntity.status(result.summary().createdGroups() > 0 ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }
}
