# CLAUDE.md — LMS Platform Architecture & Prompt Guidelines

## Project Overview

**AI Learning Management System (LMS)** — A Spring Boot backend platform for managing courses, quizzes, materials, and student/teacher authentication. Built with **Hexagonal Architecture** (Clean Architecture) principles, using **Spring Modulith** for module encapsulation.

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.6
- **Database**: PostgreSQL (Spring Data JDBC — *not* JPA)
- **Auth**: JWT (jjwt 0.12.6), Spring Security
- **Storage**: AWS S3 SDK (DigitalOcean Spaces in prod, LocalStack in dev)
- **Build**: Maven (wrapper included: `./mvnw`)
- **API docs**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Modularity**: Spring Modulith 1.4.7
- **Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL + LocalStack), REST Assured

---

## Quick Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run unit tests only (UT suffix in app/service/)
./mvnw test

# Run integration tests only (IT suffix in adapter/in/)
./mvnw verify -DskipTests=false

# Start the app (dev profile)
PROFILE=dev \
DATA_SOURCE=jdbc:postgresql://localhost:5432/lms \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
JWT_KEY=<base64-secret> \
JWT_EXPIRATION=3600000 \
DATABASE_INITIALIZATION_OPTION=always \
./mvnw spring-boot:run

# Start LocalStack for S3 development
docker compose up -d
```

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The application follows **hexagonal architecture** with 4 layers:

```
┌─────────────────────────────────────────────────────────────┐
│                     ADAPTER LAYER                           │
│  ┌──────────────┐                    ┌───────────────────┐  │
│  │  adapter.in   │                    │   adapter.out      │  │
│  │  (HTTP REST)  │                    │   (Persistence,    │  │
│  │  Controllers  │                    │    Security,       │  │
│  │  Request/     │                    │    Storage,        │  │
│  │  Response/    │                    │    Exceptions)     │  │
│  │  Mapper       │                    │                    │  │
│  └──────┬───────┘                    └────────▲──────────┘  │
│         │                                      │             │
│─────────┼──────────────────────────────────────┼─────────────│
│         ▼           PORT LAYER                 │             │
│  ┌──────────────┐                    ┌─────────┴──────────┐  │
│  │  port.in      │                    │   port.out          │  │
│  │  (Use Case    │                    │   (Load/Save/       │  │
│  │   Interfaces, │                    │    Delete Ports)    │  │
│  │   Commands)   │                    │                     │  │
│  └──────┬───────┘                    └────────▲──────────┘  │
│         │                                      │             │
│─────────┼──────────────────────────────────────┼─────────────│
│         ▼        SERVICE LAYER                 │             │
│  ┌─────────────────────────────────────────────┴──────────┐  │
│  │                    app/service                          │  │
│  │  Each service implements ONE use case interface         │  │
│  │  and orchestrates domain logic via output ports         │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
│─────────────────────────────────────────────────────────────│
│                     DOMAIN LAYER                             │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    app/domain                            │  │
│  │  Pure Java + Lombok entities. No framework annotations.  │  │
│  │  Contains business logic methods on entities.            │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow (Request → Response)

```
HTTP Request
  → Controller (adapter.in)
    → Mapper converts Request → Command
      → Service (implements UseCase interface)
        → Domain entities + business logic
        → Output Ports (interfaces in app/port/out)
          → Persistence Adapter (adapter.out) → Database
          → Storage Adapter (adapter.out) → S3
        ← Response DTO built in service
      ← Response returned
    ← Controller wraps in ResponseEntity
  ← HTTP Response
```

---

## Directory Structure

```
src/main/java/edu/ai/lms/
├── LmsApplication.java             # Entry point (@SpringBootApplication)
├── common/
│   ├── ErrorResponse.java          # Shared error response DTO
│   └── Roles.java                  # Enum: ADMIN, TEACHER, STUDENT
├── config/
│   └── OpenApiConfig.java          # Swagger/OpenAPI configuration
├── database/                       # JDBC table entities (shared across modules)
│   ├── UserJdbcTable.java
│   ├── StudentJdbcTable.java
│   ├── TeacherJdbcTable.java
│   ├── CourseJdbcTable.java
│   ├── ChapterJdbcTable.java
│   ├── MaterialJdbcTable.java
│   ├── QuizJdbcTable.java
│   ├── QuestionJdbcTable.java
│   ├── AnswerJdbcTable.java
│   └── ... (junction tables, etc.)
└── core/                           # Business modules
    ├── auth/                       # Authentication module
    ├── course/                     # Course management module
    ├── material/                   # Material/file management module
    ├── quiz/                       # Quiz/question/answer module
    └── user/                       # User module (stub)
```

