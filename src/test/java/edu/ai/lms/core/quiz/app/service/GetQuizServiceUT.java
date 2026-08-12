package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ai.lms.core.quiz.app.service.crudquiz.GetQuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotVisible;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class GetQuizServiceUT {

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

        @Mock
        private LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;

        @Mock
        private LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @InjectMocks
    private GetQuizService getQuizService;

    private CurrentActor mockTeacher;
    private CurrentActor mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID quizId;
    private Quiz existingQuiz;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = CurrentActor.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        Answer answer = Answer.builder()
                .answerNumber(1L)
                .content("Sample Answer")
                .contentFormat(RichContentFormat.TIPTAP_HTML)
                .isCorrect(true)
                .explanation("Sample Explanation")
                .explanationFormat(RichContentFormat.TIPTAP_HTML)
                .questionId(UUID.randomUUID())
                .build();

        Question question = Question.builder()
                .id(UUID.randomUUID())
                .content("Sample Question")
                .contentFormat(RichContentFormat.TIPTAP_HTML)
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .quizId(quizId)
                .answers(new ArrayList<>(List.of(answer)))
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
                .questions(new ArrayList<>(List.of(question)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully get a quiz as the teacher owner")
    void testGetQuiz_AsTeacherOwner_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadCourseLearningOutcomePort.findQuizLearningOutcomes(courseId, quizId)).thenReturn(List.of(
                QuizLearningOutcomeView.builder()
                        .learningOutcomeId(UUID.randomUUID())
                        .learningOutcomeCode("CLO-1")
                        .learningOutcomeDescription("Group child")
                        .groupLearningOutcomeId(UUID.randomUUID())
                        .groupLearningOutcomeName("Group A")
                        .groupLearningOutcomeDescription("Group description")
                        .build(),
                QuizLearningOutcomeView.builder()
                        .learningOutcomeId(UUID.randomUUID())
                        .learningOutcomeCode("CLO-2")
                        .learningOutcomeDescription("Ungrouped child")
                        .build()));
        when(loadQuestionLearningOutcomePort.findLearningOutcomesByQuizId(quizId)).thenReturn(List.of());

        // Act
        GetQuizResponse response = getQuizService.getQuiz(courseId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals("Midterm Quiz", response.getQuizTitle());
        assertEquals(60, response.getTimeLimitMinutes());
        assertEquals(70.0, response.getPassScore());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getQuestions());
        assertEquals(1, response.getQuestions().size());
        assertEquals("Sample Question", response.getQuestions().get(0).getContent());
        assertEquals(RichContentFormat.TIPTAP_HTML, response.getQuestions().get(0).getContentFormat());
        assertEquals(1, response.getQuestions().get(0).getAnswers().size());
        assertEquals("Sample Answer", response.getQuestions().get(0).getAnswers().get(0).getContent());
        assertEquals(RichContentFormat.TIPTAP_HTML, response.getQuestions().get(0).getAnswers().get(0).getContentFormat());
        assertEquals(RichContentFormat.TIPTAP_HTML, response.getQuestions().get(0).getAnswers().get(0).getExplanationFormat());
        assertEquals(1, response.getGroupedLearningOutcomes().size());
        assertEquals(1, response.getUngroupedLearningOutcomes().size());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuizPort, times(1)).getByIdWithQuestionsAndAnswers(courseId, quizId);
        verify(loadCourseLearningOutcomePort, times(1)).findQuizLearningOutcomes(courseId, quizId);
    }

    @Test
        @DisplayName("Should successfully get a visible quiz as student in the same course")
        void testGetQuiz_AsStudent_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
                when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
                when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(existingQuiz));
                when(loadCourseLearningOutcomePort.findQuizLearningOutcomes(courseId, quizId)).thenReturn(List.of());
                when(loadQuestionLearningOutcomePort.findLearningOutcomesByQuizId(quizId)).thenReturn(List.of());

                GetQuizResponse response = getQuizService.getQuiz(courseId, quizId);

                assertNotNull(response);
                assertEquals(quizId, response.getQuizId());
                assertEquals("Midterm Quiz", response.getQuizTitle());
                verify(loadQuizPort, times(1)).getByIdWithQuestionsAndAnswers(courseId, quizId);
        }

        @Test
        @DisplayName("Should throw QuizNotVisible when student tries to view a hidden quiz")
        void testGetQuiz_AsStudent_WithHiddenQuiz_ShouldThrowQuizNotVisible() {
                Quiz hiddenQuiz = Quiz.builder()
                                .id(existingQuiz.getId())
                                .quizTitle(existingQuiz.getQuizTitle())
                                .timeLimitMinutes(existingQuiz.getTimeLimitMinutes())
                                .passScore(existingQuiz.getPassScore())
                                .courseId(existingQuiz.getCourseId())
                                .teacherId(existingQuiz.getTeacherId())
                                .display(false)
                                .questions(existingQuiz.getQuestions())
                                .createdAt(existingQuiz.getCreatedAt())
                                .updatedAt(existingQuiz.getUpdatedAt())
                                .build();

                when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
                when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
                when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(hiddenQuiz));

                QuizNotVisible exception = assertThrows(QuizNotVisible.class,
                () -> getQuizService.getQuiz(courseId, quizId));

                assertEquals("Quiz is not visible for student to view: " + quizId, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("User not authenticated", exception.getMessage());

        verify(loadQuizPort, never()).getByIdWithQuestionsAndAnswers(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not a member of the course")
    void testGetQuiz_WithNonMember_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("User is not a member of this course", exception.getMessage());

        verify(loadQuizPort, never()).getByIdWithQuestionsAndAnswers(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testGetQuiz_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
                when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());
    }

    @Test
    @DisplayName("Should return unlimited quiz metadata when time limit is zero and due time is absent")
    void testGetQuiz_WithUnlimitedQuiz_ShouldReturnZeroTimeLimitAndNullDueTime() {
        Quiz unlimitedQuiz = Quiz.builder()
                .id(existingQuiz.getId())
                .quizTitle(existingQuiz.getQuizTitle())
                .timeLimitMinutes(0)
                .passScore(existingQuiz.getPassScore())
                .courseId(existingQuiz.getCourseId())
                .teacherId(existingQuiz.getTeacherId())
                .display(true)
                .questions(existingQuiz.getQuestions())
                .dueTime(null)
                .createdAt(existingQuiz.getCreatedAt())
                .updatedAt(existingQuiz.getUpdatedAt())
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(unlimitedQuiz));
        when(loadCourseLearningOutcomePort.findQuizLearningOutcomes(courseId, quizId)).thenReturn(List.of());
        when(loadQuestionLearningOutcomePort.findLearningOutcomesByQuizId(quizId)).thenReturn(List.of());

        GetQuizResponse response = getQuizService.getQuiz(courseId, quizId);

        assertNotNull(response);
        assertEquals(0, response.getTimeLimitMinutes());
        assertNull(response.getDueTime());
    }
}
