package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.adapter.in.http.mapper.CreateQuizCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.mapper.ModifyQuizCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuizRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.DeleteQuizzesRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuizRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizLearningOutcomeOperationRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuizLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesCreatedByStudentUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesCreatedByTeacherUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.RemoveQuizLearningOutcomesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/quiz")
@Tag(name = "Quiz Controller", description = "Endpoints for quiz lifecycle management, timing policies, visibility, and learning-outcome mappings.")
@RequiredArgsConstructor
public class QuizController {

        private final CreateQuizUseCase createQuizUseCase;
        private final ModifyQuizUseCase modifyQuizUseCase;
        private final DeleteQuizUseCase deleteQuizUseCase;
        private final GetQuizUseCase getQuizUseCase;
        private final GetAllQuizzesCreatedByTeacherUseCase getAllQuizzesCreatedByTeacherUseCase;
        private final GetAllQuizzesCreatedByStudentUseCase getAllQuizzesCreatedByStudentUseCase;
        private final AddQuizLearningOutcomesUseCase addQuizLearningOutcomesUseCase;
        private final RemoveQuizLearningOutcomesUseCase removeQuizLearningOutcomesUseCase;
        private final CreateQuizCommandMapper createQuizCommandMapper;
        private final ModifyQuizCommandMapper modifyQuizCommandMapper;

