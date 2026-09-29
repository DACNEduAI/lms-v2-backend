# LMS Platform Project Structure

This document is the structural guide for the LMS backend. It explains what each
directory owns, how code is expected to flow through the application, where new
code belongs, and which boundaries must not be crossed.

The repository is a Java/Spring Boot modular monolith. Business capabilities are
split into Spring Modulith modules, and each substantial module uses hexagonal
architecture (ports and adapters). The application is deployed as one process and
uses one PostgreSQL database, but modules communicate through explicit APIs,
ports, and events rather than by reaching into one another's implementation.

## 1. Technology and architectural baseline

| Concern | Current choice |
|---|---|
| Language/runtime | Java 25, as configured by `pom.xml`, the Docker image, and CI |
| Application framework | Spring Boot 3.5.6 |
| Module boundaries | Spring Modulith 1.4.7 |
| HTTP API | Spring MVC and Jakarta Bean Validation |
| Authentication | Stateless Spring Security with JWT (`jjwt` 0.12.6) |
| Persistence | PostgreSQL, Spring JDBC, and MyBatis 3.0.4 |
| Scheduling | Spring scheduling plus JDBC-backed Quartz |
| Realtime transport | Server-Sent Events and STOMP over SockJS WebSocket |
| Object storage | AWS SDK v2 against LocalStack or DigitalOcean Spaces |
| External AI calls | Spring `WebClient` |
| API documentation | SpringDoc OpenAPI/Swagger UI |
| Tests | JUnit 5, Mockito, Spring Boot Test, REST Assured, Testcontainers |
| Build/deployment | Maven wrapper, Docker, GitHub Actions, DigitalOcean |

`pom.xml` is authoritative for compiler settings. At the time of writing its
Javadoc plugin still declares source level 21 while the project compiler, CI, and
container use Java 25. Keep these settings aligned when changing Java versions.

## 2. Repository at a glance

```text
lms-v2/
├── .github/
│   └── workflows/maven.yml          # Build, test, report, image, and deploy pipeline
├── .mvn/wrapper/
│   └── maven-wrapper.properties     # Maven wrapper distribution configuration
├── .postman/config.json             # Local Postman workspace metadata
├── .vscode/launch.json              # Local IDE launch configuration
├── docs/
│   └── erd/                         # Generated database/ER diagrams
├── scripts/
│   ├── generate-course-exam-matrix-chen-erd.mjs
│   └── generate-schema-chen-erd.mjs # Diagram generation utilities
├── src/
│   ├── main/
│   │   ├── java/edu/ai/lms/         # Application and production code
│   │   └── resources/               # Configuration, SQL, and static resources
│   └── test/java/edu/ai/lms/        # Unit, integration, and architecture tests
├── storage/                          # LocalStack state; local/generated, not source
├── target/                           # Maven output; generated, not source
├── .gitattributes
├── .gitignore
├── CLAUDE.md                         # Additional coding and assistant guidance
├── Dockerfile                        # Multi-stage production image
├── docker-compose.yml                # LocalStack development service
├── init-s3.sh                        # LocalStack bucket/CORS initialization
├── mvnw / mvnw.cmd                   # Unix/Windows Maven wrappers
├── pom.xml                           # Dependencies, resources, and build lifecycle
└── PROJECT_STRUCTURE.md              # This guide
```

IDE metadata (`.idea`, `.vscode`) and local runtime output (`target`, `storage`)
are not part of the application architecture. Do not make application behavior
depend on them.

## 3. Production source layout

```text
src/main/java/edu/ai/lms/
├── LmsApplication.java
├── common/
│   ├── api/
│   └── event/
├── config/
│   └── mybatis/
└── core/
    ├── attempt/
    ├── auth/
    ├── chat/
    ├── course/
    ├── history/
    ├── material/
    ├── notification/
    ├── quiz/
    └── user/
```

### 3.1 Application entry point

`LmsApplication.java` is the root Spring Boot configuration. Its package is above
all application packages, so component scanning covers `common`, `config`, and
`core`.

It enables:

- Spring Boot auto-configuration;
- scheduled methods through `@EnableScheduling`;
- asynchronous execution through `@EnableAsync`;
- fully qualified Spring bean names to prevent collisions between modules that
  deliberately use names such as `CurrentActorAdapter` or similarly named
  repositories.

The process contains all modules. A module is a logical and enforced boundary,
not a separately deployed service.

### 3.2 `common`: shared contracts only

`common` is itself a Spring Modulith application module.

