package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
}
