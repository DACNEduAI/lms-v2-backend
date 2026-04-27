package edu.ai.lms.core.material.adapter.in;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.material.adapter.out.storage.config.StorageConfig;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Material Controller Integration Tests")
@Import(MaterialControllerIT.TestStorageConfig.class)
class MaterialControllerIT extends IntegrationTestConfiguration {

    private static final String BUCKET_NAME = "lms-materials";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Singleton LocalStack container – started once and reused across all tests
    protected static final LocalStackContainer localStack;
    private static final URI virtualHostEndpoint;

    static {
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(LocalStackContainer.Service.S3);
        localStack.start();

        // Build a virtual-hosted style endpoint.
        // *.localhost.localstack.cloud resolves to 127.0.0.1, so the SDK can
        // prepend the bucket name (e.g. lms-materials.s3.localhost.localstack.cloud).
        virtualHostEndpoint = URI.create(
                "http://s3.localhost.localstack.cloud:" + localStack.getMappedPort(4566));

        // Pre-create the S3 bucket so all tests can use it
        try (S3Client setupClient = S3Client.builder()
                .endpointOverride(virtualHostEndpoint)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build()) {
            setupClient.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        }
    }

    /**
     * Overrides the StorageConfig bean with a version pointing to the
     * LocalStack testcontainer, using virtual-hosted style URL resolution
     * (e.g. http://&lt;bucket&gt;.s3.localhost.localstack.cloud:&lt;port&gt;).
     *
     * S3StorageAdapter uses StorageConfig to obtain S3Client, S3Presigner,
     * and the bucket name, so we must override StorageConfig (not individual beans).
     */
    @TestConfiguration
    static class TestStorageConfig {

        @Bean
        @Primary
        public StorageConfig testStorageConfig() {
            return new StorageConfig() {
                @Override
                public S3Client s3Client() {
                    return S3Client.builder()
                            .endpointOverride(virtualHostEndpoint)
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                            .region(Region.of(localStack.getRegion()))
                            .build();
                }

                @Override
                public S3Presigner s3Presigner() {
                    return S3Presigner.builder()
                            .endpointOverride(virtualHostEndpoint)
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                            .region(Region.of(localStack.getRegion()))
                            .build();
                }

                @Override
                public String getBucketName() {
                    return BUCKET_NAME;
                }

                                @Override
                                public String getEndPoint() {
                                        return virtualHostEndpoint.toString();
                                }
            };
        }
    }