```text
common/
├── api/
│   ├── ErrorResponse.java
│   ├── Roles.java
│   └── package-info.java             # Named interface "api"
├── event/
│   ├── SystemActivityEvent.java
│   ├── SystemNotificationEvent.java
│   └── package-info.java             # Named interface "events"
└── package-info.java                 # Declares the common module
```

Use `common.api` for small, stable types that genuinely have the same meaning
throughout the application. Use `common.event` for cross-cutting event contracts.
Do not turn `common` into a miscellaneous utility folder or place
module-specific domain concepts in it.

### 3.3 `config`: process-wide technical configuration

```text
config/
├── AppDateFormatProperties.java      # Binds app.format.*
├── DatePatternConstants.java         # Shared date-pattern defaults/constants
├── JacksonDateFormatConfig.java      # LocalDate/LocalDateTime JSON formatting
├── OpenApiConfig.java                # OpenAPI metadata, bearer scheme, role notes
├── WebClientConfig.java              # AI-service WebClient and response timeout
└── mybatis/
    └── UuidTypeHandler.java          # PostgreSQL/MyBatis UUID conversion
```

Only configuration that is truly application-wide belongs here. Module-specific
configuration stays inside its module—for example security in `auth`, WebSocket
configuration in `chat`, storage profiles in `material`, and Quartz job
configuration beside the owning feature.

## 4. Module anatomy: the default placement rule

Most `core/<module>` packages use the following shape:

```text
core/<module>/
├── package-info.java                 # @ApplicationModule + allowed dependencies
├── api/                              # Public cross-module contract, if needed
│   └── package-info.java             # @NamedInterface("api")
├── events/                           # Public events owned by this module, if needed
│   └── package-info.java             # @NamedInterface("events")
├── app/
│   ├── domain/                       # Business model and domain behavior
│   ├── exception/                    # Business/application exceptions
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/              # Transport-independent input values
│   │   │   └── usecase/              # Operations exposed by the application
│   │   └── out/                      # Capabilities the application requires
│   └── service/                      # Use-case implementations/orchestration
│       └── <capability>/             # Optional grouping for service-heavy modules
└── adapter/
    ├── in/
    │   ├── http/ or controllers      # REST and SSE entry points
    │   ├── websocket/                # STOMP entry points
    │   ├── event/                    # Spring event consumers
    │   ├── scheduler/                # Quartz/Spring scheduled entry points
    │   ├── api/                      # Implementations of public module APIs
    │   ├── mapper/                   # Request-to-command translation
    │   ├── request/                  # Transport request DTOs and validation
    │   └── response/                 # Transport response DTOs
    └── out/
        ├── persistence/              # Port adapters and MyBatis repositories
        │   └── mapper/               # MyBatis XML statements
        ├── integration/              # Adapters calling another module's API
        ├── security/                 # Authentication infrastructure
        ├── storage/                  # Object-storage infrastructure
        ├── scheduler/                # Quartz scheduling implementations
        └── exception/                # HTTP exception translation
```

Not every module needs every directory. Create a package only when the capability
exists; do not add empty layers for symmetry.

When a module has many services, split `app.service` by cohesive business
capability rather than leaving a large flat package. The course module is the
reference structure: `course`, `chapter`, `enrollment`, `exammatrix`,
`learningoutcome`, and `grouplearningoutcome`. Smaller modules may keep a flat
service package.

### 4.1 Dependency direction

The intended direction is:

```text
HTTP / WebSocket / event / scheduled input
                    |
                    v
             input use-case port
                    |
                    v
             application service
              /             \
             v               v
       domain model     output port
                              |
                              v
                   persistence / integration /
                   security / storage adapter
```

Rules:

1. `app.domain` contains business state and behavior. It must not import another
   `core` module or depend on controllers, repositories, HTTP, or storage.
2. `app.service` implements input use cases and depends inward on domain types and
   outward only through ports. It must not import another `core` module.
3. `app.port.in` is the application-facing contract. HTTP DTOs must not leak into
   it; map requests to commands first.
4. `app.port.out` describes what the use case needs, not how the need is met.
5. `adapter.in` converts external input and invokes a use case. It must not contain
   core business decisions or query persistence directly.
6. `adapter.out` implements output ports and owns framework-specific details.
7. A module's private packages are not a cross-module API.

These rules are checked by `PackageStructureTest`, in addition to Spring
Modulith's own `ApplicationModules.verify()`.

### 4.2 Request-to-response flow

The normal synchronous REST flow is:

```text
request DTO + Jakarta validation
  -> controller
  -> input mapper
  -> command
  -> use-case interface
  -> service
  -> domain behavior and output ports
  -> outbound adapter
  -> MyBatis / another module API / S3 / external AI service
  -> response DTO
  -> ResponseEntity
```

