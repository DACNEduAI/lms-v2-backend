# LMS Platform - Project Directory Structure

> **Learning Management System** built with Spring Boot 3.5.6 & Java 21
> Architecture: Clean Architecture with Hexagonal Pattern
> Database: PostgreSQL with Spring Data JDBC

---

## 📁 Root Directory

```
lms/
├── .github/                        # GitHub Actions CI/CD
├── .idea/                          # IntelliJ IDEA configuration
├── .mvn/                           # Maven wrapper
├── .postman/                       # Postman configuration
├── .vscode/                        # VS Code settings
├── postman/                        # Postman collections
├── src/                            # Source code (see below)
├── target/                         # Build output
├── .env                            # Environment variables
├── .gitattributes                  # Git line ending config
├── .gitignore                      # Git ignore rules
├── ARCHITECTURE_DOCUMENTATION.md   # Architecture docs
├── mvnw                            # Maven wrapper (Unix)
├── mvnw.cmd                        # Maven wrapper (Windows)
└── pom.xml                         # Maven configuration
```

---

## 📦 Source Code Structure

### Main Application (`src/main/java`)

```
src/main/java/edu/ai/lms/
│
├── LmsApplication.java             # Spring Boot entry point
│
├── common/                         # Cross-cutting concerns
│   ├── ErrorResponse.java          # Standard error DTO
│   ├── Roles.java                  # User role enum (STUDENT, TEACHER)
│   └── User.java                   # Base user implementing UserDetails
│
├── config/                         # Spring configuration
│   └── OpenApiConfig.java          # Swagger/OpenAPI setup
│
├── database/                       # JDBC entity mappings (12 entities)
│   ├── AISettingsJdbcEntity.java
│   ├── AnswerJdbcEntity.java
│   ├── AttemptJdbcEntity.java
│   └── ... (9 more entities)
│
└── core/                           # Core business modules (see below)
```

---

## 🏗️ Core Module: Authentication (`core/auth`)

### Architecture Layers

```
core/auth/
│
├── app/                            # Application layer
│   │
│   ├── domain/                     # Domain entities
│   │   ├── Student.java
│   │   └── Teacher.java
│   │
│   ├── port/                       # Port interfaces (Hexagonal)
│   │   │
│   │   ├── in/usecase/             # Input ports (use cases)
│   │   │   ├── StudentRegisterUseCase.java
│   │   │   ├── StudentSignInUseCase.java
│   │   │   ├── TeacherRegisterUseCase.java
│   │   │   └── ... (2 more)
│   │   │
│   │   └── out/                    # Output ports (repository abstractions)
│   │       ├── AuthenticationPort.java
│   │       ├── LoadStudentPort.java
│   │       ├── SaveStudentPort.java
│   │       └── ... (3 more)
│   │
│   └── service/                    # Business logic services
│       ├── StudentRegisterService.java
│       ├── StudentSignInService.java
│       ├── TeacherRegisterService.java
│       └── ... (1 more)
│
├── adapter/                        # Adapter layer (Hexagonal)
│   │
│   ├── in/                         # Inbound adapters (Controllers)
│   │   │
│   │   ├── StudentAuthController.java
│   │   ├── TeacherAuthController.java
│   │   │
│   │   ├── request/                # Request DTOs
│   │   │   ├── StudentRegisterRequest.java
│   │   │   ├── StudentSignInRequest.java
│   │   │   ├── TeacherRegisterRequest.java
│   │   │   └── ... (1 more)
│   │   │
│   │   └── response/               # Response DTOs
│   │       ├── StudentRegisterResponse.java
│   │       ├── StudentSignInResponse.java
│   │       ├── UserResponseMapper.java
│   │       └── ... (2 more)
│   │
│   └── out/                        # Outbound adapters
│       │
│       ├── persistence/            # Database adapters
│       │   ├── StudentPersistenceAdapter.java
│       │   ├── StudentRepository.java
│       │   ├── TeacherPersistenceAdapter.java
│       │   └── ... (2 more)
│       │
│       └── security/               # Security adapters
│           ├── AuthenticationAdapter.java
│           ├── JwtAuthenticationFilter.java
│           ├── JwtUtil.java
│           └── ... (1 more)
│
├── exception/                      # Module-specific exceptions
│   ├── AuthExceptionHandler.java
│   ├── EmailAlreadyExistsException.java
│   └── InvalidCredentialsException.java
│
└── package-info.java               # Module documentation
```

