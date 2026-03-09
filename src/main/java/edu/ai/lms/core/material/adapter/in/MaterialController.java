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
            description = "Returns all materials with UPLOADED status for a course. No presigned URLs are included.")
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

    @Operation(summary = "Download a specific material", description = "Download a material by its ID within a course.")
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

    @Operation(summary = "Create a new general material for a course", description = "Upload a new general material within a course.")
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
            description = "Returns all materials linked to a given chapter within a course. No presigned URLs are included.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter materials retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this course")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/chapter/{chapterNumber}")
    public ResponseEntity<List<GetMaterialResponse>> getChapterMaterials(
            @PathVariable UUID courseId,
            @PathVariable Long chapterNumber) {
        List<GetMaterialResponse> response = getChapterMaterialsUseCase.getChapterMaterials(courseId, chapterNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new material for a specific chapter",
            description = "Upload a new material and associate it with a specific chapter within a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Material created for chapter successfully",
                    content = @Content(schema = @Schema(implementation = CreateMaterialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add materials to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/chapter/{chapterNumber}")
    public ResponseEntity<CreateMaterialResponse> createMaterialForChapter(
            @PathVariable UUID courseId,
            @PathVariable Long chapterNumber,
            @Valid @RequestBody CreateMaterialRequest request
    ) {
        CreateMaterialForChapterCommand command = createMaterialCommandMapper.toCommand(courseId, chapterNumber, request);
        CreateMaterialResponse response = createMaterialUseCase.createMaterialForChapter(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modify an existing material", description = "Modify a material within a course. Only the course owner (teacher) can modify materials.")
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

    @Operation(summary = "Delete a material", description = "Delete a material from a course. Only the course owner (teacher) can delete materials.")
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
            description = "Verify whether the file for a material has been uploaded to storage successfully. Updates the material status accordingly.")
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
