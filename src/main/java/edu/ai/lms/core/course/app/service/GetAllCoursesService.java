package edu.ai.lms.core.course.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.GetAllCoursesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherGetAllCourses;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllCoursesService implements TeacherGetAllCourses {

    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional(readOnly = true)
    public GetAllCoursesResponse getAllCourses(int pageSize, String sortDirection) {
        // Validate teacher exists and get authenticated teacher
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Load all courses for the teacher
        List<Course> courses = loadCoursePort.findAllByTeacherId(teacher.getId());

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
}
