package edu.ai.lms.core.course.app.service.course;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.GetCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCourseService implements GetCourseUseCase {

    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetCourseResponse getCourse(UUID courseId) {
        // Validate course exists
        Course course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        // Validate authenticated user exists
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated user not found"));

        boolean teacherInCourse = course.getTeacherIds() != null && course.getTeacherIds().contains(user.getId());
        boolean studentInCourse = course.getStudentIds() != null && course.getStudentIds().contains(user.getId());

        // Business rule: only teachers/students participating in the course can view it
        if (!teacherInCourse && !studentInCourse) {
            throw new IllegalArgumentException("User is not authorized to access this course");
        }

        // Load group learning outcomes with their nested LOs
        List<GroupLearningOutcome> groups = loadGroupLearningOutcomePort.findByCourseId(courseId);

        List<GetCourseResponse.GroupLearningOutcomeDto> groupDtos = groups.stream()
                .map(group -> GetCourseResponse.GroupLearningOutcomeDto.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .code(group.getCode())
                        .sortOrder(group.getSortOrder())
                        .displayCode(group.getCode())
                        .description(group.getDescription())
                        .learningOutcomes(group.getLearningOutcomes().stream()
                                .map(lo -> GetCourseResponse.LearningOutcomeDto.builder()
                                        .id(lo.getId())
                                        .code(lo.getCode())
                                        .prefixNumber(lo.getPrefixNumber())
                                        .sortOrder(lo.getSortOrder())
                                        .displayCode(group.getCode() + "." + lo.getPrefixNumber())
                                        .description(lo.getDescription())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        // Ungrouped LOs: those without a group assignment
        List<GetCourseResponse.LearningOutcomeDto> ungroupedDtos = course.getLearningOutcomes().stream()
                .filter(lo -> lo.getGroupLearningOutcomeId() == null)
                .map(lo -> GetCourseResponse.LearningOutcomeDto.builder()
                        .id(lo.getId())
                        .code(lo.getCode())
                        .prefixNumber(lo.getPrefixNumber())
                        .sortOrder(lo.getSortOrder())
                        .displayCode(lo.getCode())
                        .description(lo.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Return response
        return GetCourseResponse.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .teacherId(course.getTeacherIds() != null && !course.getTeacherIds().isEmpty()
                        ? course.getTeacherIds().get(0)
                        : null)
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .groupLearningOutcomes(groupDtos)
                .ungroupedLearningOutcomes(ungroupedDtos)
                .build();
    }
}
