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

import edu.ai.lms.core.course.adapter.in.mapper.CreateExamMatrixCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.BatchEditExamMatrixCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.BatchEditExamMatrixRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateExamMatrixRequest;
import edu.ai.lms.core.course.adapter.in.response.BatchEditExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.ExamMatrixListResponse;
import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllExamMatricesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherBatchEditExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteExamMatrixUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for exam matrix CRUD operations.
 * All endpoints are restricted to users with the TEACHER role.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/exam-matrices")
@Tag(name = "Exam Matrix Controller", description = "Endpoints for creating, viewing, updating, and deleting exam matrices.")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class ExamMatrixController {

    private final TeacherCreateExamMatrixUseCase teacherCreateExamMatrixUseCase;
    private final TeacherBatchEditExamMatrixUseCase teacherBatchEditExamMatrixUseCase;
    private final TeacherDeleteExamMatrixUseCase teacherDeleteExamMatrixUseCase;
    private final GetExamMatrixUseCase getExamMatrixUseCase;
    private final GetAllExamMatricesUseCase getAllExamMatricesUseCase;
    private final CreateExamMatrixCommandMapper createExamMatrixCommandMapper;
    private final BatchEditExamMatrixCommandMapper batchEditExamMatrixCommandMapper;

    // ── GET all ──────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get all exam matrices for a course",
            description = """
                    Returns all exam matrices that belong to the specified course.

                    Path fields:
                    - courseId: Target course ID.

                    Response fields:
                    - courseId, total, matrices[]: lightweight summaries with row and question totals.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam matrices retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExamMatrixListResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @GetMapping
    public ResponseEntity<ExamMatrixListResponse> getAllExamMatrices(
            @Parameter(description = "Course ID") @PathVariable UUID courseId) {
        ExamMatrixListResponse response = getAllExamMatricesUseCase.getAllExamMatrices(courseId);
        return ResponseEntity.ok(response);
    }

    // ── GET one ──────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get a specific exam matrix",
            description = """
                    Returns one exam matrix by ID within a course.

                    Path fields:
                    - courseId: Target course ID.
                    - matrixId: Target exam matrix ID.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam matrix retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExamMatrixResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or exam matrix not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @GetMapping("/{matrixId}")
    public ResponseEntity<ExamMatrixResponse> getExamMatrix(
            @Parameter(description = "Course ID") @PathVariable UUID courseId,
            @Parameter(description = "Exam matrix ID") @PathVariable UUID matrixId) {
        ExamMatrixResponse response = getExamMatrixUseCase.getExamMatrix(courseId, matrixId);
        return ResponseEntity.ok(response);
    }

    // ── POST create ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Create a new exam matrix",
            description = """
                    Creates an exam matrix for the specified course.

                    Only the teacher who owns the course may create a matrix.

                    Request fields:
                    - title: Matrix title (required).
                    - description: Optional description.
                    - rows[]: List of exam matrix rows.
                      - chapterId: Chapter ID (required).
                      - topic: Teacher-authored matrix topic (required).
                      - orderIndex: Display order.
                      - learningOutcomeIds: One or more outcomes assigned to the chapter.
                      - levelCounts: Learning-level reference, explicit MULTIPLE_CHOICE type,
                        and non-negative question count.
                    - customLearningLevels[]: At least one matrix-scoped level created atomically
                      with the matrix.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exam matrix created successfully",
                    content = @Content(schema = @Schema(implementation = ExamMatrixResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PostMapping
    public ResponseEntity<ExamMatrixResponse> createExamMatrix(
            @Parameter(description = "Course ID") @PathVariable UUID courseId,
            @Valid @RequestBody CreateExamMatrixRequest request) {
        var command = createExamMatrixCommandMapper.toCommand(courseId, request);
        ExamMatrixResponse response = teacherCreateExamMatrixUseCase.createExamMatrix(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── PUT transactional delta batch ─────────────────────────────────────────

    @Operation(
            summary = "Apply a transactional batch of exam matrix changes",
            description = """
                    Applies row creates, updates, and deletes in one transaction.
                    Updated row child relationships are replaced by the submitted lists.

                    Path fields:
                    - courseId: Target course ID.
                    - matrixId: Target exam matrix ID.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam matrix updated successfully",
                    content = @Content(schema = @Schema(implementation = BatchEditExamMatrixResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Course or exam matrix not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PutMapping({"/{matrixId}/rows/batch", "/{matrixId}/configuration/batch"})
    public ResponseEntity<BatchEditExamMatrixResponse> batchEditExamMatrix(
            @Parameter(description = "Course ID") @PathVariable UUID courseId,
            @Parameter(description = "Exam matrix ID") @PathVariable UUID matrixId,
            @Valid @RequestBody BatchEditExamMatrixRequest request) {
        var command = batchEditExamMatrixCommandMapper.toCommand(courseId, matrixId, request);
        BatchEditExamMatrixResponse response = teacherBatchEditExamMatrixUseCase.batchEdit(command);
        return ResponseEntity.ok(response);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete an exam matrix",
            description = """
                    Permanently deletes an exam matrix and all its rows.

                    Path fields:
                    - courseId: Target course ID.
                    - matrixId: Target exam matrix ID.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam matrix deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteExamMatrixResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or exam matrix not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @DeleteMapping("/{matrixId}")
    public ResponseEntity<DeleteExamMatrixResponse> deleteExamMatrix(
            @Parameter(description = "Course ID") @PathVariable UUID courseId,
            @Parameter(description = "Exam matrix ID") @PathVariable UUID matrixId) {
        DeleteExamMatrixResponse response = teacherDeleteExamMatrixUseCase.deleteExamMatrix(courseId, matrixId);
        return ResponseEntity.ok(response);
    }
}
