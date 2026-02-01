package edu.ai.lms.core.auth.adapter.in.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegisterResponse {
    private String token;

    @Builder.Default
    private String type = "Bearer";
    private String email;
}

