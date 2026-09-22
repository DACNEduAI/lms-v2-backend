package edu.ai.lms.core.quiz3.adapter.in.http.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
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
public class DeleteQuizzesRequest {

    @NotEmpty(message = "quizIds cannot be empty")
    private List<UUID> quizIds;
}
