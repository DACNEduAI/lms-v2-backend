package edu.ai.lms.core.course.app.service.ccct;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.usecase.ListCcctProfilesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCcctProfilesService implements ListCcctProfilesUseCase {
    private final CcctAccess access;
    private final LoadCcctProfilePort loadPort;

    @Override
    @Transactional(readOnly = true)
    public List<CcctProfile> list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        return loadPort.findAllByCourseId(courseId);
    }
}
