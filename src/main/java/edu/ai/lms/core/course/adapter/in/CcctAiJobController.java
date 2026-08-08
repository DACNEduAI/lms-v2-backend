package edu.ai.lms.core.course.adapter.in;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.course.adapter.in.mapper.CcctAiJobResponseMapper;
import edu.ai.lms.core.course.adapter.in.request.StartCcctStructureJobRequest;
import edu.ai.lms.core.course.adapter.in.response.CcctAiJobResponse;
import edu.ai.lms.core.course.app.service.ccct.CcctAiJobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/ccct-profiles/{profileId}/ai-jobs")
@Tag(name = "CCCT AI Jobs", description = "PDF and Custom CCCT conversion jobs")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class CcctAiJobController {
    private final CcctAiJobService service;
    private final CcctAiJobResponseMapper mapper;

    @PostMapping(value = "/pdf", consumes = "multipart/form-data")
    public ResponseEntity<CcctAiJobResponse> startPdf(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @RequestParam @PositiveOrZero long version,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(service.startPdf(
                courseId,
                profileId,
                version,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes())));
    }

    @PostMapping("/structure")
    public ResponseEntity<CcctAiJobResponse> startStructure(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @Valid @RequestBody StartCcctStructureJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(
                service.startStructure(courseId, profileId, request.version())));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<CcctAiJobResponse> get(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(mapper.toResponse(service.get(courseId, profileId, jobId)));
    }
}