The response DTO is currently often built by the service. Preserve the convention
used by the surrounding feature unless a module-wide refactor deliberately
changes it.

### 4.3 Cross-module communication

There are two supported mechanisms:

- **Synchronous APIs:** the provider publishes interfaces and views in a named
  `api` package. An adapter in the consuming module calls that API to satisfy a
  consumer-owned output port. This prevents the consumer's application layer
  from importing the provider.
- **Events:** the publisher emits a contract from `common.event` or a provider's
  named `events` package. Consumers handle it in `adapter.in.event`. Current
  Spring events are in-process. Persistent Spring Modulith event publication is
  disabled by configuration, so do not assume message-broker durability.

Never solve cross-module access by importing another module's domain, service,
repository, request, or response package. Never query another module's tables
from a repository.

## 5. Spring Modulith dependency map

Each module declares its legal dependencies in its root `package-info.java`.

| Module | Allowed dependencies |
|---|---|
| `common` | None declared |
| `user` | `common :: api` |
| `auth` | `common :: api`, `core.user :: api` |
| `course` | `common :: api`, `common :: events`, `core.auth :: api`, `core.material :: api`, `core.user :: api` |
| `material` | `common :: api`, `common :: events`, `core.auth :: api` |
| `quiz` | `common :: api`, `common :: events`, `core.auth :: api`, `core.course :: api`, `core.material :: api` |
| `attempt` | `common :: api`, `core.auth :: api`, `core.course :: api`, `core.quiz :: api` |
| `chat` | `common :: api`, `core.auth :: api`, `core.material :: api` |
| `history` | `common :: events`, `core.auth :: api` |
| `notification` | `common :: events`, `core.auth :: api`, `core.course :: api`, `core.material :: events`, `core.quiz :: events` |

When a new dependency is legitimate, first define the smallest stable named
interface in the provider, then add the exact `module :: interface` declaration.
Do not broadly expose a package to make verification pass.

## 6. Business modules

### 6.1 `user`: identity data owner

`user` owns the canonical login and user-directory records. Unlike the larger
modules, it is intentionally thin: its public interfaces are implemented directly
by JDBC adapters.

- Public contracts: `LoginAccountApi`, `UserDirectoryApi`,
  `UserRegistrationApi`, registration records, and user summary/views.
- Outbound implementations: `JdbcLoginAccountAdapter` and
  `JdbcUserDirectoryAdapter`.
- Owned tables: `users`, `students`, `teachers`.
- Consumers: primarily `auth` for registration/login and `course` for user lookup.

Keep password/account storage and canonical user lookup here. Authentication
workflow and JWT concerns belong to `auth`, not `user`.

### 6.2 `auth`: authentication and current principal

`auth` handles student/teacher registration and sign-in, JWT creation and
validation, and conversion of the Spring Security principal into a stable
cross-module API.

- Inbound REST: `StudentAuthController` and `TeacherAuthController` under
  `/api/auth/student` and `/api/auth/teacher`.
- Application services: separate register and sign-in services for students and
  teachers.
- Public API: `CurrentPrincipalApi`, `TokenAuthenticationApi`, and
  `AuthenticatedPrincipal`, implemented by `AuthenticationApiAdapter`.
- Outbound integration: student/teacher account adapters call `user` APIs.
- Security: `SecurityConfig`, normal and SSE JWT filters, `JwtUtil`, and
  `SecurityUserPrincipal`.
- Data ownership: no authentication tables; account data is owned by `user`.

The security chain is stateless. Authentication, the signed quiz-generation
webhook, Swagger, actuator, errors, the chat test page, and WebSocket handshakes
are publicly reachable at the filter-chain level. Other endpoints require JWT,
with fine-grained authorization expressed through method security.

The current `PasswordEncoder` is `NoOpPasswordEncoder`. Treat this as a
security-sensitive implementation detail and do not propagate plaintext-password
assumptions into new code.

### 6.3 `course`: courses and academic structure

`course` is the largest curriculum module. It owns:

- courses and settings;
- chapters;
- enrollments;
- learning outcomes and their hierarchy/grouping;
- chapter-to-learning-outcome relationships;
- exam matrices and rows;
- course/quiz chapter associations used as course reference data.

Its REST entry points are `CourseController`, `ChapterController`,
`CourseEnrollmentController`, `LearningOutcomeController`,
`GroupLearningOutcomeController`, and `ExamMatrixController`.

Course application services are grouped by the aggregate/capability they
orchestrate:

