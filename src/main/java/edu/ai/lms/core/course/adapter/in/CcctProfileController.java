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

import edu.ai.lms.core.course.adapter.in.mapper.CcctProfileCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.CcctProfileResponseMapper;
import edu.ai.lms.core.course.adapter.in.request.CloneCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.request.PublishCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.request.UpdateCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.response.CcctProfileListResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctProfileResponse;
import edu.ai.lms.core.course.app.port.in.usecase.CloneCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.CreateCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.DeleteCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.ListCcctProfilesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.PublishCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.UpdateCcctProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/ccct-profiles")
@Tag(name = "CCCT Profiles", description = "Course Content Coverage Template lifecycle")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class CcctProfileController {
    private final CreateCcctProfileUseCase createUseCase;
    private final UpdateCcctProfileUseCase updateUseCase;
    private final PublishCcctProfileUseCase publishUseCase;
    private final CloneCcctProfileUseCase cloneUseCase;
    private final GetCcctProfileUseCase getUseCase;
    private final ListCcctProfilesUseCase listUseCase;
    private final DeleteCcctProfileUseCase deleteUseCase;
    private final CcctProfileCommandMapper commandMapper;
    private final CcctProfileResponseMapper responseMapper;

    @Operation(summary = "List CCCT profiles in a Course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profiles retrieved"),
            @ApiResponse(responseCode = "403", description = "Teacher does not manage the Course"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping
    public ResponseEntity<CcctProfileListResponse> list(@PathVariable UUID courseId) {
        return ResponseEntity.ok(responseMapper.toList(courseId, listUseCase.list(courseId)));
    }

    @Operation(summary = "Get the current published CCCT profile")
    @GetMapping("/published")
    public ResponseEntity<CcctProfileResponse> getPublished(@PathVariable UUID courseId) {
        return ResponseEntity.ok(responseMapper.toDetail(getUseCase.getPublished(courseId)));
    }

    @Operation(summary = "Get a CCCT profile with its complete tree")
    @GetMapping("/{profileId}")
    public ResponseEntity<CcctProfileResponse> get(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId) {
        return ResponseEntity.ok(responseMapper.toDetail(getUseCase.get(courseId, profileId)));
    }

    @Operation(summary = "Create a CCCT draft")
    @PostMapping
    public ResponseEntity<CcctProfileResponse> create(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateCcctProfileRequest request) {
        var profile = createUseCase.create(commandMapper.toCreateCommand(courseId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toDetail(profile));
    }

    @Operation(summary = "Replace a CCCT draft using optimistic locking")
    @PutMapping("/{profileId}")
    public ResponseEntity<CcctProfileResponse> update(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @Valid @RequestBody UpdateCcctProfileRequest request) {
        var profile = updateUseCase.update(
                commandMapper.toUpdateCommand(courseId, profileId, request));
        return ResponseEntity.ok(responseMapper.toDetail(profile));
    }

    @Operation(summary = "Publish a CCCT draft and archive the previous published profile")
    @PostMapping("/{profileId}/publish")
    public ResponseEntity<CcctProfileResponse> publish(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @Valid @RequestBody PublishCcctProfileRequest request) {
        var profile = publishUseCase.publish(
                commandMapper.toPublishCommand(courseId, profileId, request));
        return ResponseEntity.ok(responseMapper.toDetail(profile));
    }

    @Operation(summary = "Clone a CCCT profile into a new draft")
    @PostMapping("/{profileId}/clone")
    public ResponseEntity<CcctProfileResponse> cloneProfile(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId,
            @Valid @RequestBody CloneCcctProfileRequest request) {
        var profile = cloneUseCase.cloneProfile(
                commandMapper.toCloneCommand(courseId, profileId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toDetail(profile));
    }

    @Operation(summary = "Delete a CCCT draft")
    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @PathVariable UUID profileId) {
        deleteUseCase.delete(courseId, profileId);
        return ResponseEntity.noContent().build();
    }
}