**Summary**: JWT-based authentication for Students & Teachers with role-based access control

---

## 🎓 Core Module: Course Management (`core/course`)

### Architecture Layers

```
core/course/
│
├── app/                            # Application layer
│   │
│   ├── domain/                     # Domain entities
│   │   ├── Course.java             # Aggregate root
│   │   ├── Chapter.java
│   │   ├── LearningOutcome.java    # CLOs
│   │   └── ... (3 more)
│   │
│   ├── port/                       # Port interfaces
│   │   │
│   │   ├── in/usecase/             # Input ports (use cases)
│   │   │   ├── TeacherCreateCourseUseCase.java
│   │   │   ├── TeacherModifyCourseUseCase.java
│   │   │   ├── TeacherDeleteCourseUseCase.java
│   │   │   └── ... (3 more)
│   │   │
│   │   └── out/                    # Output ports
│   │       ├── DeleteCoursePort.java
│   │       ├── LoadCoursePort.java
│   │       ├── SaveCoursePort.java
│   │       └── ... (1 more)
│   │
│   └── service/                    # Business logic services
│       ├── CreateCourseService.java
│       ├── ModifyCourseService.java
│       ├── DeleteCourseService.java
│       └── ... (3 more)
│
├── adapter/                        # Adapter layer
│   │
│   ├── in/                         # Inbound adapters (Controllers)
│   │   │
│   │   ├── CourseController.java   # 5 REST endpoints
│   │   │
│   │   ├── request/                # Request DTOs
│   │   │   ├── ChangeCourseSettingRequest.java
│   │   │   ├── CreateCourseRequest.java
│   │   │   ├── ModifyCourseRequest.java
│   │   │   └── ... (3 more)
│   │   │
│   │   └── response/               # Response DTOs
│   │       ├── CreateCourseResponse.java
│   │       ├── GetAllCoursesResponse.java
│   │       ├── GetCourseResponse.java
│   │       └── ... (2 more)
│   │
│   └── out/                        # Outbound adapters
│       │
│       └── persistence/            # Database adapters
│           ├── CoursePersistenceAdapter.java
│           ├── CourseRepository.java
│           ├── ChapterRepository.java
│           └── ... (8 more mappers/repos)
│
├── exception/                      # Module-specific exceptions
│   ├── CourseCodeAlreadyExistsException.java
│   ├── CourseExceptionHandler.java
│   ├── CourseNotFoundException.java
│   └── ... (1 more)
│
└── package-info.java               # Module documentation
```

**Summary**: Full CRUD course management with chapters, materials, and learning outcomes

---

## 🚀 Placeholder Modules (`core/quiz`, `core/user`)

```
core/
├── quiz/
│   └── package-info.java           # Future quiz management module
│
└── user/
    └── package-info.java           # Future user management module
```

---

## 🗄️ Database Entities (`database/`)

```
database/
├── AISettingsJdbcEntity.java       # ML configuration
├── AnswerJdbcEntity.java           # Quiz answers
├── AttemptJdbcEntity.java          # Student attempts
├── ChapterJdbcEntity.java          # Course chapters
├── CourseJdbcEntity.java           # Course definitions
├── EnrollmentJdbcEntity.java       # Student-Course links
├── LearningOutcomeJdbcEntity.java  # CLOs
├── MaterialJdbcEntity.java         # Learning materials
├── QuestionJdbcEntity.java         # Quiz questions
├── QuizJdbcEntity.java             # Assessments
├── StudentJdbcEntity.java          # Student records
└── TeacherJdbcEntity.java          # Teacher records
```

**Database**: PostgreSQL with 12 tables, UUID primary keys, and audit timestamps

---

## ⚙️ Configuration (`src/main/resources`)

```
src/main/resources/
├── application.properties          # Main config (DB, JWT, Spring)
├── application-dev.properties      # Development profile
├── application-prod.properties     # Production profile
└── schema.sql                      # Database initialization (176 lines)
```