### Module Internal Structure

Every module under `core/` follows this exact structure:

```
core/<module>/
├── package-info.java               # @ApplicationModule (Spring Modulith)
├── app/
│   ├── domain/                     # Domain entities (pure Java + Lombok)
│   ├── exception/                  # Business logic exceptions
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/            # Command records (packaged request data)
│   │   │   └── usecase/            # Use case interfaces (input ports)
│   │   └── out/                    # Output port interfaces (Load/Save/Delete)
│   └── service/                    # Service implementations (1 service = 1 use case)
└── adapter/
    ├── in/
    │   ├── [http/]                 # REST Controllers
    │   │   ├── mapper/             # Request → Command mappers
    │   │   ├── request/            # Request DTOs (Jakarta validation)
    │   │   └── response/           # Response DTOs
    │   └── [internal/]             # Internal APIs exposed to other modules
    └── out/
        ├── exception/              # Exception handlers (@RestControllerAdvice)
        ├── persistence/            # DB adapters, repositories, JDBC mappers
        ├── [security/]             # Security adapters (auth module only)
        └── [storage/]              # S3 storage adapters (material module only)
```

---

## Core Modules

### Auth Module (`core/auth`)
- Student & Teacher registration and login
- JWT token generation and validation
- Spring Security configuration (stateless, JWT filter)
- Exposes `LoadAuthenticatedUserApi` as internal API for cross-module auth
- Domain: `User` (base, implements `UserDetails`), `Student`, `Teacher`

### Course Module (`core/course`)
- Full CRUD for courses, chapters, learning outcomes
- Course enrollment (add/remove students)
- Course settings management
- Domain: `Course` (aggregate root), `Chapter` (weak entity), `LearningOutcome`, `Student`, `Teacher`
- Course is the aggregate root; chapters and learning outcomes are children loaded through CourseRepository

### Material Module (`core/material`)
- File upload/download via pre-signed S3 URLs
- Material lifecycle: PENDING → UPLOADED → (or FAILED)
- Cleanup scheduler for stale materials (`MaterialCleanupScheduler`)
- Backend only generates pre-signed URLs; frontend handles actual file transfer
- Domain: `Material` with types `PDF`, `VIDEO`, `LINK`
- Storage: LocalStack (dev) / DigitalOcean Spaces (prod)

### Quiz Module (`core/quiz`)
- Quiz, Question, and Answer management
- Quiz is an independent entity (can be transferred between courses)
- Questions have types: `MULTIPLE_CHOICE`, `TRUE_FALSE`, `ESSAY`
- Answer is a weak entity (composite key: `answer_number` + `question_id`)
- Domain: `Quiz`, `Question`, `Answer`, `Student`, `Teacher`

---

## Database

### Technology
- **PostgreSQL** with **Spring Data JDBC** (not JPA/Hibernate)
- JDBC table entities live in `edu.ai.lms.database` package (shared)
- Domain entities live within each module's `app/domain` package
- Mappers in `adapter/out/persistence/mapper/` convert between JDBC and domain entities

### Schema Initialization
- `schema.sql` — Creates all tables (drops first for fresh state)
- `data.sql` — Development seed data only
- Control via `spring.sql.init.mode`: `"always"` to reinitialize, `"never"` to skip

### Key Design Decisions
- **Composite keys**: `Chapter` (`chapter_number`, `course_id`) and `Answer` (`answer_number`, `question_id`) use composite primary keys. Custom `@Query` methods replace default `save()` for these.
- **Aggregate roots**: `Course` loads its children (`Chapter`, `LearningOutcome`) through `@MappedCollection`
- **Relationship ownership**: In M-N relationships (Teacher↔Course, Student↔Course), the owning side holds the list of related objects; the other side only holds IDs
- **JDBC auditing**: `@CreatedDate` and `@LastModifiedDate` on JDBC table entities, enabled by `@EnableJdbcAuditing`
- **FullyQualifiedAnnotationBeanNameGenerator**: Used to avoid bean name conflicts between modules having same-named repositories

### Entity Relationship Summary
```
Users ──1:1──> Students
Users ──1:1──> Teachers
Courses ──M:N──> Teachers  (via course_teachers)
Courses ──M:N──> Students  (via course_students)
Courses ──1:N──> Chapters  (Chapter is weak entity of Course)
Courses ──1:N──> Materials (via course_id FK, SET NULL on delete)
Chapters ──M:N──> Materials (via chapter_materials junction)
Courses ──1:N──> LearningOutcomes
Courses ──1:N──> Quizzes
Quizzes ──1:N──> Questions
Questions ──1:N──> Answers  (Answer is weak entity of Question)
Students ──1:N──> Attempts
Quizzes ──1:N──> Attempts
```

