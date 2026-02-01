package edu.ai.lms.core.auth.adapter.in;

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentRegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/student")
@Tag(name = "Student Authentication", description = "Authentication endpoints for student registration and login")
public class StudentAuthController {

    @Autowired
    private final StudentRegisterUseCase studentRegisterUseCase;
    public StudentAuthController(StudentRegisterUseCase studentRegisterUseCase) {
        this.studentRegisterUseCase = studentRegisterUseCase;
    }

//
    @Operation(summary = "Register a new student", description = "Create a new student account and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student registered successfully",
                    content = @Content(schema = @Schema(implementation = StudentRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists",
                    content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<StudentRegisterResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {

        studentRegisterUseCase.studentRegister(request);

//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(
//                        StudentRegisterResponse.builder().email()
        return null;
    }
//
//    @Operation(summary = "Student login", description = "Authenticate student and return JWT token")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Login successful",
//                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
//            @ApiResponse(responseCode = "401", description = "Invalid credentials",
//                    content = @Content)
//    })
//    @PostMapping("/login")
//    public ResponseEntity<AuthResponseDTO> loginStudent(@Valid @RequestBody LoginRequestDTO request) {
//        return ResponseEntity.ok(loginStudentUseCase.execute(request));
//    }

}