**Key Settings**:
- Database: PostgreSQL with connection pooling
- Security: JWT token-based authentication
- Port: 8080 (dev), configurable (prod)
- Logging: INFO level with SQL logging enabled

---

## 🧪 Test Structure (`src/test/java`)

```
src/test/java/edu/ai/lms/
│
├── LmsApplicationTests.java        # Application startup test
├── PackageStructureTest.java       # Architecture validation
│
├── config/
│   └── IntegrationTestConfiguration.java  # TestContainer setup
│
├── util/
│   └── TestHelper.java             # Shared test utilities
│
└── core/
    │
    ├── auth/
    │   │
    │   ├── app/service/            # Unit tests (Mockito)
    │   │   ├── StudentRegisterServiceUT.java
    │   │   ├── StudentSignInServiceUT.java
    │   │   ├── TeacherRegisterServiceUT.java
    │   │   └── ... (1 more)
    │   │
    │   └── adapter/in/             # Integration tests (REST-Assured)
    │       ├── StudentAuthControllerIT.java
    │       └── TeacherAuthControllerIT.java
    │
    └── course/
        │
        ├── app/service/            # Unit tests (Mockito)
        │   ├── CreateCourseServiceUT.java
        │   ├── DeleteCourseServiceUT.java
        │   ├── GetCourseServiceUT.java
        │   └── ... (3 more)
        │
        └── adapter/in/             # Integration tests (REST-Assured)
            └── CourseControllerIT.java  # 29 test cases
```

**Testing Strategy**:
- **Unit Tests (UT)**: Service layer with mocked ports (Mockito)
- **Integration Tests (IT)**: Full HTTP workflows with TestContainers (PostgreSQL)
- **Total**: 17 test classes

---

## 🏛️ Architecture Pattern Visualization

```
┌─────────────────────────────────────────────────────────────┐
│                   ADAPTER IN LAYER                          │
│  (Controllers + Request/Response DTOs)                      │
│  • REST API Endpoints                                       │
│  • Request Validation                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 APPLICATION LAYER                           │
│                                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │  USE CASE INTERFACES (Input Ports)             │       │
│  │  • TeacherCreateCourseUseCase                  │       │
│  │  • StudentRegisterUseCase                      │       │
│  └─────────────────────────────────────────────────┘       │
│                            ↓                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │  SERVICES (Business Logic)                     │       │
│  │  • CreateCourseService                         │       │
│  │  • StudentRegisterService                      │       │
│  └─────────────────────────────────────────────────┘       │
│                            ↓                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │  OUTPUT PORT INTERFACES                        │       │
│  │  • SaveCoursePort                              │       │
│  │  • LoadStudentPort                             │       │
│  └─────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   DOMAIN LAYER                              │
│  (Core Entities + Business Rules)                          │
│  • Course, Chapter, Material                               │
│  • Student, Teacher                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 ADAPTER OUT LAYER                           │
│  (Infrastructure Implementations)                          │
│  • Persistence Adapters                                    │
│  • JDBC Repositories                                       │
│  • Security Adapters (JWT)                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│               EXTERNAL DEPENDENCIES                         │
│  • PostgreSQL Database                                     │
│  • Spring Security                                         │
│  • External AI Service                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| **Total Java Files** | 128 |
| **Main Classes** | 111 |
| **Test Classes** | 17 |
| **Database Tables** | 12 |
| **REST Endpoints** | ~13 |
| **Core Modules** | 2 active + 2 planned |

---

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 3.5.6
- **Java**: 21
- **Spring Data JDBC**: (migrated from JPA)
- **Spring Security**: JWT-based authentication
- **Spring Modulith**: Module structure enforcement

### Database
- **PostgreSQL**: Primary database
- **Liquibase**: Schema migration (disabled in dev)

### Testing
- **JUnit 5**: Test framework
- **Mockito**: Unit test mocking
- **TestContainers**: Docker-based PostgreSQL for integration tests
- **REST-Assured**: API testing

### API Documentation
- **SpringDoc OpenAPI**: 2.8.13 (Swagger UI)

### Build Tool
- **Maven**: 3.x with wrapper

---

## 📝 Key Features

### ✅ Completed Features

1. **Authentication Module** (`core/auth`)
   - Student registration & sign-in
   - Teacher registration & sign-in
   - JWT token generation & validation
   - Role-based access control (STUDENT, TEACHER)

2. **Course Management Module** (`core/course`)
   - Create course with chapters, materials, CLOs
   - Update course information
   - Delete course (soft delete via active flag)
   - Get single course details
   - Get all courses list
   - Teacher can only modify own courses

### 🚧 Planned Features

3. **Quiz Management Module** (`core/quiz`)
   - AI-generated quizzes
   - Question management
   - Student attempts tracking

4. **User Management Module** (`core/user`)
   - Profile management
   - User preferences
   - Enrollment management

---

## 🎯 Architecture Principles

1. **Clean Architecture**
   - Domain at the center
   - Dependencies point inward
   - Framework-independent business logic

2. **Hexagonal Architecture (Ports & Adapters)**
   - Input ports (use case interfaces)
   - Output ports (repository abstractions)
   - Adapters implement ports (controllers, repositories)

3. **Dependency Inversion**
   - Services depend on port interfaces, not implementations
   - Adapters implement ports
   - Spring wires everything together

4. **Modular Monolith**
   - Each core module is self-contained
   - Clear module boundaries
   - Spring Modulith enforces package rules

---

## 🔐 Security Implementation

- **Authentication**: JWT tokens (Bearer authentication)
- **Authorization**: Role-based (STUDENT, TEACHER)
- **Password**: BCrypt encoding
- **Stateless**: No server-side sessions
- **Filter Chain**: JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter

---

## 🗃️ Database Schema

### Main Entities Relationships

```
teachers ─────┐
              │
              ├──> courses ──┬──> chapters ──> materials
              │              │