```text
core/course/app/service/
├── course/                    # Course CRUD and settings
├── chapter/                   # Chapter CRUD and chapter/outcome associations
├── enrollment/                # Add, list, and remove course students
├── exammatrix/                # Exam-matrix CRUD and queries
├── learningoutcome/           # Outcome CRUD, batch updates, and tree ordering
└── grouplearningoutcome/      # Outcome-group CRUD and membership
```

Tests mirror these subpackages under
`src/test/java/edu/ai/lms/core/course/app/service`.

Its public APIs expose course access, learning-outcome catalog data, and quiz
generation context. Outbound integration adapters obtain the current actor from
`auth`, users from `user`, and material cleanup through `material`.

Course services publish activity and notification events for important changes.
Keep course membership checks and curriculum truth in this module; consumers
should request views through the course API.

Owned tables: `courses`, `chapters`, `enrollments`,
`group_learning_outcomes`, `learning_outcomes`,
`chapter_learning_outcomes`, `exam_matrices`, `exam_matrix_rows`, and
`quiz_chapters`.

### 6.4 `material`: metadata, upload, storage, and indexing

`material` owns material metadata and the lifecycle of files attached to courses
or chapters.

- REST entry point: `MaterialController` under `/api/{courseId}/material`.
- Public APIs: material selection/eligibility, course material cleanup, public
  file URL lookup, and storage upload.
- Storage adapter: `S3StorageAdapter`.
- Storage profiles: `DevConfig` for LocalStack and `DigitalOceanConfig` for
  production Spaces.
- Background work: stale upload cleanup, indexing jobs, and a recovery job.
- External integration: AI ingestion uses `/api/ingest`; deletion uses
  `/api/ingest/{courseId}/{materialId}` on the configured AI service.
- Owned tables: `materials`, `material_index_jobs`, and `chapter_materials`.

The upload path is deliberately direct-to-object-storage:

1. Create metadata and a pre-signed upload URL.
2. The client uploads to object storage.
3. The client confirms the upload.
4. The service verifies the object, marks the material uploaded, publishes
   activity/material events, and queues indexing.
5. Quartz performs/retries AI ingestion; recovery scans run every 30 seconds.

