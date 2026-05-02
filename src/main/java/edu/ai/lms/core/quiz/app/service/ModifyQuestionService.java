package edu.ai.lms.core.quiz.app.service;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.DeleteAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.UploadQuestionImagePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionImagePublicUrlPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyQuestionService implements ModifyQuestionUseCase {

    private static final Pattern IMG_TAG_SRC_PATTERN = Pattern.compile(
            "(<img\\b[^>]*\\bsrc\\s*=\\s*['\"])([^'\">]+)(['\"][^>]*>)",
            Pattern.CASE_INSENSITIVE);

    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final LoadAnswerPort loadAnswerPort;
    private final DeleteAnswerPort deleteAnswerPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    private final UploadQuestionImagePort uploadQuestionImagePort;
    private final LoadQuestionImagePublicUrlPort loadQuestionImagePublicUrlPort;

    @Override
    @Transactional
    public ModifyQuestionResponse modifyQuestion(ModifyQuestionCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        Question existingQuestion = loadQuestionPort.getByIdWithoutAnswers(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + command.questionId()));

        if (!existingQuestion.getQuizId().equals(command.quizId())) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        Question updatedQuestion = command.question();
        updatedQuestion.setId(existingQuestion.getId());
        updatedQuestion.setQuizId(existingQuestion.getQuizId());

        // Xử lý ảnh nếu có
        processQuestionImages(updatedQuestion, command.quizId(), command.images());

        Question savedQuestion = saveQuestionPort.save(updatedQuestion);

        // --- Cập nhật Learning Outcome ---
        if (command.learningOutcomeId() != null) {
            saveQuestionLearningOutcomePort.removeLearningOutcomes(savedQuestion.getId(), null);
            saveQuestionLearningOutcomePort.addLearningOutcomes(savedQuestion.getId(), List.of(command.learningOutcomeId()));
        }

        List<Answer> existingAnswers = loadAnswerPort.getByQuestionId(existingQuestion.getId());
        List<ModifyQuestionResponse.AnswerSummary> answerSummaries = new ArrayList<>();

        if (updatedQuestion.getAnswers() != null) {
            existingAnswers.forEach(answer ->
                    deleteAnswerPort.deleteByKey(existingQuestion.getId(), answer.getAnswerNumber()));

            answerSummaries = updatedQuestion.getAnswers().stream()
                    .map(answer -> {
                        answer.setQuestionId(savedQuestion.getId());
                        Answer savedAnswer = saveAnswerPort.save(answer);
                        return ModifyQuestionResponse.AnswerSummary.builder()
                                .answerNumber(savedAnswer.getAnswerNumber())
                                .content(savedAnswer.getContent())
                                .correct(savedAnswer.isCorrect())
                                .explanation(savedAnswer.getExplanation())
                                .build();
                    })
                    .collect(Collectors.toList());
        } else {
            answerSummaries = existingAnswers.stream()
                    .map(answer -> ModifyQuestionResponse.AnswerSummary.builder()
                            .answerNumber(answer.getAnswerNumber())
                            .content(answer.getContent())
                            .correct(answer.isCorrect())
                            .explanation(answer.getExplanation())
                            .build())
                    .collect(Collectors.toList());
        }

        return ModifyQuestionResponse.builder()
                .questionId(savedQuestion.getId())
                .content(savedQuestion.getContent())
                .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                .quizId(savedQuestion.getQuizId())
                .answers(answerSummaries)
                .updatedAt(savedQuestion.getUpdatedAt())
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
                continue; // Trong trường hợp edit, ảnh có thể đã tồn tại trên server, không cần upload lại
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
            throw new IllegalArgumentException("User is not authorized to modify questions in this quiz");
        }
    }

    private record ImageUploadCandidate(String imageName, String fileKey, MultipartFile file) {
    }
}
