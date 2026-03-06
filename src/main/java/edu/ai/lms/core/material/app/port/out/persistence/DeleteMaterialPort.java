package edu.ai.lms.core.material.app.port.out.persistence;

import java.util.UUID;

public interface DeleteMaterialPort {

    void deleteById(UUID materialId);

    void deleteByCourseId(UUID courseId);
}
