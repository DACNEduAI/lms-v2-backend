package edu.ai.lms.core.quiz3.app.service.quizworkflow;

import org.springframework.stereotype.Component;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz3.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz3.app.port.out.LoadCourseMembershipPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuizAccessPolicy {
    private final LoadCourseMembershipPort courseMembershipPort;

    public void requireCanManage(CurrentActor actor, Quiz quiz) {
        if (actor.role() == Roles.TEACHER
                && quiz.getTeacherId() != null
                && courseMembershipPort.isTeacherInCourse(actor.id(), quiz.getCourseId())) {
            return;
        }
        if (actor.role() == Roles.STUDENT && actor.id().equals(quiz.getStudentId())) {
            return;
        }
        throw new UserNotAuthorizedException("User cannot manage this quiz");
    }

    public void requireCourseTeacher(CurrentActor actor, Quiz quiz) {
        if (actor.role() != Roles.TEACHER
                || quiz.getTeacherId() == null
                || !courseMembershipPort.isTeacherInCourse(actor.id(), quiz.getCourseId())) {
            throw new UserNotAuthorizedException("Only a teacher assigned to this course can manage this quiz");
        }
    }

    public static void requireMutable(Quiz quiz) {
        QuizLifecycleStatus status = quiz.getLifecycleStatus();
        if (status != QuizLifecycleStatus.DRAFT && status != QuizLifecycleStatus.REVIEW_PENDING) {
            throw new QuizLifecycleConflictException(
                    "Quiz content is locked in status " + status + "; clone a Published quiz to revise it");
        }
    }
}
