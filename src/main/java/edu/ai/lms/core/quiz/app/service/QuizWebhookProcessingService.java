package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizWebhookProcessingService {

    private final LoadQuizGenerationJobPort loadQuizGenerationJobPort;
    private final SaveQuizGenerationJobPort saveQuizGenerationJobPort;
    private final SaveQuizPort saveQuizPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    private final LoadChapterPort loadChapterPort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SseQuizGenerationService sseQuizGenerationService;

    @Transactional
    public QuizGenerationStatusResponse handleWebhook(QuizWebhookRequest request) {
        QuizGenerationJob job = loadQuizGenerationJobPort.getById(request.getJobId())
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));

        if (job.getStatus() == QuizGenerationStatus.COMPLETED || job.getStatus() == QuizGenerationStatus.FAILED) {
            return toStatusResponse(job);
        }

        QuizGenerationStatus incomingStatus = parseStatus(request.getStatus());
        if (incomingStatus == QuizGenerationStatus.FAILED) {
            job.setStatus(QuizGenerationStatus.FAILED);
            job.setErrorMessage(request.getErrorMessage());
            QuizGenerationJob failedJob = saveQuizGenerationJobPort.save(job);
            sseQuizGenerationService.pushStatus(job.getId(), toStatusResponse(failedJob));
            return toStatusResponse(failedJob);
        }

        try {
            Quiz savedQuiz = persistQuiz(job, request);
            job.setQuizId(savedQuiz.getId());
            job.setStatus(QuizGenerationStatus.COMPLETED);
            job.setErrorMessage(null);
            QuizGenerationJob completedJob = saveQuizGenerationJobPort.save(job);
            QuizGenerationStatusResponse status = toStatusResponse(completedJob);
            sseQuizGenerationService.pushStatus(job.getId(), status);
            return status;
        } catch (Exception ex) {
            log.error("Failed to persist quiz for job {}", job.getId(), ex);
            job.setStatus(QuizGenerationStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            QuizGenerationJob failedJob = saveQuizGenerationJobPort.save(job);
            sseQuizGenerationService.pushStatus(job.getId(), toStatusResponse(failedJob));
            return toStatusResponse(failedJob);
        }
    }

    private Quiz persistQuiz(QuizGenerationJob job, QuizWebhookRequest request) {
        QuizWebhookRequest.QuizPayload payload = request.getQuiz();
        if (payload == null) {
            throw new IllegalArgumentException("Quiz payload is missing");
        }

        Quiz quiz = Quiz.builder()
                .quizTitle(payload.getQuizTitle() != null ? payload.getQuizTitle() : job.getQuizTitle())
                .unlimited(true)
                .display(true)
                .courseId(job.getCourseId())
                .build();

        if (job.getRequestedRole() == Roles.TEACHER) {
            quiz.setTeacherId(job.getRequestedBy());
        } else {
            quiz.setStudentId(job.getRequestedBy());
        }

        Quiz savedQuiz = saveQuizPort.save(quiz);
        saveQuizLearningOutcomePort.addLearningOutcomes(savedQuiz.getId(), resolveLearningOutcomeIds(job));

        for (QuizWebhookRequest.QuestionPayload questionPayload : payload.getQuestions()) {
            Question.QuestionType type = questionPayload.getType() != null
                    ? questionPayload.getType()
                    : Question.QuestionType.MULTIPLE_CHOICE;

            Question question = Question.builder()
                    .content(questionPayload.getContent())
                    .type(type)
                    .quizId(savedQuiz.getId())
                    .build();

            Question savedQuestion = saveQuestionPort.save(question);

            for (QuizWebhookRequest.AnswerPayload answerPayload : questionPayload.getAnswers()) {
                Answer answer = Answer.builder()
                        .questionId(savedQuestion.getId())
                        .content(answerPayload.getContent())
                        .isCorrect(Boolean.TRUE.equals(answerPayload.getIsCorrect()))
                        .explanation(answerPayload.getExplanation())
                        .build();
                saveAnswerPort.save(answer);
            }
        }

        return savedQuiz;
    }

    private List<UUID> resolveLearningOutcomeIds(QuizGenerationJob job) {
        if (job.getChapterId() != null) {
            Chapter chapter = loadChapterPort.findByKey(job.getCourseId(), job.getChapterId())
                    .orElse(null);
            if (chapter != null && chapter.getLearningOutcomeIds() != null && !chapter.getLearningOutcomeIds().isEmpty()) {
                return chapter.getLearningOutcomeIds();
            }
        }

        return loadLearningOutcomePort.findByCourseId(job.getCourseId()).stream()
                .map(LearningOutcome::getId)
                .toList();
    }

    private QuizGenerationStatus parseStatus(String status) {
        if (status == null) {
            return QuizGenerationStatus.FAILED;
        }
        try {
            return QuizGenerationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return QuizGenerationStatus.FAILED;
        }
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
}
