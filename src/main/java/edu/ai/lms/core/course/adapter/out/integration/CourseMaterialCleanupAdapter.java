package edu.ai.lms.core.course.adapter.out.integration;

import edu.ai.lms.core.course.app.port.out.CleanupCourseMaterialsPort;
import edu.ai.lms.core.material.api.DeleteCourseMaterialsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class CourseMaterialCleanupAdapter implements CleanupCourseMaterialsPort {

    private final DeleteCourseMaterialsApi deleteCourseMaterialsApi;

    @Override
    public void deleteAllByCourseId(UUID courseId) {
        deleteCourseMaterialsApi.deleteAllMaterialsByCourseId(courseId);
    }
}
