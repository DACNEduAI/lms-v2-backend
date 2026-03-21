package edu.ai.lms.core.course.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.GetAllCoursesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllCoursesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllCoursesService implements GetAllCoursesUseCase {

    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional(readOnly = true)
    public GetAllCoursesResponse getAllCourses(int pageSize, String sortDirection) {
        // Validate user exists and get authenticated user
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Select the appropriate strategy based on user's role
        LoadCourseStrategy strategy = selectStrategy(user.getRole());

        // Load all courses for the user using the selected strategy
        List<Course> courses = strategy.loadCourses(user.getId());

        // Sort courses by course name (first letter)
        Comparator<Course> comparator = Comparator.comparing(
                course -> course.getCourseName().toLowerCase().charAt(0));

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        // Apply sorting and pagination
        List<GetAllCoursesResponse.CourseSummary> courseSummaries = courses.stream()
                .sorted(comparator)
                .limit(pageSize)
                .map(course -> GetAllCoursesResponse.CourseSummary.builder()
                        .courseId(course.getId())
                        .courseName(course.getCourseName())
                        .courseCode(course.getCourseCode())
                        .description(course.getDescription())
                        .active(course.getActive())
                        .coverImage(course.getCoverImage())
                        .startDate(course.getStartDate())
                        .endDate(course.getEndDate())
                        .build())
                .collect(Collectors.toList());

        // Return response
        return GetAllCoursesResponse.builder()
                .courses(courseSummaries)
                .totalCourses(courses.size())
                .pageSize(pageSize)
                .sortDirection(sortDirection != null ? sortDirection : "asc")
                .build();
    }

    // Strategy Pattern - Inner interface for loading courses
    private interface LoadCourseStrategy {
        List<Course> loadCourses(UUID userId);
    }

    // Strategy Pattern - Teacher implementation
    private class TeacherLoadCourseStrategy implements LoadCourseStrategy {
        @Override
        public List<Course> loadCourses(UUID userId) {
            return loadCoursePort.findAllByTeacherId(userId);
        }
    }

    // Strategy Pattern - Student implementation
    private class StudentLoadCourseStrategy implements LoadCourseStrategy {
        @Override
        public List<Course> loadCourses(UUID userId) {
            return loadCoursePort.findAllByStudentId(userId);
        }
    }

    // Strategy Pattern - Select strategy based on role
    private LoadCourseStrategy selectStrategy(Roles role) {
        return switch (role) {
            case TEACHER -> new TeacherLoadCourseStrategy();
            case STUDENT -> new StudentLoadCourseStrategy();
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }
}
