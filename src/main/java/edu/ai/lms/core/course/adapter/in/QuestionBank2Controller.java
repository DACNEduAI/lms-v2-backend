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

import edu.ai.lms.core.course.app.service.qbank2.QuestionBank2Service;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/qbank2/groups")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBank2Controller {
    private final QuestionBank2Service service;

    @GetMapping public QuestionBank2Service.QuestionBank2View list(@PathVariable UUID courseId) { return service.list(courseId); }
    @PostMapping public ResponseEntity<QuestionBank2Service.GroupView> createGroup(@PathVariable UUID courseId, @RequestBody QuestionBank2Service.GroupInput input) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(courseId, input)); }
    @PutMapping("/{groupId}") public QuestionBank2Service.GroupView updateGroup(@PathVariable UUID courseId, @PathVariable UUID groupId, @RequestBody QuestionBank2Service.GroupInput input) { return service.updateGroup(courseId, groupId, input); }
    @DeleteMapping("/{groupId}") public ResponseEntity<Void> deleteGroup(@PathVariable UUID courseId, @PathVariable UUID groupId) { service.deleteGroup(courseId, groupId); return ResponseEntity.noContent().build(); }
    @PostMapping("/{groupId}/examples") public ResponseEntity<QuestionBank2Service.ExampleView> createExample(@PathVariable UUID courseId, @PathVariable UUID groupId, @RequestBody QuestionBank2Service.ExampleInput input) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createExample(courseId, groupId, input)); }
    @PutMapping("/{groupId}/examples/{exampleId}") public QuestionBank2Service.ExampleView updateExample(@PathVariable UUID courseId, @PathVariable UUID groupId, @PathVariable UUID exampleId, @RequestBody QuestionBank2Service.ExampleInput input) { return service.updateExample(courseId, groupId, exampleId, input); }
    @DeleteMapping("/{groupId}/examples/{exampleId}") public ResponseEntity<Void> deleteExample(@PathVariable UUID courseId, @PathVariable UUID groupId, @PathVariable UUID exampleId) { service.deleteExample(courseId, groupId, exampleId); return ResponseEntity.noContent().build(); }
}
