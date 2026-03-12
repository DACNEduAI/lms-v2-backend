package edu.ai.lms.core.material.adapter.in.internal;

import java.util.UUID;

/**
 * Public API exposed by the Material module for deleting all materials belonging to a course.
 * The Course module should depend on this interface to clean up S3 files before deleting a course.
 */
public interface DeleteCourseMaterialsApi {

    /**
     * Deletes all S3 files and database records for materials belonging to the given course.
     *
     * @param courseId the course whose materials should be deleted
     */
    void deleteAllMaterialsByCourseId(UUID courseId);
}
