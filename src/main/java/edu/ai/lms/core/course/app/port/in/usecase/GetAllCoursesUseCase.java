package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.GetAllCoursesResponse;

public interface GetAllCoursesUseCase {
    GetAllCoursesResponse getAllCourses(int pageSize, String sortDirection);
}
