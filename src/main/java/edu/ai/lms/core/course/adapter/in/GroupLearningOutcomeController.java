package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.AddLearningOutcomesToGroupCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.CreateGroupLearningOutcomeCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.RemoveLearningOutcomesFromGroupCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.AddLearningOutcomesToGroupRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateGroupLearningOutcomeRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyGroupLearningOutcomeRequest;
import edu.ai.lms.core.course.adapter.in.request.RemoveLearningOutcomesFromGroupRequest;
import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToGroupResponse;
import edu.ai.lms.core.course.adapter.in.response.CreateGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromGroupResponse;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToGroupCommand;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.command.DeleteGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromGroupCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddLearningOutcomesToGroupUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveLearningOutcomesFromGroupUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Group Learning Outcome Controller", description = "Endpoints for grouping and reorganizing course learning outcomes.")
@RequiredArgsConstructor
public class GroupLearningOutcomeController {

    private final TeacherCreateGroupLearningOutcomeUseCase teacherCreateGroupLearningOutcomeUseCase;
    private final TeacherAddLearningOutcomesToGroupUseCase teacherAddLearningOutcomesToGroupUseCase;
    private final TeacherRemoveLearningOutcomesFromGroupUseCase teacherRemoveLearningOutcomesFromGroupUseCase;
    private final TeacherDeleteGroupLearningOutcomeUseCase teacherDeleteGroupLearningOutcomeUseCase;
    private final TeacherModifyGroupLearningOutcomeUseCase teacherModifyGroupLearningOutcomeUseCase;
    private final CreateGroupLearningOutcomeCommandMapper createGroupLearningOutcomeCommandMapper;
    private final AddLearningOutcomesToGroupCommandMapper addLearningOutcomesToGroupCommandMapper;
    private final RemoveLearningOutcomesFromGroupCommandMapper removeLearningOutcomesFromGroupCommandMapper;

    @Operation(summary = "Create a group learning outcome",
            description = "Creates a group learning outcome and assigns existing child outcomes. LMS logic: builds a CLO hierarchy for curriculum organization; a learning outcome cannot be in conflicting groups. Path field: courseId. Request fields: name, description, learningOutcomeIds[]. Response fields: id, name, description, courseId, learningOutcomes[] (id, code, description), createdAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Group learning outcome created successfully",
                    content = @Content(schema = @Schema(implementation = CreateGroupLearningOutcomeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or learning outcomes not found"),
            @ApiResponse(responseCode = "400", description = "Learning outcomes already assigned to another group"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/{courseId}/group-learning-outcome")
    public ResponseEntity<CreateGroupLearningOutcomeResponse> createGroupLearningOutcome(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateGroupLearningOutcomeRequest request) {
        CreateGroupLearningOutcomeCommand command = createGroupLearningOutcomeCommandMapper.toCommand(courseId, request);
        CreateGroupLearningOutcomeResponse response = teacherCreateGroupLearningOutcomeUseCase.createGroupLearningOutcome(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Add learning outcomes to a group",
            description = "Adds existing learning outcomes to a group. LMS logic: extends group composition while enforcing non-conflicting membership. Path fields: courseId, groupId. Request fields: learningOutcomeIds[]. Response fields: groupId, name, courseId, learningOutcomes[] (id, code, description), totalAdded.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes added to group successfully",
                    content = @Content(schema = @Schema(implementation = AddLearningOutcomesToGroupResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course, group, or learning outcomes not found"),
            @ApiResponse(responseCode = "400", description = "Learning outcomes already assigned to another group"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/group-learning-outcome/{groupId}/add-learning-outcomes")
    public ResponseEntity<AddLearningOutcomesToGroupResponse> addLearningOutcomesToGroup(
            @PathVariable UUID courseId,
            @PathVariable UUID groupId,
            @Valid @RequestBody AddLearningOutcomesToGroupRequest request) {
        AddLearningOutcomesToGroupCommand command = addLearningOutcomesToGroupCommandMapper.toCommand(courseId, groupId, request);
        AddLearningOutcomesToGroupResponse response = teacherAddLearningOutcomesToGroupUseCase.addLearningOutcomesToGroup(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove learning outcomes from a group",
            description = "Removes learning outcomes from a group (outcomes become ungrouped). LMS logic: supports CLO hierarchy reorganization without deleting outcomes. Path fields: courseId, groupId. Request fields: learningOutcomeIds[]. Response fields: groupId, name, courseId, totalRemoved, message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes removed from group successfully",
                    content = @Content(schema = @Schema(implementation = RemoveLearningOutcomesFromGroupResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course, group, or learning outcomes not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/group-learning-outcome/{groupId}/remove-learning-outcomes")
    public ResponseEntity<RemoveLearningOutcomesFromGroupResponse> removeLearningOutcomesFromGroup(
            @PathVariable UUID courseId,
            @PathVariable UUID groupId,
            @Valid @RequestBody RemoveLearningOutcomesFromGroupRequest request) {
        RemoveLearningOutcomesFromGroupCommand command = removeLearningOutcomesFromGroupCommandMapper.toCommand(courseId, groupId, request);
        RemoveLearningOutcomesFromGroupResponse response = teacherRemoveLearningOutcomesFromGroupUseCase.removeLearningOutcomesFromGroup(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a group learning outcome",
            description = "Deletes a top-level learning outcome group and its child learning outcomes.")
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/{courseId}/group-learning-outcome/{groupId}")
    public ResponseEntity<DeleteGroupLearningOutcomeResponse> deleteGroupLearningOutcome(
            @PathVariable UUID courseId,
            @PathVariable UUID groupId) {
        DeleteGroupLearningOutcomeCommand command = DeleteGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .build();
        DeleteGroupLearningOutcomeResponse response = teacherDeleteGroupLearningOutcomeUseCase.deleteGroupLearningOutcome(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Modify a group learning outcome",
            description = "Updates a top-level learning outcome group description.")
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/group-learning-outcome/{groupId}")
    public ResponseEntity<GetGroupLearningOutcomeResponse> modifyGroupLearningOutcome(
            @PathVariable UUID courseId,
            @PathVariable UUID groupId,
            @Valid @RequestBody ModifyGroupLearningOutcomeRequest request) {
        ModifyGroupLearningOutcomeCommand command = ModifyGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .description(request.getDescription())
                .build();
        GetGroupLearningOutcomeResponse response = teacherModifyGroupLearningOutcomeUseCase.modifyGroupLearningOutcome(command);
        return ResponseEntity.ok(response);
    }
}
