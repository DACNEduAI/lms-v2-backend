package edu.ai.lms.core.quiz.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateQuizService implements CreateQuizUseCase {

    private final SaveQuizPort saveQuizPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;

    @Override
    @Transactional
    public CreateQuizResponse createQuiz(CreateQuizCommand command) {
        // Verify user is authenticated
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Verify user belongs to the course
        verifyCourseMembership(user, command.courseId());

        // Set quiz properties
        Quiz quiz = command.quiz();
        quiz.setCourseId(command.courseId());

        if (user.getRole() == Roles.TEACHER) {
            quiz.setTeacherId(user.getId());
        } else {
            quiz.setStudentId(user.getId());
        }

        // Save quiz
        Quiz savedQuiz = saveQuizPort.save(quiz);

        // Save nested questions and answers if present
        List<CreateQuizResponse.CreatedQuestion> createdQuestions = new ArrayList<>();

        if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            for (Question question : quiz.getQuestions()) {
                question.setQuizId(savedQuiz.getId());
                Question savedQuestion = saveQuestionPort.save(question);

                // Save nested answers if present
                List<CreateQuizResponse.CreatedAnswer> createdAnswers = new ArrayList<>();

                if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                    for (Answer answer : question.getAnswers()) {
                        answer.setQuestionId(savedQuestion.getId());
                        Answer savedAnswer = saveAnswerPort.save(answer);

                        createdAnswers.add(CreateQuizResponse.CreatedAnswer.builder()
                                .answerNumber(savedAnswer.getAnswerNumber())
                                .content(savedAnswer.getContent())
                                .isCorrect(savedAnswer.isCorrect())
                                .explanation(savedAnswer.getExplanation())
                                .build());
                    }
                }

                createdQuestions.add(CreateQuizResponse.CreatedQuestion.builder()
                        .questionId(savedQuestion.getId())
                        .content(savedQuestion.getContent())
                        .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                        .points(savedQuestion.getPoints())
                        .answers(createdAnswers)
                        .build());
            }
        }

        return CreateQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .quizTitle(savedQuiz.getQuizTitle())
                .timeLimitMinutes(savedQuiz.getTimeLimitMinutes())
                .passScore(savedQuiz.getPassScore())
                .courseId(savedQuiz.getCourseId())
                .active(savedQuiz.getActive())
                .questions(createdQuestions)
                .createdAt(savedQuiz.getCreatedAt())
                .build();
    }

    private void verifyCourseMembership(User user, java.util.UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
