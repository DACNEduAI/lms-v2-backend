package edu.ai.lms.core.auth.adapter.in.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.auth.adapter.in.http.mapper.StudentRegisterCommandMapper;
import edu.ai.lms.core.auth.adapter.in.http.mapper.StudentSignInCommandMapper;
import edu.ai.lms.core.auth.adapter.in.http.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.http.request.StudentSignInRequest;
import edu.ai.lms.core.auth.adapter.in.http.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.adapter.in.http.response.StudentSignInResponse;
import edu.ai.lms.core.auth.app.port.in.command.StudentRegisterCommand;
import edu.ai.lms.core.auth.app.port.in.command.StudentSignInCommand;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentRegisterUseCase;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentSignInUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/student")
@Tag(name = "Student Authentication", description = "Authentication endpoints for student registration and login")
@RequiredArgsConstructor
public class StudentAuthController {

    private final StudentRegisterUseCase studentRegisterUseCase;
    private final StudentSignInUseCase studentSignInUseCase;
    private final StudentRegisterCommandMapper studentRegisterCommandMapper;
    private final StudentSignInCommandMapper studentSignInCommandMapper;

        @Operation(
            summary = "Register a new student",
            description = """
                Registers a student account and returns a JWT token for immediate authenticated access.

                LMS business logic:
                - Creates a STUDENT identity used for course enrollment and quiz attempt flows.
                - Validates registration constraints before account creation.

                Request fields:
                - firstName: Student first name.
                - lastName: Student last name.
                - email: Unique student login email.
                - password: Student password (minimum length applies).
                - studentId: Institution student identifier.
                - enrollmentYear: Academic enrollment year.

                Response fields:
                - token: JWT access token.
                - type: Token type (Bearer).
                - email: Registered account email.
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student registered successfully",
                    content = @Content(schema = @Schema(implementation = StudentRegisterResponse.class))),
    })

    @PostMapping("/register")
    public ResponseEntity<StudentRegisterResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        StudentRegisterCommand command = studentRegisterCommandMapper.toCommand(request);
        StudentRegisterResponse response = studentRegisterUseCase.studentRegister(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @Operation(
            summary = "Student login",
            description = """
                Authenticates a student and returns token + identity profile details.

                LMS business logic:
                - Validates credentials against student account records.
                - Returns role context used by authorization checks on LMS APIs.

                Request fields:
                - email: Student login email.
                - password: Student login password.

                Response fields:
                - token: JWT access token.
                - type: Token type (Bearer).
                - email: Student email.
                - firstName: Student first name.
                - lastName: Student last name.
                - role: Security role for the authenticated user.
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = StudentSignInResponse.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<StudentSignInResponse> loginStudent(@Valid @RequestBody StudentSignInRequest request) {
        StudentSignInCommand command = studentSignInCommandMapper.toCommand(request);
        StudentSignInResponse response = studentSignInUseCase.signIn(command);
        return ResponseEntity.ok(response);
    }
}
