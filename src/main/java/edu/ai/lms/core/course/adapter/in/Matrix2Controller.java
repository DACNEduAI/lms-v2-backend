package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.app.service.matrix2.Matrix2Service;
import lombok.RequiredArgsConstructor;

/** Matrix 2 is a separate teacher-facing API and is intentionally not wired to AI. */
@RestController
@RequestMapping("/api/courses/{courseId}/matrix-2")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class Matrix2Controller {
    private final Matrix2Service service;

    @GetMapping
    public ResponseEntity<Matrix2Service.Matrix2List> list(@PathVariable UUID courseId) {
        return ResponseEntity.ok(service.list(courseId));
    }

    @GetMapping("/{matrixId}")
    public ResponseEntity<Matrix2Service.Matrix2Detail> get(
            @PathVariable UUID courseId, @PathVariable UUID matrixId) {
        return ResponseEntity.ok(service.get(courseId, matrixId));
    }

    @PostMapping
    public ResponseEntity<Matrix2Service.Matrix2Detail> create(
            @PathVariable UUID courseId, @RequestBody Matrix2Service.Matrix2Payload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(courseId, payload));
    }

    @PostMapping("/{matrixId}/copy")
    public ResponseEntity<Matrix2Service.Matrix2Detail> copyAsDraft(
            @PathVariable UUID courseId, @PathVariable UUID matrixId,
            @RequestBody(required = false) Matrix2Service.Matrix2CopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.copyAsDraft(courseId, matrixId, request));
    }

    @PostMapping("/{matrixId}/publish")
    public ResponseEntity<Matrix2Service.Matrix2Detail> publish(
            @PathVariable UUID courseId, @PathVariable UUID matrixId,
            @RequestBody Matrix2Service.Matrix2PublishRequest request) {
        return ResponseEntity.ok(service.publish(courseId, matrixId, request));
    }

    @PutMapping("/{matrixId}")
    public ResponseEntity<Matrix2Service.Matrix2Detail> update(
            @PathVariable UUID courseId, @PathVariable UUID matrixId,
            @RequestBody Matrix2Service.Matrix2Payload payload) {
        return ResponseEntity.ok(service.update(courseId, matrixId, payload));
    }

    @DeleteMapping("/{matrixId}")
    public ResponseEntity<Void> delete(@PathVariable UUID courseId, @PathVariable UUID matrixId) {
        service.delete(courseId, matrixId);
        return ResponseEntity.noContent().build();
    }
}
