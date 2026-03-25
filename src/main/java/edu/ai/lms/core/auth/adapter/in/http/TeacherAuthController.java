package edu.ai.lms.core.auth.adapter.in.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.auth.adapter.in.http.mapper.TeacherRegisterCommandMapper;
import edu.ai.lms.core.auth.adapter.in.http.mapper.TeacherSignInCommandMapper;
import edu.ai.lms.core.auth.adapter.in.http.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.http.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.adapter.in.http.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.adapter.in.http.response.TeacherSignInResponse;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;
import edu.ai.lms.core.auth.app.port.in.command.TeacherSignInCommand;
import edu.ai.lms.core.auth.app.port.in.usecase.TeacherRegisterUseCase;
import edu.ai.lms.core.auth.app.port.in.usecase.TeacherSignInUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/teacher")
@Tag(name = "Teacher Authentication", description = "Authentication endpoints for teacher registration and login")
@RequiredArgsConstructor
public class TeacherAuthController {

    private final TeacherRegisterUseCase teacherRegisterUseCase;
    private final TeacherSignInUseCase teacherSignInUseCase;
    private final TeacherRegisterCommandMapper teacherRegisterCommandMapper;
    private final TeacherSignInCommandMapper teacherSignInCommandMapper;

        @Operation(
            summary = "Register a new teacher",
            description = """
                Registers a teacher account and returns a JWT token.

                LMS business logic:
                - Creates a TEACHER identity used for course ownership and content management flows.
                - Captures teacher profile metadata for institutional management.

                Request fields:
                - firstName: Teacher first name.
                - lastName: Teacher last name.
                - email: Teacher login email.
                - password: Teacher login password.
                - employeeId: Institution employee identifier.
                - phoneNumber: Optional contact number.
                - department: Optional department name.
                - specialization: Optional subject specialization.

                Response fields:
                - token: JWT access token.
                - type: Token type (Bearer).
                - email: Registered account email.
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Teacher registered successfully",
                    content = @Content(schema = @Schema(implementation = TeacherRegisterResponse.class))),
    })
    @PostMapping("/register")
    public ResponseEntity<TeacherRegisterResponse> registerTeacher(@Valid @RequestBody TeacherRegisterRequest request) {
        TeacherRegisterCommand command = teacherRegisterCommandMapper.toCommand(request);
        TeacherRegisterResponse response = teacherRegisterUseCase.teacherRegister(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @Operation(
            summary = "Teacher login",
            description = """
                Authenticates a teacher and returns token + identity details.

                LMS business logic:
                - Validates teacher credentials.
                - Returns TEACHER role context used to guard write endpoints in courses/quizzes/materials.

                Request fields:
                - email: Teacher login email.
                - password: Teacher login password.

                Response fields:
                - token: JWT access token.
                - type: Token type (Bearer).
                - email: Teacher email.
                - firstName: Teacher first name.
                - lastName: Teacher last name.
                - role: Security role for the authenticated user.
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = TeacherSignInResponse.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<TeacherSignInResponse> loginTeacher(@Valid @RequestBody TeacherSignInRequest request) {
        TeacherSignInCommand command = teacherSignInCommandMapper.toCommand(request);
        TeacherSignInResponse response = teacherSignInUseCase.signIn(command);
        return ResponseEntity.ok(response);
    }
}
