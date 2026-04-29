package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetQuestionService implements GetQuestionUseCase {

    private final LoadQuestionPort loadQuestionPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional(readOnly = true)
    public GetQuestionResponse getQuestion(UUID quizId, UUID questionId) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Load question WITH answers (sub-aggregate root loading)
        Question question = loadQuestionPort.getById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + questionId));

        if (!question.getQuizId().equals(quizId)) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        List<GetQuestionResponse.AnswerDto> answerDtos = question.getAnswers().stream()
                .map(a -> GetQuestionResponse.AnswerDto.builder()
                        .answerNumber(a.getAnswerNumber())
                        .content(a.getContent())
                        .isCorrect(a.isCorrect())
                        .explanation(a.getExplanation())
                        .build())
                .collect(Collectors.toList());

        return GetQuestionResponse.builder()
                .questionId(question.getId())
                .content(question.getContent())
                .type(question.getType() != null ? question.getType().name() : null)
                .quizId(question.getQuizId())
                .answers(answerDtos)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
