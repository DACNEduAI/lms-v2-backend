package edu.ai.lms.core.auth.api;

public interface TokenAuthenticationApi {

    AuthenticatedPrincipal authenticate(String token);
}
