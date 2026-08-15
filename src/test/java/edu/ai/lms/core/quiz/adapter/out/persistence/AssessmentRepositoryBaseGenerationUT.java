package edu.ai.lms.core.quiz.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class AssessmentRepositoryBaseGenerationUT {

    @Test
    void returnsNullWhenAssessmentHasNoBaseGenerationJob() throws Exception {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        AssessmentRepository repository = new AssessmentRepository(jdbc);

        when(jdbc.query(anyString(), anyMap(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<UUID> extractor = invocation.getArgument(2);
            return extractor.extractData(resultSet);
        });
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject(1, UUID.class)).thenReturn(null);

        assertThat(repository.baseGenerationJobId(UUID.randomUUID(), UUID.randomUUID())).isNull();
    }
}
