package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import edu.ai.lms.core.notification.app.port.out.CourseAnnouncementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseAnnouncementPersistenceAdapter implements CourseAnnouncementPort {

    private final CourseAnnouncementRepository repository;

    @Override
    public CourseAnnouncement save(CourseAnnouncement announcement) {
        if (announcement.getId() == null) {
            announcement.setId(UUID.randomUUID());
            repository.insert(announcement);
        }
        return announcement;
    }

    @Override
    public Page<CourseAnnouncement> findByCourseId(UUID courseId, Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<CourseAnnouncement> announcements = repository.findByCourseId(courseId, offset, limit);
        long total = repository.countByCourseId(courseId);
        
        return new PageImpl<>(announcements, pageable, total);
    }

    @Override
    public Optional<CourseAnnouncement> findById(UUID id) {
        return repository.findById(id);
    }
}
