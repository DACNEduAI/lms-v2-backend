package edu.ai.lms.core.material.adapter.in;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.material.app.port.in.usecase.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.material.adapter.in.mapper.CreateMaterialCommandMapper;
import edu.ai.lms.core.material.adapter.in.mapper.ModifyMaterialCommandMapper;
import edu.ai.lms.core.material.adapter.in.request.CreateMaterialRequest;
import edu.ai.lms.core.material.adapter.in.request.ModifyMaterialRequest;
import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;
import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.adapter.in.response.DeleteMaterialResponse;
import edu.ai.lms.core.material.adapter.in.response.GetMaterialResponse;
import edu.ai.lms.core.material.adapter.in.response.ModifyMaterialResponse;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;
import edu.ai.lms.core.material.app.port.in.command.ModifyMaterialCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/material")
@RequiredArgsConstructor
public class MaterialController {

    private final CreateMaterialUseCase createMaterialUseCase;
    private final GetMaterialUseCase getMaterialUseCase;
    private final GetUploadedMaterialsUseCase getUploadedMaterialsUseCase;
    private final GetChapterMaterialsUseCase getChapterMaterialsUseCase;
    private final ModifyMaterialUseCase modifyMaterialUseCase;
    private final DeleteMaterialUseCase deleteMaterialUseCase;
    private final ConfirmUploadUseCase confirmUploadUseCase;
    private final CreateMaterialCommandMapper createMaterialCommandMapper;
    private final ModifyMaterialCommandMapper modifyMaterialCommandMapper;

    @Operation(summary = "Get all uploaded materials for a course",
            description = "Returns uploaded materials for a course. LMS logic: only materials in UPLOADED state are surfaced to consumers. Path field: courseId. Response fields per item: id, title, type, fileKey, isDownloadable, status, presignedUrl, courseId, createdAt, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uploaded materials retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this course")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping
    public ResponseEntity<List<GetMaterialResponse>> getUploadedMaterials(
            @PathVariable UUID courseId) {
        List<GetMaterialResponse> response = getUploadedMaterialsUseCase.getUploadedMaterials(courseId);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Download a specific material", description = "Gets one material by ID. LMS logic: returns content metadata and secure access URL details for course material retrieval. Path fields: courseId, materialId. Response fields: id, title, type, fileKey, isDownloadable, status, presignedUrl, courseId, createdAt, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Material retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Material not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this material")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{materialId}")
    public ResponseEntity<GetMaterialResponse> getMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID materialId) {
        GetMaterialResponse response = getMaterialUseCase.getMaterial(courseId, materialId);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Create a new general material for a course", description = "Creates a course-level material upload record. LMS logic: prepares file metadata and presigned upload URL for direct client upload. Path field: courseId. Request fields: title, type (PDF/VIDEO/LINK), isDownloadable. Response fields: id, title, type, fileKey, isDownloadable, status, presignedUploadUrl, courseId, createdAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Material created successfully",
                    content = @Content(schema = @Schema(implementation = CreateMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add materials to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public ResponseEntity<CreateMaterialResponse> createMaterialForCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateMaterialRequest request
    ) {
        CreateMaterialForCourseCommand command = createMaterialCommandMapper.toCommand(courseId, request);
        CreateMaterialResponse response = createMaterialUseCase.createMaterialForCourse(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all materials for a specific chapter",
            description = "Returns materials linked to a chapter. LMS logic: chapter-scoped content retrieval for structured learning flow. Path fields: courseId, chapterId. Response fields per item: id, title, type, fileKey, isDownloadable, status, presignedUrl, courseId, createdAt, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter materials retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this course")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<GetMaterialResponse>> getChapterMaterials(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        List<GetMaterialResponse> response = getChapterMaterialsUseCase.getChapterMaterials(courseId, chapterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new material for a specific chapter",
            description = "Creates a chapter-level material upload record. LMS logic: associates newly created material with a specific instructional chapter. Path fields: courseId, chapterId. Request fields: title, type, isDownloadable. Response fields: id, title, type, fileKey, isDownloadable, status, presignedUploadUrl, courseId, createdAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Material created for chapter successfully",
                    content = @Content(schema = @Schema(implementation = CreateMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add materials to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/chapter/{chapterId}")
    public ResponseEntity<CreateMaterialResponse> createMaterialForChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody CreateMaterialRequest request
    ) {
        CreateMaterialForChapterCommand command = createMaterialCommandMapper.toCommand(courseId, chapterId, request);
        CreateMaterialResponse response = createMaterialUseCase.createMaterialForChapter(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @Operation(summary = "Modify an existing material", description = "Updates material metadata. LMS logic: course owner can revise title/type/download settings while preserving material lifecycle tracking. Path fields: courseId, materialId. Request fields: title, type, isDownloadable. Response fields: id, title, type, fileKey, isDownloadable, status, presignedUploadUrl, courseId, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Material modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or material not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify materials in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{materialId}")
    public ResponseEntity<ModifyMaterialResponse> modifyMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID materialId,
            @Valid @RequestBody ModifyMaterialRequest request
    ) {
        ModifyMaterialCommand command = modifyMaterialCommandMapper.toCommand(courseId, materialId, request);
        ModifyMaterialResponse response = modifyMaterialUseCase.modifyMaterial(command);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Delete a material", description = "Deletes a material from a course. LMS logic: removes material association and related content references. Path fields: courseId, materialId. Response fields: courseId, materialId, message, success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Material deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or material not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete materials in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{materialId}")
    public ResponseEntity<DeleteMaterialResponse> deleteMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID materialId) {
        DeleteMaterialResponse response = deleteMaterialUseCase.deleteMaterial(courseId, materialId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirm material upload",
            description = "Confirms upload completion for a material file. LMS logic: verifies storage object existence and updates material status in lifecycle. Path fields: courseId, materialId. Response fields: materialId, courseId, fileKey, status, uploaded, message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload verification completed",
                    content = @Content(schema = @Schema(implementation = ConfirmUploadResponse.class))),
            @ApiResponse(responseCode = "404", description = "Material not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{materialId}/confirm-upload")
    public ResponseEntity<ConfirmUploadResponse> confirmUpload(
            @PathVariable UUID courseId,
            @PathVariable UUID materialId) {
        ConfirmUploadResponse response = confirmUploadUseCase.confirmUpload(courseId, materialId);
        return ResponseEntity.ok(response);
    }
}
