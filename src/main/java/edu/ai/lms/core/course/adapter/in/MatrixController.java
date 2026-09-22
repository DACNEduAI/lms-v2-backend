package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import edu.ai.lms.core.course.app.service.matrix.MatrixService;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/courses/{courseId}/matrix") @PreAuthorize("hasRole('TEACHER')") @RequiredArgsConstructor
public class MatrixController {
 private final MatrixService service;
 @GetMapping public MatrixService.MatrixList list(@PathVariable UUID courseId){return service.list(courseId);}
 @GetMapping("/{matrixId}") public MatrixService.MatrixDetail get(@PathVariable UUID courseId,@PathVariable UUID matrixId){return service.get(courseId,matrixId);}
 @PostMapping public ResponseEntity<MatrixService.MatrixDetail> create(@PathVariable UUID courseId,@RequestBody MatrixService.MatrixPayload payload){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(courseId,payload));}
 @PostMapping("/allocation-preview") public MatrixService.AllocationPreview allocationPreview(@PathVariable UUID courseId,@RequestBody MatrixService.AllocationPreviewRequest request){return service.previewAllocation(request);}
 @PutMapping("/{matrixId}") public MatrixService.MatrixDetail update(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody MatrixService.MatrixPayload payload){return service.update(courseId,matrixId,payload);}
 @PostMapping("/{matrixId}/copy") public ResponseEntity<MatrixService.MatrixDetail> copy(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody(required=false) MatrixService.CopyRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.copyAsDraft(courseId,matrixId,request));}
 @PostMapping("/{matrixId}/publish") public MatrixService.MatrixDetail publish(@PathVariable UUID courseId,@PathVariable UUID matrixId,@RequestBody MatrixService.PublishRequest request){return service.publish(courseId,matrixId,request);}
 @DeleteMapping("/{matrixId}") public ResponseEntity<Void> delete(@PathVariable UUID courseId,@PathVariable UUID matrixId){service.delete(courseId,matrixId);return ResponseEntity.noContent().build();}
}
