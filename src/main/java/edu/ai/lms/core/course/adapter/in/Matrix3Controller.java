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

import edu.ai.lms.core.course.app.service.matrix3.Matrix3Service;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/matrix-3")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class Matrix3Controller {
    private final Matrix3Service service;

    @GetMapping public Matrix3Service.Matrix3List list(@PathVariable UUID courseId) { return service.list(courseId); }
    @GetMapping("/{matrixId}") public Matrix3Service.Matrix3Detail get(@PathVariable UUID courseId,@PathVariable UUID matrixId) { return service.get(courseId,matrixId); }
    @PostMapping public ResponseEntity<Matrix3Service.Matrix3Detail> create(@PathVariable UUID courseId,@RequestBody Matrix3Service.Matrix3Payload input) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(courseId,input)); }
    @PutMapping("/{matrixId}") public Matrix3Service.Matrix3Detail update(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody Matrix3Service.Matrix3Payload input) { return service.update(courseId,matrixId,input); }
    @PostMapping("/{matrixId}/copy") public ResponseEntity<Matrix3Service.Matrix3Detail> copy(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody(required=false) Matrix3Service.Matrix3CopyRequest input) { return ResponseEntity.status(HttpStatus.CREATED).body(service.copyAsDraft(courseId,matrixId,input)); }
    @PostMapping("/{matrixId}/publish") public Matrix3Service.Matrix3Detail publish(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody Matrix3Service.Matrix3PublishRequest input) { return service.publish(courseId,matrixId,input); }
    @DeleteMapping("/{matrixId}") public ResponseEntity<Void> delete(@PathVariable UUID courseId,@PathVariable UUID matrixId) { service.delete(courseId,matrixId); return ResponseEntity.noContent().build(); }
}
