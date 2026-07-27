package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import edu.ai.lms.core.course.adapter.in.mapper.CustomLearningLevelCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.BatchEditCustomLearningLevelsRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateCustomLearningLevelRequest;
import edu.ai.lms.core.course.adapter.in.response.*;
import edu.ai.lms.core.course.app.port.in.usecase.CustomLearningLevelUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/custom-learning-levels")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
@Tag(name = "Custom Learning Level Controller",
        description = "Teacher-managed course learning levels with editable mappings to one or more learning outcomes.")
public class CustomLearningLevelController {
    private final CustomLearningLevelUseCase useCase;
    private final CustomLearningLevelCommandMapper mapper;

    @PostMapping
    @Operation(summary = "Create a custom learning level")
    public ResponseEntity<CustomLearningLevelResponse> create(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateCustomLearningLevelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                useCase.create(courseId, mapper.toValue(request)));
    }

    @GetMapping
    @Operation(summary = "List custom learning levels for a course")
    public ResponseEntity<CustomLearningLevelListResponse> getAll(@PathVariable UUID courseId) {
        return ResponseEntity.ok(useCase.getAll(courseId));
    }

    @GetMapping("/{levelId}")
    @Operation(summary = "Get one custom learning level")
    public ResponseEntity<CustomLearningLevelResponse> getOne(
            @PathVariable UUID courseId,
            @PathVariable UUID levelId) {
        return ResponseEntity.ok(useCase.getOne(courseId, levelId));
    }

    @PutMapping("/batch")
    @Operation(summary = "Transactionally create, update, and delete custom learning levels")
    public ResponseEntity<BatchEditCustomLearningLevelsResponse> batchEdit(
            @PathVariable UUID courseId,
            @Valid @RequestBody BatchEditCustomLearningLevelsRequest request) {
        return ResponseEntity.ok(useCase.batchEdit(mapper.toCommand(courseId, request)));
    }

    @DeleteMapping("/{levelId}")
    @Operation(summary = "Delete one unused custom learning level")
    public ResponseEntity<DeleteCustomLearningLevelResponse> delete(
            @PathVariable UUID courseId,
            @PathVariable UUID levelId) {
        return ResponseEntity.ok(useCase.delete(courseId, levelId));
    }
}