Material status and ingestion status are separate concepts. `Material` tracks the
upload lifecycle, while `MaterialIndexJob` tracks `INGEST`/`DELETE` work through
`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, or `CANCELLED`.

### 6.5 `quiz3`: quizzes, questions, answers, and AI generation

`quiz3` owns AI assessment authoring, generation, review, variants, and publication.

- REST entry points include Quiz3 assessment, question, variant, and internal
  webhook controllers.
- Generation persists `quiz3_generation_jobs`, dispatches to `/quiz3/generate`,
  and receives signed completion and progress callbacks at
  `/api/internal/webhooks/quiz3-generation`.
- Generated quiz rows remain in shared `quizzes`, `questions`, and `answers`;
  Quiz3-specific ownership and provenance use `quiz3_*` tables.
- Question-AI editing uses the dedicated internal AI specialist endpoints.

Quiz3 validates the published Matrix snapshot and selected materials before
dispatch, persists exact generation quotas, and recovers stale generation leases.

### 6.6 `attempt`: taking and scoring quizzes

`attempt` owns a student's execution of a quiz.

- REST entry point: `AttemptController` under `/api/{courseId}/attempt`.
- Use cases: start attempt, submit attempt, and get the current student's
  attempts.
- Domain: `Attempt`, submitted answers, answer keys, current actor, and quiz
  attempt policy.
- Integrations: current principal from `auth`, course access from `course`, and
  attempt policy/answer keys from `quiz`.
- Background work: one Quartz auto-close job per due attempt.
- Owned table: `attempts`.

Starting an attempt loads policy, validates access and attempt limits, persists
the attempt, and schedules auto-close. Submission validates state/time, scores
against answer-key views, persists the result, and unschedules the close job.
The scheduled input adapter closes overdue attempts through the application
service rather than manipulating persistence itself.

### 6.7 `chat`: direct chat and course AI chat

`chat` has two related transports:

- teacher/student messaging over STOMP plus REST history;
- course-aware AI chat sessions and streamed AI responses over HTTP/SSE.

Entry points:

- `TeacherStudentChatHistoryController` at `/api/chat`;
- `ChatController` at application destination `/app/chat.send`;
- `AiChatController` at `/api/course/{courseId}/ai-chat`.

The WebSocket handshake is `/ws` in development and `/wss` in production.
SockJS is enabled. The simple broker uses `/topic` and `/queue`, application
destinations use `/app`, and user destinations use `/user`. Authentication is
performed by `WebSocketAuthInterceptor`.

AI chat calls `/api/chat` on the configured AI service and validates selected
materials through the `material` public API.

Owned tables: `user_chat_messages`, `ai_chat_sessions`, and
`ai_course_chat_messages`.

### 6.8 `history`: activity audit

`history` consumes `SystemActivityEvent` and exposes the signed-in user's
paginated history at `/api/history/me`.

- Domain: `ActivityLog`.
- Input: Spring event listeners and `HistoryController`.
- Integration: current principal from `auth`.
- Owned table: `activity_logs`.

History is a consumer of events. Feature modules should publish the shared event
rather than write activity rows or invoke history repositories.

### 6.9 `notification`: inbox, announcements, and live delivery

`notification` owns persistent notifications, course announcements, and
in-memory SSE delivery.

- REST/SSE entry point: `NotificationController` at `/api/notifications`.
- Use cases: notification inbox/read operations and course announcements.
- Event input: `SystemNotificationEvent`,
  `MaterialUploadConfirmedEvent`, and `QuizDeadlineApproachingEvent`.
- Integrations: current principal from `auth` and course students from `course`.
- Owned tables: `notifications` and `course_announcements`.

Publishers emit events; this module resolves recipients, persists notifications,
and pushes them to connected SSE clients. SSE is a live delivery mechanism, while
the database is the durable inbox.

## 7. Persistence and schema ownership

### 7.1 Database namespaces

`schema.sql` creates the `edu` PostgreSQL schema and sets the search path to it.
Application tables are referenced as `edu.<table>` in MyBatis mappings.

`quartz_schema_init.sql` creates a separate `quartz_schema` namespace containing
the standard `qrtz_*` tables. Quartz uses the configured
`quartz_schema.qrtz_` table prefix and supports clustering when enabled.

### 7.2 Table ownership summary

| Owner | Tables |
|---|---|
| `user` | `users`, `students`, `teachers` |
| `course` | `courses`, `chapters`, `enrollments`, `group_learning_outcomes`, `learning_outcomes`, `chapter_learning_outcomes`, `exam_matrices`, `exam_matrix_rows`, `quiz_chapters` |
| `material` | `materials`, `material_index_jobs`, `chapter_materials` |
| `quiz` | `quizzes`, `quiz_generation_jobs`, `questions`, `question_images`, `answers`, `quiz_learning_outcomes`, `question_learning_outcomes` |
| `attempt` | `attempts` |
| `history` | `activity_logs` |
| `notification` | `notifications`, `course_announcements` |
| `chat` | `user_chat_messages`, `ai_chat_sessions`, `ai_course_chat_messages` |
| Spring infrastructure | `event_publication` and `quartz_schema.qrtz_*` |

Foreign keys may point across ownership boundaries, but SQL access still belongs
to the owning module. Ask the owner through an API when another module needs the
data.

### 7.3 MyBatis organization

A normal persistence slice is:

```text
app/port/out/LoadThingPort.java
app/port/out/SaveThingPort.java
adapter/out/persistence/ThingPersistenceAdapter.java
adapter/out/persistence/ThingRepository.java
adapter/out/persistence/mapper/ThingRepositoryMapper.xml
```

The adapter translates the application's port into repository calls. Repository
interfaces and XML are implementation details and should generally remain
package-private where practical.

Mapper XML is intentionally colocated with Java source. `pom.xml` adds matching
`**/adapter/out/persistence/mapper/*Mapper.xml` files from `src/main/java` as
resources, and `application.properties` configures the corresponding classpath
glob. If an XML mapper is moved or renamed, update its namespace, repository
method, and result mappings together.

MyBatis globally maps underscore column names to camel-case properties and uses
the custom UUID type-handler package. Explicit result maps are still preferable
for joins, nested objects, aliases, or nontrivial constructors.

### 7.4 Schema change policy

At present, Liquibase is included but disabled. `schema.sql` is the effective
application schema definition and `data.sql` is development seed data.

For every persistence change:

1. Update `schema.sql`, including constraints and indexes.
2. Update the owning repository XML/interface and domain mapping.
3. Update `data.sql` if seed rows depend on the changed shape.
4. Update table ownership enforcement in `PackageStructureTest` when a new owned
   table should be protected.
5. Add or update repository/integration tests.
6. Update generated diagrams when the relationship is architecturally relevant.

Do not rely on a developer database's manually altered state.

## 8. Resources and configuration

```text
src/main/resources/
├── application.properties           # Shared defaults and property wiring
├── application-dev.properties       # Development profile
├── application-prod.properties      # Production profile
├── .env.dev / .env.prod             # Environment templates; not auto-loaded by Spring
├── schema.sql                        # Application schema
├── data.sql                          # Development seed data
├── quartz_schema_init.sql            # Quartz PostgreSQL schema
└── static/chat-test.html             # Manual WebSocket chat client
```

Spring activates `${PROFILE:dev}`. The `.env.*` files are templates/convenience
files; Java properties do not automatically import dotenv files. Supply variables
through the shell, IDE, container platform, or a local configuration mechanism.
Never commit real credentials.

### 8.1 Required base environment

| Variable | Purpose |
|---|---|
| `PROFILE` | `dev` or `prod`; defaults to `dev` |
| `DATA_SOURCE` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials |
| `DATABASE_INITIALIZATION_OPTION` | Spring SQL init mode, normally `always` for disposable development data and `never` for managed production data |
| `JWT_KEY` | JWT signing secret |
| `JWT_EXPIRATION` | Token lifetime in milliseconds |

### 8.2 Optional/tunable environment

| Variable | Default/purpose |
|---|---|
| `AI_SERVICE_URL` | AI service base URL; defaults to `http://localhost:8000` |
| `AI_SERVICE_TOKEN` | Optional bearer token sent to the AI service |
| `AI_SERVICE_RESPONSE_TIMEOUT_SECONDS` | AI HTTP response timeout; default 900 |
| `QUIZ_WEBHOOK_BASE_URL` | Public base URL used to build AI callbacks |
| `QUIZ_WEBHOOK_SECRET` | HMAC secret for quiz-generation callbacks |
| `QUIZ_GENERATION_TIMEOUT_MINUTES` | Generation lease/timeout; default 15 |
| `QUIZ_GENERATION_MAX_ATTEMPTS` | Retry limit; default 3 |
| `QUIZ_GENERATION_RETENTION_DAYS` | Completed job retention; default 30 |
| `QUIZ_GENERATION_SSE_HEARTBEAT_MS` | Generation-stream heartbeat; default 15000 |
| `MATERIAL_INDEXING_TIMEOUT_MINUTES` | Indexing lease/timeout; default 15 |
| `MATERIAL_INDEXING_MAX_ATTEMPTS` | Indexing retry limit; default 3 |
| `QUARTZ_AUTO_STARTUP` | Start Quartz; default true |
| `QUARTZ_CLUSTERED` | Enable Quartz cluster coordination; default false |
| `QUARTZ_THREAD_COUNT` | Quartz worker count; default 5 |

Production storage binds `REGION`, `DO_STORAGE_END_POINT`,
`DO_STORAGE_ACCESS_KEY`, `DO_STORAGE_SECRET_KEY`, and `DO_STORAGE_BUCKET`.
Development storage settings are provided by `application-dev.properties`.

Some deployment build-argument names in `Dockerfile` and `.github/workflows`
currently use the shorter `DO_END_POINT`, `DO_ACCESS_KEY`, `DO_SECRET_KEY`, and
`DO_BUCKET` forms, while `application-prod.properties` reads the
`DO_STORAGE_*` forms. Treat `application-prod.properties` as the runtime binding
contract and keep the Docker/CI names synchronized before relying on a new
deployment.

Date formats are centralized under `app.format.local-date` and
`app.format.local-date-time`; DTOs should reuse the shared constants/configuration
instead of inventing per-endpoint formats.

## 9. HTTP, realtime, and security structure

All business HTTP APIs live under `/api`. Authentication uses
`Authorization: Bearer <token>` except for special transports:

- the SSE JWT filter supports the project's SSE authentication path;
- WebSocket authentication occurs at STOMP connect time;
- the AI generation webhook is filter-chain public but validates its HMAC inside
  the controller.

Role rules belong on controller methods through `@PreAuthorize`. OpenAPI appends
the expression to endpoint documentation.

Important development endpoints:

- Swagger UI: `/swagger-ui.html`;
- OpenAPI document: `/v3/api-docs`;
- WebSocket/SockJS: `/ws` (`/wss` in production);
- static chat client: `/chat-test.html`;
- actuator: `/actuator/**`.

Controllers should use resource-appropriate HTTP methods, validate request DTOs,
and return deliberate status codes. Business exceptions belong in
`app.exception`; module-scoped `@RestControllerAdvice` classes translate them
into the shared `ErrorResponse`.

## 10. Background and asynchronous execution

There are four distinct mechanisms; choose deliberately:

| Mechanism | Use |
|---|---|
| Spring application events | Immediate in-process decoupling between modules |
| `@Scheduled` | Simple recurring process-local tasks, such as stale upload cleanup or SSE heartbeat |
| Quartz one-shot jobs | Durable work keyed by a business job/attempt ID |
| Quartz recurring jobs | Durable recovery scans and deadline reminders |

Quartz jobs are inbound adapters. They must call application services/use cases
and must not implement business state transitions directly. Persist business-job
state in the owning application table; Quartz trigger state alone is not a
business audit trail.

Current scheduled work includes:

- attempt auto-close at an attempt's due time;
- material stale-upload cleanup every five minutes;
- material indexing and a 30-second recovery scan;
- quiz generation and a 30-second recovery scan;
- quiz deadline reminders hourly;
- quiz-generation SSE heartbeat.

The app enables both scheduling and async execution globally. Avoid adding
unbounded executors, silent fire-and-forget work, or duplicate scheduling when a
durable job already exists.

## 11. Test structure and lifecycle

Tests mirror production package ownership:

```text
src/test/java/edu/ai/lms/
├── LmsApplicationTests.java
├── PackageStructureTest.java
├── config/
│   └── IntegrationTestConfiguration.java
├── util/
│   └── TestHelper.java
└── core/<module>/
    ├── app/service/**/*UT.java
    ├── adapter/in/**/*IT.java
    └── adapter/out/persistence/*Test.java
```

### 11.1 Unit tests

Service unit tests end in `UT.java`, live under `app/service`, use Mockito, and
mock output ports. Maven Surefire is explicitly configured to run service `UT`
patterns plus `PackageStructureTest`.

```bash
./mvnw test
```

Because Surefire uses an explicit include list, a normally named `*Test.java`
outside those includes may compile but not run in this phase. Update the plugin
configuration deliberately if adding a new test category.

### 11.2 Integration tests

Controller integration tests end in `IT.java` and live under `adapter/in`.
Failsafe runs them during `verify`.

```bash
./mvnw verify
```

`IntegrationTestConfiguration`:

- starts the full application on a random port;
- activates the development profile;
- uses a singleton PostgreSQL 18 Testcontainer;
- initializes `schema.sql` and disables seed data;
- disables Liquibase, Spring Boot Admin client, Quartz auto-start, and persistent
  Modulith event publication;
- configures REST Assured before each test.

Tests that depend on a background Quartz trigger must explicitly enable or invoke
the relevant behavior; the shared integration base disables automatic startup.

### 11.3 Architecture tests

`PackageStructureTest` is a required guardrail. It:

1. asks Spring Modulith to verify module dependencies and cycles;
2. forbids imports from another `core` module in `app/domain` and `app/service`;
3. scans Java and XML persistence code for cross-owner SQL table access.

Run it whenever adding a module, public API, table, or cross-module integration.

### 11.4 Persistence tests outside configured patterns

Several repository tests use names such as `*Test.java` and some adapter tests do
not end in `IT`. They are useful source-level tests, but the current explicit
Surefire/Failsafe include patterns do not select every such class automatically.
When adding or relying on one, verify its Maven phase or run it explicitly with
`-Dtest=ClassName`.

## 12. Build, local runtime, and deployment

### 12.1 Common commands

```bash
# Compile/package without tests
./mvnw clean package -DskipTests

# Unit and architecture tests selected by Surefire
./mvnw test

# Full Maven verification, including selected integration tests
./mvnw verify

# Start LocalStack and initialize the lms-materials bucket
docker compose up -d

# Run the application after exporting required environment variables
./mvnw spring-boot:run
```

A local PostgreSQL instance is still required when running the application
outside integration tests. `docker-compose.yml` currently provides LocalStack
only.

### 12.2 Docker

The Dockerfile:

1. builds with Maven 3.9 and Temurin Java 25;
2. runs `mvn clean package -DskipTests`;
3. copies the executable JAR into a Temurin Java 25 JRE Alpine image;
4. exposes port 8080 and launches `java -jar app.jar`.

Do not copy source-only mapper XML outside Maven's resource pipeline; the build
already packages the colocated mappers.

### 12.3 CI/CD

`.github/workflows/maven.yml` runs on pushes and pull requests to `main`.

- `build` packages with JDK 25.
- `test` runs unit tests and `verify`, then publishes Surefire/Failsafe reports.
- `deploy` runs only for a successful push to `main`, builds and pushes a
  DigitalOcean Container Registry image, and deploys the DigitalOcean app.

The integration base starts its own PostgreSQL Testcontainer even though CI also
declares a PostgreSQL service. When changing test infrastructure, decide which
source is authoritative rather than accidentally maintaining both.

## 13. Documentation and generated artifacts

`docs/erd` contains the committed ERD output used for architectural discussion.
The JavaScript files in `scripts` generate SVG/PNG diagrams from the schema or a
focused course/exam-matrix model. Generated images belong in `docs`, not in
application source.

`target` contains compiled classes, reports, Javadocs, and packaged artifacts.
`storage` contains LocalStack state. Both are disposable runtime/build outputs
and must not be treated as source of truth.

## 14. Naming and implementation conventions

Use the established role-based names:

| Type | Pattern |
|---|---|
| Input use case | `<Action><Entity>UseCase` |
| Input command | `<Action><Entity>Command` |
| Service | `<Action><Entity>Service` |
| Controller | `<Entity>Controller` |
| Request/response | `<Action><Entity>Request` / `Response` |
| Input mapper | `<Action><Entity>CommandMapper` |
| Output port | `Load...Port`, `Save...Port`, `Delete...Port`, or capability name |
| Persistence adapter | `<Entity>PersistenceAdapter` |
| MyBatis interface | `<Entity>Repository` |
| Mapper XML | `<Entity>RepositoryMapper.xml` |
| Business exception | `<Entity><Reason>Exception` |
| HTTP advice | `<ModuleOrEntity>ExceptionHandler` |
| Unit test | `<Service>UT` |
| Integration test | `<Controller>IT` |

Use constructor injection (normally Lombok `@RequiredArgsConstructor`). Apply
`@Transactional` at service boundaries for state-changing use cases. Keep
transport validation in requests and business validation in domain/services.
Use records or ordinary DTO classes according to the surrounding package;
consistency within a feature matters more than forcing one representation.

Do not add JPA annotations or repositories: this project maps domain data through
MyBatis/JDBC. Do not place SQL in controllers or application services.

## 15. Where a new change belongs

Use this decision guide:

| Change | Location |
|---|---|
| New business rule or state | Owning module's `app/domain` |
| New user operation | `app/port/in/usecase`, command if needed, then `app/service` |
| New HTTP endpoint | `adapter/in/http` (or the module's existing `adapter/in` convention) |
| HTTP payload validation | `adapter/in/request` |
| Request-to-command conversion | `adapter/in/mapper` |
| New DB/object-storage/external need | Consumer-owned `app/port/out` |
| MyBatis implementation | `adapter/out/persistence` and its `mapper` XML |
| Call to another module | Consumer `adapter/out/integration` calling provider `api` |
| Contract exposed to modules | Provider `api` plus named-interface `package-info` |
| Cross-module fact/notification | Provider `events` or stable `common.event` |
| Event handler | Consumer `adapter/in/event` |
| Scheduled trigger | `adapter/in/scheduler`; scheduling mechanism in `adapter/out/scheduler` |
| Business-to-HTTP error mapping | Exception in `app/exception`, advice in `adapter/out/exception` |
| Global framework configuration | `config`; module-only configuration stays in the module |

### Example feature slice

For a new “publish course” operation:

1. Add `PublishCourseCommand` if the operation has input beyond a path ID.
2. Add `PublishCourseUseCase`.
3. Add `PublishCourseService`, working only with course domain and course-owned
   output ports.
4. Add a request/mapper only if an HTTP body exists.
5. Add the controller method and method-security rule.
6. Extend `CourseRepository` and its XML only if persistence changes.
7. Publish an event if history or notification must react; do not call their
   repositories.
8. Add a service UT, controller IT, and any persistence test.

## 16. Definition-of-done checklist for structural changes

Before considering a change complete:

- The code is in the module that owns the business concept.
- Domain and service code import no other `core` module.
- Cross-module access uses a named API or event and is declared in
  `package-info.java`.
- Repositories query only owned tables.
- Request DTOs validate transport constraints; services validate business rules.
- State-changing use cases have an intentional transaction boundary.
- New SQL is represented in `schema.sql`, mappings, indexes, and tests.
- Mapper XML still matches Maven resource and MyBatis location patterns.
- Security behavior is explicit (`@PreAuthorize`, public matcher, STOMP, SSE, or
  webhook HMAC as applicable).
- Scheduled work enters through a use case and has retry/idempotency behavior
  appropriate to its durability.
- Unit/integration test names match the Maven phase intended to run them.
- `./mvnw test` passes, and `./mvnw verify` is run when adapters or infrastructure
  change.
- `PROJECT_STRUCTURE.md`, other guidance, and diagrams are updated when the
  architectural contract changes.

This document describes the current repository contract. If implementation and
documentation diverge, update both in the same change and preserve the executable
checks as the final authority.