---

## Coding Conventions

### Entity Annotations (Required on ALL entity-like classes)
Every class acting as domain entity, request body, response body, or JDBC table must have:
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder          // Use @SuperBuilder if inheritance exists
```

### Service Pattern
```java
@Service
@RequiredArgsConstructor
public class XxxService implements XxxUseCase {
    private final SomeOutputPort somePort;
    
    @Override
    @Transactional  // When write operations are involved
    public XxxResponse execute(XxxCommand command) {
        // 1. Extract domain objects from command
        // 2. Business validation
        // 3. Call output ports
        // 4. Build and return response
    }
}
```

### Controller Pattern
```java
@RestController
@RequestMapping("/api/<module>/<resource>")
@Tag(name = "...", description = "...")  // OpenAPI
@RequiredArgsConstructor
public class XxxController {
    private final XxxUseCase xxxUseCase;
    private final XxxCommandMapper xxxCommandMapper;
    
    @PostMapping
    public ResponseEntity<XxxResponse> create(@Valid @RequestBody XxxRequest request) {
        XxxCommand command = xxxCommandMapper.toCommand(request);
        XxxResponse response = xxxUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Command Pattern
```java
@Builder
public record XxxCommand(DomainEntity entity) {}
```

### Mapper Pattern
```java
@Component
public class XxxCommandMapper {
    public XxxCommand toCommand(XxxRequest request) {
        DomainEntity entity = DomainEntity.builder()
                .field1(request.getField1())
                .build();
        return XxxCommand.builder().entity(entity).build();
    }
}
```

### Exception Handling Pattern
```java
// app/exception/ — business exception
public class XxxNotFoundException extends RuntimeException {
    public XxxNotFoundException(String message) { super(message); }
}

// adapter/out/exception/ — HTTP exception handler
@RestControllerAdvice(basePackages = "edu.ai.lms.core.<module>.adapter.in")
public class XxxExceptionHandler {
    @ExceptionHandler(XxxNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(XxxNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### Persistence Adapter Pattern
```java
@Component
@RequiredArgsConstructor
class XxxPersistenceAdapter implements SaveXxxPort, LoadXxxPort, DeleteXxxPort {
    private final XxxRepository xxxRepository;
    private final XxxJdbcMapper xxxJdbcMapper;
    
    @Override
    public Xxx save(Xxx domain) {
        XxxJdbcTable entity = xxxJdbcMapper.toJdbc(domain);
        XxxJdbcTable saved = xxxRepository.save(entity);
        return xxxJdbcMapper.toDomain(saved);
    }
}
```

### Repository Pattern
```java
@Repository
interface XxxRepository extends CrudRepository<XxxJdbcTable, UUID> {
    // Custom queries for composite keys or complex operations
    @Modifying
    @Query("INSERT INTO xxx (...) VALUES (:param1, :param2)")
    void insertXxx(@Param("param1") Type param1, @Param("param2") Type param2);
}
```

### HTTP Method Rules
| Method | Request Body | Path Vars / Query Params |
|--------|-------------|-------------------------|
| POST   | ✅ Yes      | Optional                |
| PUT    | ✅ Yes      | ✅ Yes (resource ID)    |
| GET    | ❌ No       | ✅ Yes                  |
| DELETE | ❌ No       | ✅ Yes                  |
| PATCH  | ✅ Yes      | ✅ Yes                  |

### Request Validation
Use Jakarta Bean Validation on request classes:
```java
@NotBlank(message = "Field is required")
@Email(message = "Email should be valid")
@Size(min = 8, message = "Must be at least 8 characters")
@NotNull(message = "Field is required")
```

### Module Encapsulation
Every module must contain a `package-info.java`:
```java
@ApplicationModule
package edu.ai.lms.core.<module>;

import org.springframework.modulith.ApplicationModule;
```
Spring Modulith enforces that modules only access other modules through their public APIs (e.g., `adapter.in.internal` package).

### Naming Conventions
| Type | Naming Pattern | Example |
|------|----------------|---------|
| Use Case | `<Action><Entity>UseCase` | `TeacherCreateCourseUseCase` |
| Command | `<Action><Entity>Command` | `CreateCourseCommand` |
| Service | `<Action><Entity>Service` | `CreateCourseService` |
| Controller | `<Entity>Controller` | `CourseController` |
| Request | `<Action><Entity>Request` | `CreateCourseRequest` |
| Response | `<Action><Entity>Response` | `CreateCourseResponse` |
| Mapper | `<Action><Entity>CommandMapper` | `CreateCourseCommandMapper` |
| Output Port | `<Action><Entity>Port` | `SaveCoursePort`, `LoadCoursePort` |
| Persistence Adapter | `<Entity>PersistenceAdapter` | `CoursePersistenceAdapter` |
| Repository | `<Entity>Repository` | `CourseRepository` |
| JDBC Table | `<Entity>JdbcTable` | `CourseJdbcTable` |
| JDBC Mapper | `<Entity>JdbcMapper` | `CourseJdbcMapper` |
| Exception | `<Entity><Reason>Exception` | `CourseNotFoundException` |
| Exception Handler | `<Entity>ExceptionHandler` | `CourseExceptionHandler` |
| Unit Test | `<ServiceName>UT` | `CreateCourseServiceUT` |
| Integration Test | `<ControllerName>IT` | `CourseControllerIT` |

---

## Testing

### Structure
Tests mirror the main source structure:
```
src/test/java/edu/ai/lms/
├── LmsApplicationTests.java
├── PackageStructureTest.java         # Spring Modulith verification
├── config/
│   └── IntegrationTestConfiguration.java  # Base class for IT tests
├── util/
│   └── TestHelper.java              # REST Assured helper methods
└── core/
    ├── auth/
    │   ├── adapter/in/              # Integration tests (*IT.java)
    │   └── app/service/             # Unit tests (*UT.java)
    ├── course/
    │   ├── adapter/in/
    │   └── app/service/
    ├── material/
    │   ├── adapter/in/
    │   └── app/service/
    └── quiz/
        ├── adapter/in/http/
        └── app/service/
```

### Unit Tests (`*UT.java`)
- Located in `app/service/` directories
- Use `@ExtendWith(MockitoExtension.class)`
- Mock all output ports via `@Mock`
- Inject service under test via `@InjectMocks`
- Run with `./mvnw test` (Surefire plugin, matches `**/app/service/**/UT*.java` and `**/*UT.java`)

### Integration Tests (`*IT.java`)
- Located in `adapter/in/` directories
- Extend `IntegrationTestConfiguration`
- Use Testcontainers (PostgreSQL 18-alpine, singleton pattern)
- Use REST Assured for HTTP assertions
- Full Spring Boot context with random port
- Run with `./mvnw verify` (Failsafe plugin, matches `**/adapter/in/**/*IT.java`)
- `@DirtiesContext(classMode = AFTER_CLASS)` ensures clean state between test classes
- Schema is re-initialized per test run (`spring.sql.init.mode=always`)
- `data.sql` is disabled in tests for clean database state

### Test Helper Utilities (`TestHelper.java`)
Provides convenience methods for integration tests:
- `post()`, `postWithAuth()`, `get()`, `getWithAuth()` — REST Assured wrappers
- `registerStudentAndGetToken()`, `registerTeacherAndGetToken()` — Auth helpers
- `loginStudentAndGetToken()`, `loginTeacherAndGetToken()` — Login helpers
- `generateStudentEmail()`, `generateTeacherEmail()` — Unique email generators

---

## Storage (S3)

### Architecture
- Backend generates pre-signed URLs only; frontend handles actual upload/download
- `StorageConfig` interface with two implementations:
  - `DevConfig` — LocalStack (dev profile)
  - `DigitalOceanConfig` — DigitalOcean Spaces (prod profile)
- `S3StorageAdapter` is the facade implementing all storage ports

### Development Setup
1. Run `docker compose up -d` to start LocalStack
2. `init-s3.sh` auto-creates the `lms-materials` bucket with CORS config
3. Dev credentials: `access-key=test`, `secret-key=test`
4. Endpoint: `https://s3.localhost.localstack.cloud:4566`

### Upload Flow
1. Frontend calls POST API → backend creates material record (status: `PENDING`) and returns pre-signed upload URL
2. Frontend uses PUT to upload file directly to S3 via pre-signed URL
3. Frontend calls PATCH API → backend verifies upload via HeadObject, updates status to `UPLOADED`
4. `MaterialCleanupScheduler` periodically cleans up stale `PENDING` materials

### Download Flow
1. Frontend calls GET API → backend returns pre-signed download URL
2. Frontend uses GET to download file from S3 directly

### File Naming
- If filename contains spaces, frontend must replace spaces with `_`

---

## Configuration

### Environment Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `PROFILE` | Active Spring profile | `dev` or `prod` |
| `DATA_SOURCE` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/lms` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_KEY` | Base64-encoded JWT secret | (64+ char base64 string) |
| `JWT_EXPIRATION` | Token expiration in ms | `3600000` |
| `DATABASE_INITIALIZATION_OPTION` | Schema init mode | `always` or `never` |

### Production-Only Variables
| Variable | Description |
|----------|-------------|
| `REGION` | DigitalOcean region |
| `DO_END_POINT` | DigitalOcean S3 endpoint |
| `DO_ACCESS_KEY` | DigitalOcean access key |
| `DO_SECRET_KEY` | DigitalOcean secret key |
| `DO_BUCKET` | DigitalOcean bucket name |

### Profiles
- **dev**: LocalStack S3, debug logging, local PostgreSQL
- **prod**: DigitalOcean Spaces, Supabase PostgreSQL

---

## Prompt Guidelines for AI Assistants

### When Adding a New Feature to an Existing Module

1. **Domain entity** → Create in `core/<module>/app/domain/`
2. **Use case interface** → Create in `core/<module>/app/port/in/usecase/`
3. **Command record** → Create in `core/<module>/app/port/in/command/`
4. **Output ports** → Create interfaces in `core/<module>/app/port/out/`
5. **Service** → Implement use case in `core/<module>/app/service/`
6. **Request/Response DTOs** → Create in `core/<module>/adapter/in/[http/]request/` and `response/`
7. **Command mapper** → Create in `core/<module>/adapter/in/[http/]mapper/`
8. **Controller endpoint** → Add to existing controller or create new one in `core/<module>/adapter/in/[http/]`
9. **Persistence adapter** → Implement output ports in `core/<module>/adapter/out/persistence/`
10. **JDBC mapper** → Add mapping in `core/<module>/adapter/out/persistence/mapper/`
11. **Repository** → Create/update in `core/<module>/adapter/out/persistence/`
12. **JDBC table entity** → Create/update in `database/` package if new table needed
13. **Schema** → Update `schema.sql` if new tables needed
14. **Exception** → Business exception in `app/exception/`, handler in `adapter/out/exception/`
15. **Unit tests** → `*UT.java` in `test/.../app/service/`
16. **Integration tests** → `*IT.java` in `test/.../adapter/in/`

### When Creating a New Module

1. Create package `core/<module>/`
2. Add `package-info.java` with `@ApplicationModule`
3. Follow the module internal structure (app/domain, app/port, app/service, adapter/in, adapter/out)
4. If the module needs to access auth info, depend on `LoadAuthenticatedUserApi` from the auth module's internal package
5. Add new JDBC table entities to the shared `database/` package
6. Update `schema.sql` with new tables

### Context to Always Consider
- This project uses **Spring Data JDBC**, NOT JPA/Hibernate. No `@Entity`, `@OneToMany`, etc. Use `@Table`, `@MappedCollection`, `@Id` from `org.springframework.data.relational`
- Commands are **records** with `@Builder`
- Services use **constructor injection** via `@RequiredArgsConstructor` (Lombok)
- Repositories are **package-private** interfaces (no `public` modifier) to enforce encapsulation
- Persistence adapters are **package-private** classes
- All domain entities are **pure Java + Lombok** (no Spring/JDBC annotations)
- **PasswordEncoder** is currently `NoOpPasswordEncoder` (will be replaced)
- The `FullyQualifiedAnnotationBeanNameGenerator` is used globally to prevent bean name clashes across modules

### Common Pitfalls to Avoid
- Do NOT use JPA annotations (`@Entity`, `@Column` from `javax.persistence`)
- Do NOT create circular references between modules — use the internal API pattern
- Do NOT put JDBC table entities inside module packages — they go in `database/`
- Do NOT make repositories or persistence adapters `public` — keep them package-private
- Do NOT add business logic to controllers — keep it in services
- Do NOT use request/response DTOs directly in services — convert via commands
- For composite keys, do NOT rely on default `save()` — write custom `@Query` methods
- Do NOT reference the full related object in M-N relationships from the non-owning side — only reference IDs

### Relationship Modeling Rules
- **Owning side**: Contains the list of related domain objects
- **Non-owning side**: Contains only a list of IDs (e.g., `List<UUID>`)
- Example: `Course` has `List<Chapter> chapters`, but `Chapter` has `UUID courseId`
- Weak entities (Chapter, Answer) use composite primary keys with auto-generated identity columns
