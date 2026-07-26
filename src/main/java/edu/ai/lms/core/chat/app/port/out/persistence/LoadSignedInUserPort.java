package edu.ai.lms.core.chat.app.port.out.persistence;

import java.util.Optional;

import edu.ai.lms.core.chat.app.domain.CurrentActor;

public interface LoadSignedInUserPort {
    Optional<CurrentActor> getAuthenticatedUser();
}
