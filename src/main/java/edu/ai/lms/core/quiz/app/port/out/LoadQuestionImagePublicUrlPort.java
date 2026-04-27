package edu.ai.lms.core.quiz.app.port.out;

public interface LoadQuestionImagePublicUrlPort {

    /**
     * Returns the public URL for a previously uploaded question image.
     *
     * @param fileKey object key in storage
     * @return public URL
     */
    String getPublicImageUrl(String fileKey);
}
