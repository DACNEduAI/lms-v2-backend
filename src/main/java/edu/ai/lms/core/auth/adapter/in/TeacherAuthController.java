package edu.ai.lms.core.auth.adapter.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.auth.adapter.in.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.adapter.in.response.TeacherSignInResponse;
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

    @Operation(summary = "Register a new teacher", description = "Create a new teacher account and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Teacher registered successfully",
                    content = @Content(schema = @Schema(implementation = TeacherRegisterResponse.class))),
    })
    @PostMapping("/register")
    public ResponseEntity<TeacherRegisterResponse> registerTeacher(@Valid @RequestBody TeacherRegisterRequest request) {
        TeacherRegisterResponse response = teacherRegisterUseCase.teacherRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Teacher login", description = "Authenticate teacher and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = TeacherSignInResponse.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<TeacherSignInResponse> loginTeacher(@Valid @RequestBody TeacherSignInRequest request) {
        TeacherSignInResponse response = teacherSignInUseCase.signIn(request);
        return ResponseEntity.ok(response);
    }
}
