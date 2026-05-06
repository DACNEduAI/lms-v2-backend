package edu.ai.lms.core.quiz.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationJobResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.InvalidQuizGenerationRequestException;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.GenerateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizGenerationJobUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizGenerationService implements GenerateQuizUseCase, GetQuizGenerationJobUseCase {

    private final SaveQuizGenerationJobPort saveQuizGenerationJobPort;
    private final LoadQuizGenerationJobPort loadQuizGenerationJobPort;
    private final ScheduleQuizGenerationJobPort scheduleQuizGenerationJobPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;
    private final SaveQuizGenerationJobLearningOutcomePort saveQuizGenerationJobLearningOutcomePort;

    @Override
    @Transactional
    public QuizGenerationJobResponse generateQuiz(GenerateQuizCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        verifyCourseMembership(user, command.courseId());

        boolean useOptions = Boolean.TRUE.equals(command.useOptions());
        if (useOptions && !StringUtils.hasText(command.prompt())) {
            if (!StringUtils.hasText(command.quizTitle())) {
                throw new InvalidQuizGenerationRequestException("quizTitle is required when prompt is empty");
            }
            if (command.learningOutcomeIds() == null || command.learningOutcomeIds().isEmpty()) {
                throw new InvalidQuizGenerationRequestException("learningOutcomeIds are required when prompt is empty");
            }
        }

        QuizGenerationJob job = QuizGenerationJob.builder()
                .courseId(command.courseId())
                .chapterId(command.chapterId())
                .quizTitle(command.quizTitle())
                .questionCount(command.questionCount())
                .questionType(command.questionType())
            .prompt(command.prompt())
            .useOptions(useOptions)
            .unlimited(command.unlimited())
            .timeLimitMinutes(command.timeLimitMinutes())
            .passScore(command.passScore())
            .startTime(command.startTime())
            .dueTime(command.dueTime())
            .attemptLimited(command.attemptLimited())
            .display(command.display())
                .requestedBy(user.getId())
                .requestedRole(user.getRole())
                .status(QuizGenerationStatus.PENDING)
                .build();

        QuizGenerationJob savedJob = saveQuizGenerationJobPort.save(job);
        if (command.learningOutcomeIds() != null && !command.learningOutcomeIds().isEmpty()) {
            saveQuizGenerationJobLearningOutcomePort.addLearningOutcomes(savedJob.getId(), command.learningOutcomeIds());
        }
        scheduleQuizGenerationJobPort.schedule(savedJob.getId());

        return QuizGenerationJobResponse.builder()
                .jobId(savedJob.getId())
                .status(savedJob.getStatus())
                .createdAt(savedJob.getCreatedAt())
                .build();
    }

    @Override
    public QuizGenerationStatusResponse getStatus(UUID jobId) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        QuizGenerationJob job = loadQuizGenerationJobPort.getById(jobId)
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));

        if (!job.getRequestedBy().equals(user.getId())) {
            throw new UserNotAuthorizedException("User is not allowed to access this job");
        }

        return toStatusResponse(job);
    }

    private QuizGenerationStatusResponse toStatusResponse(QuizGenerationJob job) {
        return QuizGenerationStatusResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .quizId(job.getQuizId())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private void verifyCourseMembership(UserAuth user, UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new UserNotAuthorizedException("User is not a member of this course");
        }
    }
}
