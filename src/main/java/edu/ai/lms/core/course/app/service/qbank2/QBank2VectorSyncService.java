package edu.ai.lms.core.course.app.service.qbank2;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

/** Synchronous AI vector boundary. Exceptions deliberately abort the enclosing QBank transaction. */
@Service
@RequiredArgsConstructor
public class QBank2VectorSyncService {
    private final WebClient aiWebClient;

    public void upsert(UUID courseId, QuestionBank2Service.GroupView group, QuestionBank2Service.ExampleView example) {
        aiWebClient.post().uri("/qbank2/examples/index").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new IndexRequest(courseId, group.id(), group.code(), group.name(), group.description(),
                        example.id(), example.sequence(), example.content()))
                .retrieve().toBodilessEntity().block(Duration.ofSeconds(60));
    }

    public void delete(UUID exampleId) {
        aiWebClient.post().uri("/qbank2/examples/delete").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DeleteRequest(exampleId)).retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    }

    public void upsertAll(UUID courseId, QuestionBank2Service.GroupView group) {
        for (QuestionBank2Service.ExampleView example : group.examples()) upsert(courseId, group, example);
    }

    public void deleteAll(List<QuestionBank2Service.ExampleView> examples) {
        for (QuestionBank2Service.ExampleView example : examples) delete(example.id());
    }

    private record IndexRequest(UUID courseId, UUID contentGroupId, String contentGroupCode, String contentGroupName,
            String contentGroupDescription, UUID exampleId, int sequence, String content) { }
    private record DeleteRequest(UUID exampleId) { }
}