        @Operation(summary = "Get teacher-created quizzes in a course", description = "Returns teacher-created quizzes in a course. LMS logic: exposes visible teacher-authored assessments to course participants. Path field: courseId. Query fields: pageSize, lastCreated (sort hint). Response fields: quizzes[] (quizId, quizTitle, unlimited, timeLimitMinutes, passScore, startTime, dueTime, attemptLimited, display, createdAt), totalQuizzes, pageSize, lastCreated.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully", content = @Content(schema = @Schema(implementation = GetAllQuizzesResponse.class)))
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @GetMapping("/teacher-created")
        public ResponseEntity<GetAllQuizzesResponse> getAllQuizzesCreatedByTeacher(
                        @PathVariable UUID courseId,
                        @RequestParam(defaultValue = "10") int pageSize,
                        @RequestParam(defaultValue = "asc") String lastCreated) {
                GetAllQuizzesResponse response = getAllQuizzesCreatedByTeacherUseCase
                                .getAllQuizzesCreatedByTeacher(courseId, pageSize, lastCreated);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get student-created quizzes in a course", description = "Returns quizzes created by the authenticated student in the course. LMS logic: personal authored quiz management for students. Path field: courseId. Query fields: pageSize, lastCreated. Response fields: quizzes[] and paging metadata.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully", content = @Content(schema = @Schema(implementation = GetAllQuizzesResponse.class)))
        })
        @PreAuthorize("hasRole('STUDENT')")
        @GetMapping("/student-created")
        public ResponseEntity<GetAllQuizzesResponse> getAllQuizzesCreatedByStudent(
                        @PathVariable UUID courseId,
                        @RequestParam(defaultValue = "10") int pageSize,
                        @RequestParam(defaultValue = "asc") String lastCreated) {
                GetAllQuizzesResponse response = getAllQuizzesCreatedByStudentUseCase
                                .getAllQuizzesCreatedByStudent(courseId, pageSize, lastCreated);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get a specific quiz", description = "Gets full quiz detail. LMS logic: primary aggregate view for quiz delivery/editing, including outcomes and question/answer structure. Path fields: courseId, quizId. Response fields: quiz metadata + ungroupedLearningOutcomes[] + groupedLearningOutcomes[] + questions[] with answers[].")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quiz retrieved successfully", content = @Content(schema = @Schema(implementation = GetQuizResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Quiz not found")
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @GetMapping("/{quizId}")
        public ResponseEntity<GetQuizResponse> getQuiz(
                        @PathVariable UUID courseId,
                        @PathVariable UUID quizId) {
                GetQuizResponse response = getQuizUseCase.getQuiz(courseId, quizId);
                return ResponseEntity.ok(response);
        }

                                @Operation(
                                                                                                summary = "Create a new quiz",
                                                                                                description = """
                                                                                                                                                                Creates a quiz in a course.

                                                                                                                                                                LMS time business logic:
                                                                                                                                                                - If unlimited is true:
                                                                                                                                                                        - startTime must be null.
                                                                                                                                                                        - dueTime must be null.
                                                                                                                                                                        - timeLimitMinutes must be null.
                                                                                                                                                                - If unlimited is false:
                                                                                                                                                                        - dueTime is required.
                                                                                                                                                                        - If startTime is provided, dueTime must be after or equal to startTime.
                                                                                                                                                                - Attempt timer behavior:
                                                                                                                                                                        - Timer mode is active only when unlimited=false and timeLimitMinutes>0.
                                                                                                                                                                        - Effective attempt due time is min(attemptStart + timeLimitMinutes, quiz dueTime).
                                                                                                                                                                        - If timer mode is not active, quiz dueTime is used as attempt deadline.

                                                                                                                                                                Additional LMS behavior:
                                                                                                                                                                - Student-authored quizzes are normalized to unlimited mode.
                                                                                                                                                                - For student-authored quizzes: startTime, dueTime, timeLimitMinutes, and attemptLimited are cleared.

                                                                                                                                                                Path field:
                                                                                                                                                                - courseId.

                                                                                                                                                                Request fields:
                                                                                                                                                                - quizTitle, unlimited, timeLimitMinutes, passScore, startTime, dueTime, attemptLimited, display.

                                                                                                                                                                Response fields:
                                                                                                                                                                - quizId, quizTitle, unlimited, timeLimitMinutes, passScore, startTime, dueTime, attemptLimited, courseId, display, createdAt.
                                                                                                                                                                """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Quiz created successfully", content = @Content(schema = @Schema(implementation = CreateQuizResponse.class)))
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @PostMapping()
        public ResponseEntity<CreateQuizResponse> createQuiz(
                        @PathVariable UUID courseId,
                        @Valid @RequestBody CreateQuizRequest request

                                ) {
                CreateQuizCommand command = createQuizCommandMapper.toCommand(courseId, request);
                CreateQuizResponse response = createQuizUseCase.createQuiz(command);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

                                @Operation(
                                                                                                summary = "Modify an existing quiz",
                                                                                                description = """
                                                                                                                                                                Updates quiz configuration.

                                                                                                                                                                LMS time business logic:
                                                                                                                                                                - If unlimited is true:
                                                                                                                                                                        - startTime must be null.
                                                                                                                                                                        - dueTime must be null.
                                                                                                                                                                - If unlimited is false:
                                                                                                                                                                        - dueTime is required.
                                                                                                                                                                        - If startTime is provided, dueTime must be after or equal to startTime.
                                                                                                                                                                - Attempt timer behavior remains driven by unlimited + timeLimitMinutes at attempt start.

                                                                                                                                                                LMS authorization logic:
                                                                                                                                                                - Only the quiz creator can modify assessment policy, schedule, and visibility.

                                                                                                                                                                Path fields:
                                                                                                                                                                - courseId, quizId.

                                                                                                                                                                Request fields:
                                                                                                                                                                - quizTitle, unlimited, timeLimitMinutes, passScore, startTime, dueTime, attemptLimited, display.

                                                                                                                                                                Response fields:
                                                                                                                                                                - quizId, quizTitle, unlimited, timeLimitMinutes, passScore, startTime, dueTime, attemptLimited, courseId, display, updatedAt.
                                                                                                                                                                """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quiz modified successfully", content = @Content(schema = @Schema(implementation = ModifyQuizResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Quiz not found")
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @PutMapping("/{quizId}")
        public ResponseEntity<ModifyQuizResponse> modifyQuiz(
                        @PathVariable UUID courseId,
                        @PathVariable UUID quizId,
                        @Valid @RequestBody ModifyQuizRequest request) {
                ModifyQuizCommand command = modifyQuizCommandMapper.toCommand(courseId, quizId, request);
                ModifyQuizResponse response = modifyQuizUseCase.modifyQuiz(command);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Delete a quiz", description = "Deletes one quiz. LMS logic: creator removes quiz from course assessment catalog. Path fields: courseId, quizId. Response fields: quizId, message.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quiz deleted successfully", content = @Content(schema = @Schema(implementation = DeleteQuizResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Quiz not found")
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @DeleteMapping("/{quizId}")
        public ResponseEntity<DeleteQuizResponse> deleteQuiz(
                        @PathVariable UUID courseId,
                        @PathVariable UUID quizId) {
                DeleteQuizResponse response = deleteQuizUseCase.deleteQuiz(courseId, quizId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Delete quizzes", description = "Batch deletes quizzes. LMS logic: bulk assessment cleanup. Path field: courseId. Request fields: quizIds[]. Response: list of delete results (quizId, message).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quizzes deleted successfully", content = @Content(schema = @Schema(implementation = DeleteQuizResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Quiz not found")
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @DeleteMapping()
        public ResponseEntity<List<DeleteQuizResponse>> deleteQuizzes(
                        @PathVariable UUID courseId,
                        @Valid @RequestBody DeleteQuizzesRequest request) {
                List<DeleteQuizResponse> responses = request.getQuizIds().stream()
                                .map(quizId -> deleteQuizUseCase.deleteQuiz(courseId, quizId))
                                .collect(Collectors.toList());
                return ResponseEntity.ok(responses);
        }

        @Operation(summary = "Add learning outcomes to a quiz", description = "Adds learning outcomes to a quiz using direct outcome IDs and/or group IDs. LMS logic: expands group IDs to child outcomes and deduplicates mappings. Path fields: courseId, quizId. Request fields: learningOutcomeIds[], groupLearningOutcomeIds[]. Response fields: quizId, courseId, operation, totalAffected, learningOutcomeIds[].")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Learning outcomes added successfully", content = @Content(schema = @Schema(implementation = QuizLearningOutcomeOperationResponse.class)))
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @PostMapping("/{quizId}/learning-outcomes")
        public ResponseEntity<QuizLearningOutcomeOperationResponse> addLearningOutcomes(
                        @PathVariable UUID courseId,
                        @PathVariable UUID quizId,
                        @Valid @RequestBody QuizLearningOutcomeOperationRequest request) {

                AddQuizLearningOutcomesCommand command = AddQuizLearningOutcomesCommand.builder()
                                .courseId(courseId)
                                .quizId(quizId)
                                .learningOutcomeIds(request.getLearningOutcomeIds())
                                .groupLearningOutcomeIds(request.getGroupLearningOutcomeIds())
                                .build();

                QuizLearningOutcomeOperationResponse response = addQuizLearningOutcomesUseCase
                                .addLearningOutcomes(command);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Remove learning outcomes from a quiz", description = "Removes learning outcomes from a quiz using direct outcome IDs and/or group IDs. LMS logic: expands group IDs and deduplicates removal set. Path fields: courseId, quizId. Request fields: learningOutcomeIds[], groupLearningOutcomeIds[]. Response fields: quizId, courseId, operation, totalAffected, learningOutcomeIds[].")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Learning outcomes removed successfully", content = @Content(schema = @Schema(implementation = QuizLearningOutcomeOperationResponse.class)))
        })
        @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @DeleteMapping("/{quizId}/learning-outcomes")
        public ResponseEntity<QuizLearningOutcomeOperationResponse> removeLearningOutcomes(
                        @PathVariable UUID courseId,
                        @PathVariable UUID quizId,
                        @Valid @RequestBody QuizLearningOutcomeOperationRequest request) {

                RemoveQuizLearningOutcomesCommand command = RemoveQuizLearningOutcomesCommand.builder()
                                .courseId(courseId)
                                .quizId(quizId)
                                .learningOutcomeIds(request.getLearningOutcomeIds())
                                .groupLearningOutcomeIds(request.getGroupLearningOutcomeIds())
                                .build();

                QuizLearningOutcomeOperationResponse response = removeQuizLearningOutcomesUseCase
                                .removeLearningOutcomes(command);
                return ResponseEntity.ok(response);
        }
}