students ─────┤              └──> learning_outcomes
              │
              └──> enrollments

courses ──> quizzes ──> questions ──> answers
                │
students ───────┴──> attempts
```

### Key Tables

- **students**: User accounts for students
- **teachers**: User accounts for teachers
- **courses**: Course definitions with teacher ownership
- **chapters**: Sequential course sections
- **materials**: Learning resources (video, pdf, etc.)
- **learning_outcomes**: Course Learning Outcomes (CLOs)
- **enrollments**: Student-Course many-to-many
- **ai_settings**: ML model configuration
- **quizzes, questions, answers, attempts**: Assessment system

---

## 📂 Package Naming Convention

```
edu.ai.lms.core.<module>
    ├── app                     # Application layer
    │   ├── domain              # Domain entities
    │   ├── port
    │   │   ├── in.usecase      # Use case interfaces
    │   │   └── out             # Repository interfaces
    │   └── service             # Business logic
    │
    ├── adapter                 # Adapter layer
    │   ├── in                  # Inbound adapters
    │   │   ├── <Controller>.java
    │   │   ├── request         # Request DTOs
    │   │   └── response        # Response DTOs
    │   └── out                 # Outbound adapters
    │       ├── persistence     # Database
    │       └── <other>         # External services
    │
    ├── exception               # Module exceptions
    └── package-info.java       # Module documentation
```

---

## 🚀 Running the Application

### Development Mode

```bash
# Using Maven wrapper
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Application starts at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run unit tests only
./mvnw test -Dtest="**/*UT"

# Run integration tests only
./mvnw test -Dtest="**/*IT"
```

---

## 📚 Documentation

- **Architecture**: `/ARCHITECTURE_DOCUMENTATION.md`
- **API Docs**: Swagger UI at `/swagger-ui.html` (when running)
- **Module Docs**: `package-info.java` in each module
- **Database Schema**: `/src/main/resources/schema.sql`

---

## 🔄 Recent Changes

- Migrated from Spring Data JPA to Spring Data JDBC
- Fixed TestContainer database refresh between tests
- Implemented complete course management with unit & integration tests
- Added JWT authentication for both students and teachers
- Established Clean Architecture structure with Spring Modulith

---

## 👥 Development Team

**Project**: Learning Management System with AI Integration
**Repository**: DACNEduAI/lms
**Current Branch**: `write-document`
**Build Status**: Clean (no pending changes)

---

*Last Updated*: 2026-02-12
*Generated by*: Claude Code Assistant
