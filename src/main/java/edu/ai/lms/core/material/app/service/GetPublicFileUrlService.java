package edu.ai.lms.core.material.app.service;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.adapter.in.internal.GetPublicFileUrlApi;
import edu.ai.lms.core.material.app.port.out.storage.GetPublicFileUrlPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPublicFileUrlService implements GetPublicFileUrlApi {

    private final GetPublicFileUrlPort getPublicFileUrlPort;

    @Override
    public String getPublicFileUrl(String fileKey) {
        return getPublicFileUrlPort.getPublicFileUrl(fileKey);
    }
}
