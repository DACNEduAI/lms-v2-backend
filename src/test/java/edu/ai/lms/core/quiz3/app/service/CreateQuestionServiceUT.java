package edu.ai.lms.core.quiz3.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.quiz3.app.service.crudquiz.CreateQuestionService;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz3.app.domain.Answer;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.exception.InvalidLearningOutcomeException;
import edu.ai.lms.core.quiz3.app.exception.MissingLearningOutcomeException;
import edu.ai.lms.core.quiz3.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz3.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.UploadQuestionImagePort;
import edu.ai.lms.core.quiz3.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuestionLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class CreateQuestionServiceUT {

    @Mock
    private SaveQuestionPort saveQuestionPort;

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UploadQuestionImagePort uploadQuestionImagePort;

    @Mock
    private LoadQuestionImagePublicUrlPort loadQuestionImagePublicUrlPort;

    @Mock
    private LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;

    @Mock
    private SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;

    @InjectMocks
    private CreateQuestionService createQuestionService;

    private CurrentActor mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private UUID learningOutcomeId;
    private Quiz existingQuiz;
    private CreateQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        learningOutcomeId = UUID.randomUUID();

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(UUID.randomUUID())
                .teacherId(teacherId)
                .display(true)
                .build();

        Answer answer = Answer.builder()
                .content("Java is a programming language")
                .isCorrect(true)
                .explanation("Correct")
                .build();

        Question question = Question.builder()
                .content("What is Java?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .answers(new ArrayList<>(List.of(answer)))
                .build();

        validCommand = CreateQuestionCommand.builder()
                .quizId(quizId)
                .question(question)
                .learningOutcomeId(learningOutcomeId)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a question with answers")
    void testCreateQuestion_WithValidDetails_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(any(), any()))
                .thenReturn(List.of(learningOutcomeId));

        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        Answer savedAnswer = Answer.builder()
                .answerNumber(1L)
                .content("Java is a programming language")
                .isCorrect(true)
                .explanation("Correct")
                .questionId(questionId)
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        CreateQuestionResponse response = createQuestionService.createQuestion(validCommand);

        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals(1, response.getAnswers().size());
        assertEquals(1L, response.getAnswers().get(0).getAnswerNumber());
        verify(saveQuestionPort, times(1)).save(any(Question.class));
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
        verify(saveQuestionLearningOutcomePort, times(1)).addLearningOutcomes(eq(questionId), eq(List.of(learningOutcomeId)));
    }

    @Test
    @DisplayName("Should throw MissingLearningOutcomeException when learningOutcomeId is missing")
    void testCreateQuestion_WithMissingLearningOutcome_ShouldThrowException() {
        CreateQuestionCommand invalidCommand = CreateQuestionCommand.builder()
                .quizId(validCommand.quizId())
                .question(validCommand.question())
                .images(validCommand.images())
                .learningOutcomeId(null)
                .build();

        MissingLearningOutcomeException exception = assertThrows(MissingLearningOutcomeException.class,
                () -> createQuestionService.createQuestion(invalidCommand));

        assertEquals("Question must belong to exactly one learning outcome", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidLearningOutcomeException when learningOutcome doesn't belong to course")
    void testCreateQuestion_WithInvalidLearningOutcome_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(any(), any()))
                .thenReturn(List.of()); // Not found

        InvalidLearningOutcomeException exception = assertThrows(InvalidLearningOutcomeException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("The learning outcome does not belong to this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testCreateQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("User not authenticated", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testCreateQuestion_WithNonExistentQuiz_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testCreateQuestion_WithNonOwner_ShouldThrowException() {
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherTeacher = CurrentActor.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("User is not authorized to add questions to this quiz", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should set quizId on question before saving")
    void testCreateQuestion_ShouldSetQuizId() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(any(), any()))
                .thenReturn(List.of(learningOutcomeId));

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(Answer.builder().answerNumber(1L).build());

        createQuestionService.createQuestion(validCommand);

        verify(saveQuestionPort).save(questionCaptor.capture());
        Question capturedQuestion = questionCaptor.getValue();
        assertEquals(quizId, capturedQuestion.getQuizId());
    }

    @Test
    @DisplayName("Should upload question images and replace html src with public URLs")
    void testCreateQuestion_WithImagesInHtml_ShouldUploadAndReplaceContent() {
        String imageName = "equation:abc123.png";
        String originalContent = "<p>Solve equation</p><img src=\"" + imageName + "\" alt=\"eq\"/>";
        String publicUrl = "https://cdn.example.com/quiz/" + imageName;

        Question questionWithImage = Question.builder()
                .content(originalContent)
                .type(Question.QuestionType.ESSAY)
                .answers(new ArrayList<>())
                .build();

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                imageName,
                "image/png",
                "fake-image-content".getBytes());

        CreateQuestionCommand commandWithImage = CreateQuestionCommand.builder()
                .quizId(quizId)
                .question(questionWithImage)
                .learningOutcomeId(learningOutcomeId)
                .images(List.of(imageFile))
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(any(), any()))
                .thenReturn(List.of(learningOutcomeId));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionImagePublicUrlPort.getPublicImageUrl(anyString())).thenReturn(publicUrl);

        when(saveQuestionPort.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            return Question.builder()
                    .id(questionId)
                    .content(question.getContent())
                    .type(question.getType())
                    .quizId(question.getQuizId())
                    .images(question.getImages())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        CreateQuestionResponse response = createQuestionService.createQuestion(commandWithImage);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(saveQuestionPort).save(questionCaptor.capture());

        verify(uploadQuestionImagePort, times(1)).uploadQuestionImage(
                eq("quiz/" + quizId + "/question-images/" + imageName),
                any(byte[].class),
                eq("image/png"),
                eq(true));
        verify(loadQuestionImagePublicUrlPort, times(1)).getPublicImageUrl("quiz/" + quizId + "/question-images/" + imageName);
        verify(saveAnswerPort, never()).save(any(Answer.class));

        Question savedQuestion = questionCaptor.getValue();
        assertEquals("<p>Solve equation</p><img src=\"" + publicUrl + "\" alt=\"eq\"/>", savedQuestion.getContent());
        assertEquals(1, savedQuestion.getImages().size());
        assertEquals(imageName, savedQuestion.getImages().get(0).getImageName());
        assertEquals(publicUrl, savedQuestion.getImages().get(0).getUrl());

        assertEquals(savedQuestion.getContent(), response.getContent());
    }
}
