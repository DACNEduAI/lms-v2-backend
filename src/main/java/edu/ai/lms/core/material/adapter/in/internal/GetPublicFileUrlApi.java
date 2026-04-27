package edu.ai.lms.core.material.adapter.in.internal;

/**
 * Public API exposed by the Material module for resolving public file URLs.
 */
public interface GetPublicFileUrlApi {

    /**
     * Returns the public URL for the given file key.
     *
     * @param fileKey object key in storage
     * @return public URL
     */
    String getPublicFileUrl(String fileKey);
}
