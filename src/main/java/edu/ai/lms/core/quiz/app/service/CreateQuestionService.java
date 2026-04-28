package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.common.Roles;
import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.UploadQuestionImagePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateQuestionService implements CreateQuestionUseCase {

    private static final Pattern IMG_TAG_SRC_PATTERN = Pattern.compile(
            "(<img\\b[^>]*\\bsrc\\s*=\\s*['\"])([^'\">]+)(['\"][^>]*>)",
            Pattern.CASE_INSENSITIVE);

    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final ApplicationEventPublisher eventPublisher;
    private final UploadQuestionImagePort uploadQuestionImagePort;
    private final LoadQuestionImagePublicUrlPort loadQuestionImagePublicUrlPort;

    @Override
    @Transactional
    public CreateQuestionResponse createQuestion(CreateQuestionCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        // set quizId in question (to know that which quiz this question belongs to)
        Question question = command.question();
        question.setQuizId(quiz.getId());

        processQuestionImages(question, command.quizId(), command.images());

        // save question to database
        Question savedQuestion = saveQuestionPort.save(question);
        List<CreateQuestionResponse.AnswerSummary> answerSummaries = new ArrayList<>();

        // return the question as a response
        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
            answerSummaries = question.getAnswers().stream()
                    .map(answer -> {
                        answer.setQuestionId(savedQuestion.getId());
                        Answer savedAnswer = saveAnswerPort.save(answer);
                        return CreateQuestionResponse.AnswerSummary.builder()
                                .answerNumber(savedAnswer.getAnswerNumber())
                                .content(savedAnswer.getContent())
                                .isCorrect(savedAnswer.isCorrect())
                                .explanation(savedAnswer.getExplanation())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // Publish activity event
        eventPublisher.publishEvent(SystemActivityEvent.builder()
                .userId(user.getId())
                .actionType("CREATE")
                .entityType("QUESTION")
                .entityId(savedQuestion.getId())
                .entityName("Câu hỏi trong " + quiz.getQuizTitle())
                .description("Giảng viên vừa thêm câu hỏi mới vào bài kiểm tra: " + quiz.getQuizTitle())
                .timestamp(LocalDateTime.now())
                .build());

        return CreateQuestionResponse.builder()
                .questionId(savedQuestion.getId())
                .content(savedQuestion.getContent())
                .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                .points(savedQuestion.getPoints())
                .quizId(savedQuestion.getQuizId())
                .answers(answerSummaries)
                .createdAt(savedQuestion.getCreatedAt())
                .build();
    }

    private void processQuestionImages(Question question, UUID quizId, List<MultipartFile> uploadedFiles) {
        if (question.getContent() == null || question.getContent().isBlank()) {
            return;
        }

        Map<String, MultipartFile> filesByName = mapFilesByName(uploadedFiles);
        Map<String, ImageUploadCandidate> candidatesByImageName = collectUploadCandidates(question.getContent(), quizId,
                filesByName);

        if (candidatesByImageName.isEmpty()) {
            return;
        }

        // Step 1: upload all images sequentially
        for (ImageUploadCandidate candidate : candidatesByImageName.values()) {
            try {
                uploadQuestionImagePort.uploadQuestionImage(
                        candidate.fileKey(),
                        candidate.file().getBytes(),
                        resolveContentType(candidate.file()),
                        true);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to read image bytes for: " + candidate.imageName(), ex);
            }
        }

        // Step 2: resolve public links after uploads are completed
        Map<String, String> publicUrlByImageName = new LinkedHashMap<>();
        List<Question.Image> uploadedImages = new ArrayList<>();

        for (ImageUploadCandidate candidate : candidatesByImageName.values()) {
            String publicUrl = loadQuestionImagePublicUrlPort.getPublicImageUrl(candidate.fileKey());
            publicUrlByImageName.put(candidate.imageName(), publicUrl);
            uploadedImages.add(Question.Image.builder()
                    .imageName(candidate.imageName())
                    .url(publicUrl)
                    .build());
        }

        question.setContent(rewriteImageSrcWithPublicUrls(question.getContent(), publicUrlByImageName));
        question.setImages(uploadedImages);
    }

    private Map<String, ImageUploadCandidate> collectUploadCandidates(String questionContent,
            UUID quizId,
            Map<String, MultipartFile> filesByName) {
        Map<String, ImageUploadCandidate> candidatesByImageName = new LinkedHashMap<>();
        Matcher matcher = IMG_TAG_SRC_PATTERN.matcher(questionContent);

        while (matcher.find()) {
            String src = matcher.group(2);
            String imageName = normalizeImageNameFromSrc(src);

            if (imageName == null || imageName.isBlank()) {
                continue;
            }

            if (candidatesByImageName.containsKey(imageName)) {
                continue;
            }

            MultipartFile matchingFile = filesByName.get(imageName);
            if (matchingFile == null) {
                throw new IllegalArgumentException("Missing uploaded file for question image: " + imageName);
            }

            candidatesByImageName.put(imageName, new ImageUploadCandidate(
                    imageName,
                    buildQuestionImageKey(quizId, imageName),
                    matchingFile));
        }

        return candidatesByImageName;
    }

    private String rewriteImageSrcWithPublicUrls(String content, Map<String, String> publicUrlByImageName) {
        Matcher matcher = IMG_TAG_SRC_PATTERN.matcher(content);
        StringBuffer rewritten = new StringBuffer();

        while (matcher.find()) {
            String src = matcher.group(2);
            String imageName = normalizeImageNameFromSrc(src);

            if (imageName == null || !publicUrlByImageName.containsKey(imageName)) {
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String replacement = matcher.group(1) + publicUrlByImageName.get(imageName) + matcher.group(3);
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    private String buildQuestionImageKey(UUID quizId, String imageName) {
        return "quiz/" + quizId + "/question-images/" + imageName;
    }

    private Map<String, MultipartFile> mapFilesByName(List<MultipartFile> files) {
        Map<String, MultipartFile> filesByName = new LinkedHashMap<>();

        if (files == null) {
            return filesByName;
        }

        for (MultipartFile file : files) {
            String normalizedName = normalizeFileName(file.getOriginalFilename());
            if (normalizedName != null && !normalizedName.isBlank()) {
                filesByName.put(normalizedName, file);
            }
        }

        return filesByName;
    }

    private String normalizeImageNameFromSrc(String src) {
        if (src == null || src.isBlank()) {
            return null;
        }

        String normalized = src.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("data:")) {
            return null;
        }

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }

        normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);

        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
            normalized = normalized.substring(slashIndex + 1);
        }

        return normalizeFileName(normalized);
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String normalized = fileName.trim().replace("\\\\", "/");
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
            normalized = normalized.substring(slashIndex + 1);
        }
        return normalized;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to add questions to this quiz");
        }
    }

    private record ImageUploadCandidate(String imageName, String fileKey, MultipartFile file) {
    }
}
