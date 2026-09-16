package edu.ai.lms.core.learnermodel.adapter.out.integration;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class LearnerActorAdapter implements LoadLearnerActorPort {
    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public UUID requireStudentId() {
        var principal = currentPrincipalApi.currentPrincipal()
                .orElseThrow(() -> new LearnerModelAccessDeniedException("User is not authenticated"));
        if (principal.role() != Roles.STUDENT) {
            throw new LearnerModelAccessDeniedException("Only students can request learner-model predictions");
        }
        return principal.userId();
    }
}