    @DynamicPropertySource
    static void configureStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("dev.storage.bucket-name", () -> BUCKET_NAME);
        registry.add("dev.storage.access-key", localStack::getAccessKey);
        registry.add("dev.storage.secret-key", localStack::getSecretKey);
        registry.add("dev.storage.end-point", virtualHostEndpoint::toString);
    }

    private static String teacherToken;
    private static UUID courseId;
    private static UUID materialId;
    private static String presignedUploadUrl;

    // ── Setup: register teacher & create course ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for material tests")
    void testRegisterTeacher() {
        String body = """
                {
                    "firstName": "Material",
                    "lastName": "Teacher",
                    "email": "material.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-MAT001",
                    "department": "Computer Science",
                    "specialization": "Software Engineering"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue())
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for material tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String body = """
                {
                    "courseName": "Material Test Course",
                    "description": "Course for material integration tests",
                    "courseCode": "MAT-IT-001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        courseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    // ── Full upload → confirm flow ──────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should create a PDF material and receive a presigned upload URL")
    void testCreateMaterial() {
        String body = """
                {
                    "title": "Introduction to Algorithms",
                    "type": "PDF",
                    "isDownloadable": true
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Introduction to Algorithms"))
                .body("type", equalTo("PDF"))
                .body("status", equalTo("PENDING"))
                .body("presignedUploadUrl", notNullValue())
                .body("courseId", equalTo(courseId.toString()))
                .extract().response();

        materialId = UUID.fromString(response.jsonPath().getString("id"));
        presignedUploadUrl = response.jsonPath().getString("presignedUploadUrl");
    }

    @Test
    @Order(4)
    @DisplayName("Should upload file to the presigned URL in LocalStack S3")
    void testUploadFileToPresignedUrl() {
        byte[] pdfContent = "%PDF-1.4 test content".getBytes();

        given()
                .urlEncodingEnabled(false)
                .contentType("application/pdf")
                .body(pdfContent)
                .when()
                .put(presignedUploadUrl)
                .then()
                .log().all()
                .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("Should confirm upload and update material status to UPLOADED")
    void testConfirmUpload() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + materialId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(200)
                .body("materialId", equalTo(materialId.toString()))
                .body("status", equalTo("UPLOADED"))
                .body("uploaded", equalTo(true));
    }

    // ── GET material ────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should get material with presigned download URL when downloadable")
    void testGetMaterialWithDownloadUrl() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/" + materialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(materialId.toString()))
                .body("title", equalTo("Introduction to Algorithms"))
                .body("type", equalTo("PDF"))
                .body("status", equalTo("UPLOADED"))
                .body("isDownloadable", equalTo(true))
                .body("presignedUrl", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 404 when material does not exist")
    void testGetNonExistentMaterial() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/" + nonExistentId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ── Modify material ─────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Should modify material title successfully")
    void testModifyMaterial() {
        String body = """
                {
                    "title": "Advanced Algorithms",
                    "isDownloadable": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .put("/api/" + courseId + "/material/" + materialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(materialId.toString()))
                .body("title", equalTo("Advanced Algorithms"))
                .body("isDownloadable", equalTo(false));
    }

    // ── Duplicate fileKey checks ────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Should reject creating material with duplicate fileKey (already uploaded)")
    void testCreateDuplicateMaterial() {
        // Same title + type -> same fileKey as the already-uploaded material
        String body = """
                {
                    "title": "Introduction to Algorithms",
                    "type": "PDF",
                    "isDownloadable": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(409);
    }

    // ── Auth / authorization checks ─────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Should fail to create material without authentication")
    void testCreateMaterialWithoutAuth() {
        String body = """
                {
                    "title": "Unauthorized Material",
                    "type": "PDF",
                    "isDownloadable": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to confirm upload for non-existent material")
    void testConfirmUploadNonExistentMaterial() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + nonExistentId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ── Delete material ─────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Should delete material successfully")
    void testDeleteMaterial() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + courseId + "/material/" + materialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("materialId", equalTo(materialId.toString()))
                .body("success", equalTo(true))
                .body("message", equalTo("Material deleted successfully"));
    }

    @Test
    @Order(13)
    @DisplayName("Should return 404 when deleting already-deleted material")
    void testDeleteNonExistentMaterial() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + courseId + "/material/" + materialId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ── VIDEO material full flow ────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("Should create, upload, and confirm a VIDEO material")
    void testVideoMaterialFullFlow() {
        // 1. Create VIDEO material
        String body = """
                {
                    "title": "Course Introduction Video",
                    "type": "VIDEO",
                    "isDownloadable": true
                }
                """;

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(201)
                .body("type", equalTo("VIDEO"))
                .body("status", equalTo("PENDING"))
                .body("presignedUploadUrl", notNullValue())
                .extract().response();

        String videoPresignedUrl = createResponse.jsonPath().getString("presignedUploadUrl");
        UUID videoMaterialId = UUID.fromString(createResponse.jsonPath().getString("id"));

        // 2. Upload video content to presigned URL
        byte[] videoContent = "fake video content for testing".getBytes();

        given()
                .urlEncodingEnabled(false)
                .contentType("video/mp4")
                .body(videoContent)
                .when()
                .put(videoPresignedUrl)
                .then()
                .log().all()
                .statusCode(200);

        // 3. Confirm upload
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + videoMaterialId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("UPLOADED"))
                .body("uploaded", equalTo(true));
    }

    // ── Confirm upload for file NOT in S3 ───────────────────────────────

    @Test
    @Order(15)
    @DisplayName("Should mark material as FAILED when file is not found in S3")
    void testConfirmUploadFileNotInS3() {
        // Create a material but do NOT upload the file
        String body = """
                {
                    "title": "Ghost Material",
                    "type": "PDF",
                    "isDownloadable": false
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        UUID ghostId = UUID.fromString(response.jsonPath().getString("id"));

        // Confirm upload without actually uploading → should mark as FAILED
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + ghostId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("uploaded", equalTo(false));
    }

    // ══════════════════════════════════════════════════════════════════════
    //                  CHAPTER MATERIAL TESTS
    // ══════════════════════════════════════════════════════════════════════

    private static UUID chapterId;
    private static UUID chapterMaterialId;
    private static String chapterPresignedUrl;

    @Test
    @Order(16)
    @DisplayName("Should create a chapter for chapter material tests")
    void testCreateChapter() {
        String body = """
                {
                    "title": "Chapter 1 - Introduction",
                    "description": "Introduction to the course material"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .extract().response();

        chapterId = UUID.fromString(response.jsonPath().getString("chapterId"));
    }

    @Test
    @Order(17)
    @DisplayName("Should create a PDF material for a specific chapter")
    void testCreateMaterialForChapter() {
        String body = """
                {
                    "title": "Chapter Notes",
                    "type": "PDF",
                    "isDownloadable": true
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Chapter Notes"))
                .body("type", equalTo("PDF"))
                .body("status", equalTo("PENDING"))
                .body("presignedUploadUrl", notNullValue())
                .body("courseId", equalTo(courseId.toString()))
                .extract().response();

        chapterMaterialId = UUID.fromString(response.jsonPath().getString("id"));
        chapterPresignedUrl = response.jsonPath().getString("presignedUploadUrl");
    }

    @Test
    @Order(18)
    @DisplayName("Should upload chapter material file to presigned URL")
    void testUploadChapterMaterialFile() {
        byte[] pdfContent = "%PDF-1.4 chapter content".getBytes();

        given()
                .urlEncodingEnabled(false)
                .contentType("application/pdf")
                .body(pdfContent)
                .when()
                .put(chapterPresignedUrl)
                .then()
                .log().all()
                .statusCode(200);
    }

    @Test
    @Order(19)
    @DisplayName("Should confirm chapter material upload successfully")
    void testConfirmChapterMaterialUpload() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + chapterMaterialId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(200)
                .body("materialId", equalTo(chapterMaterialId.toString()))
                .body("status", equalTo("UPLOADED"))
                .body("uploaded", equalTo(true));
    }

    @Test
    @Order(20)
    @DisplayName("Should get chapter material with presigned download URL")
    void testGetChapterMaterial() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/" + chapterMaterialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(chapterMaterialId.toString()))
                .body("title", equalTo("Chapter Notes"))
                .body("type", equalTo("PDF"))
                .body("status", equalTo("UPLOADED"))
                .body("isDownloadable", equalTo(true))
                .body("presignedUrl", notNullValue());
    }

    @Test
    @Order(21)
    @DisplayName("Should modify chapter material title")
    void testModifyChapterMaterial() {
        String body = """
                {
                    "title": "Updated Chapter Notes",
                    "isDownloadable": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .put("/api/" + courseId + "/material/" + chapterMaterialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(chapterMaterialId.toString()))
                .body("title", equalTo("Updated Chapter Notes"))
                .body("isDownloadable", equalTo(false));
    }

    @Test
    @Order(22)
    @DisplayName("Should create a VIDEO material for a chapter")
    void testCreateVideoMaterialForChapter() {
        String body = """
                {
                    "title": "Chapter Video Lecture",
                    "type": "VIDEO",
                    "isDownloadable": true
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(201)
                .body("type", equalTo("VIDEO"))
                .body("status", equalTo("PENDING"))
                .body("presignedUploadUrl", notNullValue())
                .extract().response();

        String videoPresignedUrl = response.jsonPath().getString("presignedUploadUrl");
        UUID videoMaterialId = UUID.fromString(response.jsonPath().getString("id"));

        // Upload video
        given()
                .urlEncodingEnabled(false)
                .contentType("video/mp4")
                .body("fake video content".getBytes())
                .when()
                .put(videoPresignedUrl)
                .then()
                .log().all()
                .statusCode(200);

        // Confirm upload
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .patch("/api/" + courseId + "/material/" + videoMaterialId + "/confirm-upload")
                .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("UPLOADED"));
    }

    @Test
    @Order(23)
    @DisplayName("Should fail to create chapter material without authentication")
    void testCreateChapterMaterialWithoutAuth() {
        String body = """
                {
                    "title": "Unauthorized Chapter Material",
                    "type": "PDF",
                    "isDownloadable": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(24)
    @DisplayName("Should delete chapter material successfully")
    void testDeleteChapterMaterial() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + courseId + "/material/" + chapterMaterialId)
                .then()
                .log().all()
                .statusCode(200)
                .body("materialId", equalTo(chapterMaterialId.toString()))
                .body("success", equalTo(true))
                .body("message", equalTo("Material deleted successfully"));
    }

    @Test
    @Order(25)
    @DisplayName("Should return 404 when getting deleted chapter material")
    void testGetDeletedChapterMaterial() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/" + chapterMaterialId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ══════════════════════════════════════════════════════════════════════
    //                  GET UPLOADED MATERIALS TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(26)
    @DisplayName("Should return all UPLOADED materials for the course without presigned URLs")
    void testGetUploadedMaterials() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(200)
                .body("$", notNullValue())
                // every item must have status UPLOADED
                .body("status.every { it == 'UPLOADED' }", equalTo(true))
                // presigned URLs must NOT be present in the list response
                .body("presignedUrl.every { it == null }", equalTo(true));
    }

    @Test
    @Order(27)
    @DisplayName("Should return at least one UPLOADED material with correct fields")
    void testGetUploadedMaterials_FieldsAreCorrect() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].type", notNullValue())
                .body("[0].status", equalTo("UPLOADED"))
                .body("[0].courseId", equalTo(courseId.toString()))
                .body("[0].presignedUrl", equalTo(null));
    }

    @Test
    @Order(28)
    @DisplayName("Should return 401/403 when getting uploaded materials without authentication")
    void testGetUploadedMaterials_WithoutAuth() {
        given()
                .when()
                .get("/api/" + courseId + "/material")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(29)
    @DisplayName("Should return empty list for a course with no uploaded materials")
    void testGetUploadedMaterials_EmptyCourse() {
        // Create a fresh course that will have no materials
        String startDate = java.time.LocalDate.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String endDate = java.time.LocalDate.now().plusMonths(3)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String courseBody = """
                {
                    "courseName": "Empty Material Course",
                    "description": "Course with no materials",
                    "courseCode": "MAT-EMPTY-001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        UUID emptyCourseId = UUID.fromString(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + teacherToken)
                        .body(courseBody)
                        .when()
                        .post("/api/course")
                        .then()
                        .statusCode(201)
                        .extract().jsonPath().getString("courseId"));

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + emptyCourseId + "/material")
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", equalTo(0));
    }

    // ══════════════════════════════════════════════════════
    //                  GET CHAPTER MATERIALS TESTS
    // ══════════════════════════════════════════════════════

    // chapterId is already set from the chapter material tests (Order 16)

    @Test
    @Order(30)
    @DisplayName("Should return all materials for a chapter including all statuses")
    void testGetChapterMaterials() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    @Order(31)
    @DisplayName("Should return correct fields for chapter materials (no presigned URL)")
    void testGetChapterMaterials_FieldsAreCorrect() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].type", notNullValue())
                .body("[0].courseId", equalTo(courseId.toString()))
                .body("[0].presignedUrl", equalTo(null));
    }

    @Test
    @Order(32)
    @DisplayName("Should return multiple materials for the chapter")
    void testGetChapterMaterials_ReturnsMultipleMaterials() {
        // Chapter already has at least 1 UPLOADED VIDEO from Order(22)
        // (chapter notes PDF was deleted in Order(24), video was confirmed uploaded)
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    @Order(33)
    @DisplayName("Should return empty list for a chapter with no materials")
    void testGetChapterMaterials_EmptyChapter() {
        // Create a second chapter with no materials
        UUID emptyChapterId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {
                            "title": "Empty Chapter",
                            "description": "A chapter with no materials"
                        }
                        """)
                .when()
                .post("/api/" + courseId + "/chapter")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("chapterId", java.util.UUID.class);

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/chapter/" + emptyChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", equalTo(0));
    }

    @Test
    @Order(34)
    @DisplayName("Should return 401/403 when getting chapter materials without authentication")
    void testGetChapterMaterials_WithoutAuth() {
        given()
                .when()
                .get("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(35)
    @DisplayName("Should also include PENDING materials in chapter list")
    void testGetChapterMaterials_IncludesPendingMaterial() {
        // Create a material for the chapter but do NOT confirm upload (status stays PENDING)
        String body = """
                {
                    "title": "Pending Chapter Material",
                    "type": "PDF",
                    "isDownloadable": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .statusCode(201);

        // The list endpoint should include the PENDING material too
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + courseId + "/material/chapter/" + chapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("status", org.hamcrest.Matchers.hasItem("PENDING"));
    }
}
