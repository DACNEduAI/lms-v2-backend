package edu.ai.lms.core.course.adapter.in;

import static edu.ai.lms.core.course.app.service.qbank2.QBank2ImportModels.*;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ImportPreview previewNew(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        return service.previewNew(courseId, request);
    }

    @PostMapping
    public ResponseEntity<ImportResult> commitNew(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.commitNew(courseId, request));
    }

    @PutMapping("/preview")
    public ImportPreview previewExisting(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        return service.previewExisting(courseId, request);
    }

    @PutMapping
    public ImportResult commitExisting(@PathVariable UUID courseId, @RequestBody ImportRequest request) {
        return service.commitExisting(courseId, request);
    }
}
