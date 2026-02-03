# LMS Platform - Hexagonal Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [Directory Structure](#directory-structure)
4. [Layer Responsibilities](#layer-responsibilities)
5. [Design Patterns & Conventions](#design-patterns--conventions)
6. [Data Flow & Communication](#data-flow--communication)
7. [Validation Strategy](#validation-strategy)
8. [Database Layer](#database-layer)
9. [Examples & Implementation](#examples--implementation)

---

## Overview

The LMS (Learning Management System) Platform is a Spring Boot 3.5.6 application built using **Hexagonal Architecture** (also known as Ports & Adapters or Clean Architecture). The application is designed with clear separation of concerns, ensuring that business logic remains independent of technical implementation details.

### Technology Stack
- **Framework**: Spring Boot 3.5.6
- **Java Version**: 21
- **Build Tool**: Maven
- **ORM**: Spring Data JPA
- **Security**: Spring Security with JWT
- **API Documentation**: OpenAPI/Swagger
- **Architecture**: Spring Modulith

---

## Architecture Principles

### Core Hexagonal Architecture Concepts

The application follows these fundamental principles:

1. **Domain Independence**: Business logic in the `app` layer is completely independent of technical frameworks
2. **Dependency Inversion**: Dependencies point inward (from adapters to domain), never outward
3. **Ports & Adapters**: All external interactions happen through defined interfaces (ports)
4. **Testability**: Core business logic can be tested without external dependencies

### Clean Architecture Rules

#### 🚫 What's NOT Allowed in Use Cases
- Direct use of technical frameworks (Spring annotations, JPA entities, HTTP objects)
- Infrastructure concerns (database access, file I/O, network calls)
- Presentation logic (JSON formatting, HTTP response building)

#### ✅ What IS Allowed in Use Cases
- Domain entities and business models
- Business rule validation
- Domain logic implementation
- Calling output ports (interfaces)

---

## Directory Structure

```
src/main/java/edu/ai/lms/
│
├── LmsApplication.java                 # Spring Boot entry point
├── config/                             # Global configuration (OpenAPI, etc.)
│   └── OpenApiConfig.java
│
├── core/                               # Application core (business logic)
│   ├── auth/                          # Authentication feature module
│   │   ├── package-info.java         # Spring Modulith module definition
│   │   ├── adapter/                  # Adapters (external interfaces)
│   │   │   ├── in/                   # Input adapters (controllers)
│   │   │   │   ├── StudentAuthController.java
│   │   │   │   ├── request/          # Request DTOs
│   │   │   │   │   ├── StudentRegisterRequest.java
│   │   │   │   │   └── StudentSignInRequest.java
│   │   │   │   └── response/         # Response DTOs
│   │   │   │       ├── StudentRegisterResponse.java
│   │   │   │       ├── StudentSignInResponse.java
│   │   │   │       └── UserResponseMapper.java
│   │   │   └── out/                  # Output adapters (infrastructure)
│   │   │       ├── persistence/      # Database adapters
│   │   │       │   ├── StudentPersistenceAdapter.java
│   │   │       │   ├── UserJpaMapper.java
│   │   │       │   └── UserRepository.java
│   │   │       └── security/         # Security infrastructure
│   │   │           ├── SecurityConfig.java
│   │   │           ├── JwtUtil.java
│   │   │           ├── JwtAuthenticationFilter.java
│   │   │           └── AuthenticationAdapter.java
│   │   ├── app/                      # Application layer (business logic)
│   │   │   ├── domain/               # Domain entities (business models)
│   │   │   │   ├── Student.java
│   │   │   │   └── Roles.java
│   │   │   ├── port/                 # Port interfaces
│   │   │   │   ├── in/               # Input ports (use case interfaces)
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── StudentRegisterUseCase.java
│   │   │   │   │       ├── StudentSignInUseCase.java
│   │   │   │   │       ├── TeacherRegisterUseCase.java
│   │   │   │   │       └── TeacherSignInUseCase.java
│   │   │   │   └── out/              # Output ports (data access interfaces)
│   │   │   │       ├── SaveStudentPort.java
│   │   │   │       ├── LoadStudentPort.java
│   │   │   │       ├── AuthenticationPort.java
│   │   │   │       └── TokenGenerationPort.java
│   │   │   └── service/              # Use case implementations
│   │   │       ├── StudentRegisterService.java
│   │   │       └── StudentSignInService.java
│   │   └── exception/                # Feature-specific exceptions
│   │       ├── AuthExceptionHandler.java
│   │       ├── EmailAlreadyExistsException.java
│   │       └── InvalidCredentialsException.java
│   │
│   ├── course/                        # Course feature module
│   │   ├── package-info.java
│   │   ├── Course.java
│   │   ├── domain/
│   │   └── exception/
│   │
│   ├── quiz/                          # Quiz feature module
│   │   └── package-info.java
│   │
│   └── user/                          # User feature module
│       └── package-info.java
│
└── database/                          # Shared JPA entities (technical layer)
    ├── UserJpaEntity.java
    ├── StudentJpaEntity.java
    ├── TeacherJpaEntity.java
    ├── CourseJpaEntity.java
    ├── ChapterJpaEntity.java
    ├── MaterialJpaEntity.java
    ├── QuizJpaEntity.java
    ├── QuestionJpaEntity.java
    ├── AnswerJpaEntity.java
    ├── AttemptJpaEntity.java
    ├── EnrollmentJpaEntity.java
    ├── LearningOutcomeJpaEntity.java
    ├── AISettingsJpaEntity.java
    ├── StudentAISettingsJpaEntity.java
    ├── TeacherAISettingsJpaEntity.java
    └── UserAuthJpaEntity.java
```

---

## Layer Responsibilities

### 1. Core Layer (`core/`)

The heart of the application containing all business logic, organized by feature modules.

#### Feature Module Structure

Each feature (e.g., `auth`, `course`, `quiz`) follows this structure:

```
feature/
├── package-info.java      # Spring Modulith module definition
├── adapter/
│   ├── in/               # Input adapters
│   └── out/              # Output adapters
├── app/
│   ├── domain/           # Domain entities
│   ├── port/             # Port interfaces
│   │   ├── in/          # Use case interfaces
│   │   └── out/         # Data access interfaces
│   └── service/         # Use case implementations
└── exception/           # Feature-specific exceptions
```

### 2. Adapter Layer

#### Input Adapters (`adapter/in/`)

**Controllers** - REST API endpoints

Responsibilities:
1. Map HTTP requests to Java objects
2. Perform authorization checks
3. Validate input (syntactical validation via `@Valid`)
4. Map input to the use case input model
5. Call the use case
6. Map use case output to HTTP response
7. Return HTTP response

**Request DTOs** (`adapter/in/request/`)
- Located inside `adapter/in/request/` package
- Responsible for **syntactical validation** (format, required fields, regex patterns)
- Uses Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, etc.)
- Fields must be annotated with validation constraints
- Validation happens automatically via `@Valid` annotation in controller

**Response DTOs** (`adapter/in/response/`)
- Located inside `adapter/in/response/` package
- Used to format data for HTTP responses
- Can include response mappers for domain → DTO conversion

#### Output Adapters (`adapter/out/`)

**Persistence Adapters** (`adapter/out/persistence/`)

Responsibilities:
1. Take input from use cases (domain objects)
2. Map domain objects to JPA entities using **JPA Mappers**
3. Send data to the database (via Spring Data repositories)
4. Map JPA entities back to domain objects
5. Return domain objects to use cases

Components:
- **PersistenceAdapter**: Implements output port interfaces
- **JpaMapper**: Maps between domain entities and JPA entities
- **Repository**: Spring Data JPA repository interface

**Security/Infrastructure Adapters** (`adapter/out/security/`)
- Security configuration
- JWT utilities
- Authentication filters
- Technical infrastructure implementations

### 3. Application Layer (`app/`)

#### Domain Layer (`app/domain/`)

**Domain Entities** - Pure business objects

Characteristics:
- Represent core business concepts
- Contain business logic and business rule validation
- Independent of any framework or infrastructure
- Use standard Java/Lombok annotations only
- Implement business behavior methods

Example: `Student.java` is a domain entity representing a student with business logic

#### Port Layer (`app/port/`)

**Input Ports** (`app/port/in/`)

Use case interfaces that define what the application can do.

Characteristics:
- Each use case = one interface with one method
- Follows **Interface Segregation Principle (ISP)**
- Method signature includes request and response types
- Located in `usecase/` subdirectory

Example:
```java
public interface StudentRegisterUseCase {
    StudentRegisterResponse studentRegister(StudentRegisterRequest request);
}
```

**Output Ports** (`app/port/out/`)

Data access interfaces that define how the application interacts with external systems.

Characteristics:
- Focused, single-responsibility interfaces (ISP)
- Work with domain entities, not JPA entities
- Implemented by persistence adapters

Examples:
- `SaveStudentPort` - for saving students
- `LoadStudentPort` - for loading students
- `TokenGenerationPort` - for generating JWT tokens

#### Service Layer (`app/service/`)

**Use Case Implementations** - Business logic orchestration

Responsibilities:
1. Take input (request DTO or domain object)
2. Validate **business rules** (semantical validation)
3. Manipulate domain model state
4. Call output ports for data persistence/retrieval
5. Return output (response DTO or domain object)

Characteristics:
- One service implements one use case interface
- Annotated with `@Service`
- Uses `@Transactional` when needed
- Dependency injection via constructor (Lombok `@RequiredArgsConstructor`)

### 4. Database Layer (`database/`)

**JPA Entities** - Technical persistence layer

Why a dedicated `database` package?
- **Cross-feature relationships**: Allows JPA entities from different features to reference each other
- **All entities are public**: Enables relationship annotations (`@ManyToOne`, `@OneToMany`, etc.)
- **Separation of concerns**: Domain entities are separate from persistence entities

Characteristics:
- Annotated with JPA annotations (`@Entity`, `@Table`, `@Column`, etc.)
- Use inheritance when appropriate (`UserJpaEntity` → `StudentJpaEntity`)
- Contain database-specific concerns (indexes, constraints, relationships)
- Public visibility to allow cross-module relationships

---

## Design Patterns & Conventions

### Use Cases vs CRUD Operations

#### Complex Use Cases

**When to use:**
- Business logic involves multiple steps
- Requires business rule validation
- Orchestrates multiple domain objects
- Has significant complexity

**Implementation:**
1. Create dedicated use case interface in `app/port/in/usecase/`
2. Implement in service class in `app/service/`
3. Controller calls the use case interface

**Example:** Student Registration
- Checks if email exists (business rule)
- Creates student with encoded password
- Generates JWT token
- Returns response with token

#### Simple CRUD Operations

**When to use:**
- Basic create/read/update/delete without complex logic
- No significant business rules
- Straightforward data access

**Implementation:**
- Still modeled as a use case in design diagrams
- Implementation may use a **query interface** instead of full use case
- Simpler validation and logic flow

### Interface Segregation Principle (ISP)

All port interfaces follow ISP:

✅ **Good:**
```java
public interface SaveStudentPort {
    boolean save(Student student);
}

public interface LoadStudentPort {
    Optional<Student> findByEmail(String email);
}
```

❌ **Bad:**
```java
public interface StudentPort {
    boolean save(Student student);
    Optional<Student> findByEmail(String email);
    void delete(Student student);
    List<Student> findAll();
}
```

### DTO Design Conventions

#### Request DTOs

**Annotations:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
```

**Validation:**
- Use Jakarta Bean Validation annotations
- Validate format, presence, size, patterns

**Example:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

#### Response DTOs

**Annotations:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
```

**Characteristics:**
- Fields should be `final` to prevent accidental changes (convention, may vary in implementation)
- Use `@Builder` for flexibility in adding fields
- May include builder methods for construction

### Entity Annotations

Any class acting as:
- **Domain entity**
- **Request DTO**
- **Response DTO**
- **JPA entity**

Must have these annotations:
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Use @SuperBuilder if there is inheritance
```

For classes with inheritance (like `StudentJpaEntity extends UserJpaEntity`):
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder  // Instead of @Builder
```

### HTTP Method Conventions

**Request Body Rules:**
- **POST**: ✅ Allowed (create operations)
- **PUT**: ✅ Allowed (update operations)
- **GET**: ❌ Only path variables or URL params
- **DELETE**: ❌ Only path variables or URL params

---

## Data Flow & Communication

### Typical Request Flow

```
HTTP Request
    ↓
[Controller] (adapter/in)
    ↓ validates input syntax
    ↓ maps to domain/DTO
    ↓
[Use Case Interface] (app/port/in)
    ↓
[Service Implementation] (app/service)
    ↓ validates business rules
    ↓ manipulates domain state
    ↓
[Output Port Interface] (app/port/out)
    ↓
[Persistence Adapter] (adapter/out/persistence)
    ↓ maps domain → JPA entity
    ↓
[JPA Repository]
    ↓
Database
```

### Response Flow

```
Database
    ↓
[JPA Repository]
    ↓ returns JPA entity
    ↓
[Persistence Adapter]
    ↓ maps JPA entity → domain
    ↓
[Service Implementation]
    ↓ builds response
    ↓
[Controller]
    ↓ maps to Response DTO
    ↓
HTTP Response (JSON)
```

---

## Validation Strategy

### Two Types of Validation

#### 1. Input Validation (Syntactical)

**Purpose:** Validate raw input format and structure

**Where:** Request DTOs in `adapter/in/request/`

**How:** Jakarta Bean Validation annotations

**Examples:**
- `@NotBlank` - field cannot be blank
- `@Email` - must be valid email format
- `@Size(min=8)` - minimum length
- `@Pattern` - regex validation
- `@NotNull` - cannot be null

**Triggered:** Automatically via `@Valid` annotation in controller

```java
@PostMapping("/register")
public ResponseEntity<StudentRegisterResponse> registerStudent(
    @Valid @RequestBody StudentRegisterRequest request) {
    // If validation fails, Spring returns 400 Bad Request automatically
    // ...
}
```

#### 2. Business Rule Validation (Semantical)

**Purpose:** Validate business logic and domain rules

**Where:** 
- Use case implementations (`app/service/`)
- Domain entities (`app/domain/`)

**How:** 
- Conditional logic with exceptions
- Domain methods

**Examples:**
- Email already exists
- Insufficient permissions
- Invalid business state transitions
- Domain-specific constraints

**Implementation:**
```java
@Service
public class StudentRegisterService implements StudentRegisterUseCase {
    
    @Override
    public StudentRegisterResponse studentRegister(StudentRegisterRequest request) {
        // Business rule validation
        if (loadStudentPort.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        // Business logic
        Student student = Student.builder()
            .email(request.getEmail())
            // ...
            .build();
            
        saveStudentPort.save(student);
        // ...
    }
}
```

### Validation Flow Diagram

```
Request → Controller
             ↓
         @Valid checks
             ↓
         [Input Validation]
         (format, required, size)
             ↓ (pass)
         Use Case
             ↓
         [Business Rule Validation]
         (email exists, permissions, domain rules)
             ↓ (pass)
         Business Logic Execution
```

---

## Database Layer

### JPA Entity Design

#### Shared Database Package

All JPA entities are in `edu.ai.lms.database` package:

**Rationale:**
1. **Cross-feature relationships**: Courses, Students, Teachers, Quizzes all need to reference each other
2. **Public visibility**: All entities must be public to use JPA relationships
3. **Centralized persistence model**: Single source of truth for database schema

#### Inheritance Strategy

Uses **JOINED** table inheritance:

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "users")
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String email;
    private String password;
    // common fields
}

@Entity
@Table(name = "students")
public class StudentJpaEntity extends UserJpaEntity {
    private Long enrollmentYear;
    
    @OneToMany(mappedBy = "student")
    private List<AttemptJpaEntity> attempts;
}
```

### Persistence Adapter Pattern

#### Components:

1. **PersistenceAdapter** - Implements output ports
2. **JpaMapper** - Maps domain ↔ JPA entities
3. **Repository** - Spring Data JPA interface

#### Example Implementation:

**PersistenceAdapter:**
```java
@Component
class StudentPersistenceAdapter implements SaveStudentPort, LoadStudentPort {
    
    private final UserRepository userRepository;
    private final UserJpaMapper userJpaMapper;
    
    @Override
    public boolean save(Student student) {
        userRepository.save(userJpaMapper.mapToStudentJpaEntity(student));
        return true;
    }
    
    @Override
    public Optional<Student> findByEmail(String email) {
        return userRepository.findStudentByEmail(email)
            .map(userJpaMapper::mapToStudent);
    }
}
```

**JpaMapper:**
```java
@Component
class UserJpaMapper {
    
    public StudentJpaEntity mapToStudentJpaEntity(Student student) {
        return StudentJpaEntity.builder()
            .id(student.getId())
            .email(student.getEmail())
            .password(student.getPassword())
            .enrollmentYear(student.getEnrollmentYear())
            .role(mapToJpaRole(student.getRole()))
            .build();
    }
    
    public Student mapToStudent(StudentJpaEntity entity) {
        return Student.builder()
            .id(entity.getId())
            .email(entity.getEmail())
            .password(entity.getPassword())
            .enrollmentYear(entity.getEnrollmentYear())
            .role(mapToDomainRole(entity.getRole()))
            .build();
    }
    
    // Role mapping methods...
}
```

**Repository:**
```java
public interface UserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<StudentJpaEntity> findStudentByEmail(String email);
}
```

#### Persistence Flow:

**Save:**
```
Domain Entity (Student)
    ↓
[JpaMapper.mapToStudentJpaEntity()]
    ↓
JPA Entity (StudentJpaEntity)
    ↓
[Repository.save()]
    ↓
Database
```

**Load:**
```
Database
    ↓
[Repository.findByEmail()]
    ↓
JPA Entity (StudentJpaEntity)
    ↓
[JpaMapper.mapToStudent()]
    ↓
Domain Entity (Student)
```

---

## Examples & Implementation

### Complete Feature Implementation: Student Registration

#### 1. Request DTO (`adapter/in/request/StudentRegisterRequest.java`)

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Enrollment year is required")
    private Long enrollmentYear;
}
```

#### 2. Response DTO (`adapter/in/response/StudentRegisterResponse.java`)

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegisterResponse {
    private String token;
    private String email;
}
```

#### 3. Controller (`adapter/in/StudentAuthController.java`)

```java
@RestController
@RequestMapping("/api/auth/student")
@RequiredArgsConstructor
public class StudentAuthController {

    private final StudentRegisterUseCase studentRegisterUseCase;

    @PostMapping("/register")
    public ResponseEntity<StudentRegisterResponse> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request) {
        StudentRegisterResponse response = studentRegisterUseCase.studentRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### 4. Use Case Interface (`app/port/in/usecase/StudentRegisterUseCase.java`)

```java
public interface StudentRegisterUseCase {
    StudentRegisterResponse studentRegister(StudentRegisterRequest request);
}
```

#### 5. Domain Entity (`app/domain/Student.java`)

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student implements UserDetails {
    private UUID id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Long enrollmentYear;
    private Roles role;

    public String getRoleAsString() {
        return this.role != null ? this.role.name() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleAsString()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
```

#### 6. Output Ports (`app/port/out/`)

```java
public interface SaveStudentPort {
    boolean save(Student student);
}

public interface LoadStudentPort {
    Optional<Student> findByEmail(String email);
}

public interface TokenGenerationPort {
    String generateToken(Student student);
}
```

#### 7. Service Implementation (`app/service/StudentRegisterService.java`)

```java
@Service
@RequiredArgsConstructor
public class StudentRegisterService implements StudentRegisterUseCase {

    private final SaveStudentPort saveStudentPort;
    private final LoadStudentPort loadStudentPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public StudentRegisterResponse studentRegister(StudentRegisterRequest request) {
        // Step 1: Business rule validation
        if (loadStudentPort.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // Step 2: Build domain object
        Student student = Student.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enrollmentYear(request.getEnrollmentYear())
                .role(Roles.STUDENT)
                .build();

        // Step 3: Persist domain object
        saveStudentPort.save(student);

        // Step 4: Generate token
        String token = tokenGenerationPort.generateToken(student);

        // Step 5: Return response
        return StudentRegisterResponse.builder()
                .token(token)
                .email(student.getEmail())
                .build();
    }
}
```

#### 8. Persistence Adapter (`adapter/out/persistence/StudentPersistenceAdapter.java`)

```java
@Component
@AllArgsConstructor
class StudentPersistenceAdapter implements SaveStudentPort, LoadStudentPort {
    
    private final UserRepository userRepository;
    private final UserJpaMapper userJpaMapper;

    @Override
    public boolean save(Student student) {
        userRepository.save(userJpaMapper.mapToStudentJpaEntity(student));
        return true;
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return userRepository.findStudentByEmail(email)
                .map(userJpaMapper::mapToStudent);
    }
}
```

#### 9. JPA Mapper (`adapter/out/persistence/UserJpaMapper.java`)

```java
@Component
class UserJpaMapper {

    public StudentJpaEntity mapToStudentJpaEntity(Student student) {
        return StudentJpaEntity.builder()
                .id(student.getId())
                .email(student.getEmail())
                .password(student.getPassword())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .avatarUrl(student.getAvatarUrl())
                .enrollmentYear(student.getEnrollmentYear())
                .role(mapToJpaRole(student.getRole()))
                .build();
    }

    public Student mapToStudent(StudentJpaEntity entity) {
        return Student.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .avatarUrl(entity.getAvatarUrl())
                .enrollmentYear(entity.getEnrollmentYear())
                .role(mapToDomainRole(entity.getRole()))
                .build();
    }
    
    private UserJpaEntity.Roles mapToJpaRole(Roles role) {
        if (role == null) return UserJpaEntity.Roles.STUDENT;
        return switch (role) {
            case ADMIN -> UserJpaEntity.Roles.ADMIN;
            case TEACHER -> UserJpaEntity.Roles.TEACHER;
            case STUDENT -> UserJpaEntity.Roles.STUDENT;
        };
    }

    private Roles mapToDomainRole(UserJpaEntity.Roles role) {
        if (role == null) return Roles.STUDENT;
        return switch (role) {
            case ADMIN -> Roles.ADMIN;
            case TEACHER -> Roles.TEACHER;
            case STUDENT -> Roles.STUDENT;
        };
    }
}
```

#### 10. JPA Entity (`database/StudentJpaEntity.java`)

```java
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentJpaEntity extends UserJpaEntity {
    @Column(nullable = false)
    private Long enrollmentYear;

    @OneToMany(mappedBy = "student")
    private List<AttemptJpaEntity> attempts;

    @OneToMany(mappedBy = "student")
    private List<QuizJpaEntity> quizzes;

    @ManyToMany
    @JoinTable(
        name = "student_learning_outcomes",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "learning_outcome_id")
    )
    private List<LearningOutcomeJpaEntity> masteredLearningOutcomes;
}
```

#### 11. Base JPA Entity (`database/UserJpaEntity.java`)

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserJpaEntity {

    public enum Roles {
        ADMIN,
        TEACHER,
        STUDENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private Roles role;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### 12. Repository Interface (`adapter/out/persistence/UserRepository.java`)

```java
public interface UserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<StudentJpaEntity> findStudentByEmail(String email);
    Optional<TeacherJpaEntity> findTeacherByEmail(String email);
}
```

#### 13. Exception Handling (`exception/EmailAlreadyExistsException.java`)

```java
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### 14. Exception Handler (`exception/AuthExceptionHandler.java`)

```java
@RestControllerAdvice
public class AuthExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

---

## Module Organization (Spring Modulith)

Each feature module is marked with `@ApplicationModule`:

```java
@ApplicationModule
package edu.ai.lms.core.auth;

import org.springframework.modulith.ApplicationModule;
```

This enables:
- Module boundary enforcement
- Documentation generation
- Module testing
- Event-driven communication between modules

---

## Testing Strategy

### Unit Testing Use Cases

Test services independently by mocking ports:

```java
@ExtendWith(MockitoExtension.class)
class StudentRegisterServiceTest {
    
    @Mock
    private SaveStudentPort saveStudentPort;
    
    @Mock
    private LoadStudentPort loadStudentPort;
    
    @Mock
    private TokenGenerationPort tokenGenerationPort;
    
    @InjectMocks
    private StudentRegisterService service;
    
    @Test
    void shouldRegisterStudent() {
        // Given
        StudentRegisterRequest request = StudentRegisterRequest.builder()
            .email("test@example.com")
            .password("password123")
            .firstName("John")
            .lastName("Doe")
            .enrollmentYear(2024L)
            .build();
            
        when(loadStudentPort.findByEmail(anyString())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateToken(any())).thenReturn("mock-token");
        when(saveStudentPort.save(any())).thenReturn(true);
        
        // When
        StudentRegisterResponse response = service.studentRegister(request);
        
        // Then
        assertNotNull(response);
        assertEquals("mock-token", response.getToken());
        verify(saveStudentPort).save(any());
    }
    
    @Test
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        StudentRegisterRequest request = StudentRegisterRequest.builder()
            .email("existing@example.com")
            .build();
            
        when(loadStudentPort.findByEmail("existing@example.com"))
            .thenReturn(Optional.of(mock(Student.class)));
        
        // When & Then
        assertThrows(EmailAlreadyExistsException.class, 
            () -> service.studentRegister(request));
    }
}
```

---

## Best Practices Summary

### ✅ DO

1. **Separate validation types**: Input validation in DTOs, business rules in use cases
2. **Use ISP for ports**: Small, focused interfaces
3. **One use case = one interface + one service**: Clear responsibility
4. **Map between layers**: Never pass JPA entities to controllers
5. **Keep domain pure**: No framework dependencies in domain layer
6. **Use builders**: For all DTOs and entities
7. **Transaction boundaries**: Apply `@Transactional` at service level
8. **Exception handling**: Feature-specific exception handlers

### ❌ DON'T

1. **Mix validation types**: Don't do business rules in DTOs or input validation in services
2. **Expose JPA entities**: Never return JPA entities from controllers
3. **Bypass ports**: Don't access repositories directly from services
4. **Large interfaces**: Avoid god interfaces with many methods
5. **Framework in domain**: No Spring annotations in domain entities
6. **Shared exceptions**: Don't use generic exception handling across features
7. **Mutable DTOs**: Prefer immutable fields (final) in DTOs when possible
8. **Cross-module dependencies**: Use events instead of direct calls

---

## Additional Resources

### Configuration Files

- **application.properties**: Database, security, and application configuration
- **pom.xml**: Maven dependencies and build configuration
- **OpenApiConfig.java**: Swagger/OpenAPI documentation setup

### Security

- JWT-based authentication
- Custom security filters
- Role-based access control (ADMIN, TEACHER, STUDENT)

### API Documentation

- OpenAPI/Swagger integration
- Available at `/swagger-ui.html` (when running)
- Annotations on controllers for API documentation

---

## Appendix: Quick Reference

### Package Naming Convention

```
edu.ai.lms.core.<feature>.<layer>.<sublayer>
```

Examples:
- `edu.ai.lms.core.auth.adapter.in.request`
- `edu.ai.lms.core.auth.app.service`
- `edu.ai.lms.core.auth.app.port.out`

### File Naming Convention

- **Controller**: `<Entity>Controller.java` (e.g., `StudentAuthController.java`)
- **Request DTO**: `<Entity><Action>Request.java` (e.g., `StudentRegisterRequest.java`)
- **Response DTO**: `<Entity><Action>Response.java` (e.g., `StudentRegisterResponse.java`)
- **Use Case Interface**: `<Entity><Action>UseCase.java` (e.g., `StudentRegisterUseCase.java`)
- **Service**: `<Entity><Action>Service.java` (e.g., `StudentRegisterService.java`)
- **Port**: `<Action><Entity>Port.java` (e.g., `SaveStudentPort.java`, `LoadStudentPort.java`)
- **Adapter**: `<Entity>PersistenceAdapter.java` (e.g., `StudentPersistenceAdapter.java`)
- **Mapper**: `<Entity>JpaMapper.java` (e.g., `UserJpaMapper.java`)
- **JPA Entity**: `<Entity>JpaEntity.java` (e.g., `StudentJpaEntity.java`)

---

**Document Version**: 1.0  
**Last Updated**: February 3, 2026  
**Project**: LMS Platform - Hexagonal Architecture  
**Spring Boot Version**: 3.5.6  
**Java Version**: 21
