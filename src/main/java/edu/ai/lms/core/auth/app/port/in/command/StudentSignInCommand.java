package edu.ai.lms.core.auth.app.port.in.command;

import lombok.Builder;

@Builder
public record StudentSignInCommand(String email, String password) {
}
