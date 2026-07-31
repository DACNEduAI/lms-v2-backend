# LMS Backend - Lệnh chạy nhanh

Yêu cầu: JDK 25, `.env.dev` và PostgreSQL.

## 1. Run BE để FE kết nối

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.config.import=optional:file:.env.dev[.properties]"
```

BE chạy tại `http://localhost:8080`. Lệnh này compile code hiện tại nhưng không tạo JAR. Dừng bằng `Ctrl+C`.

## 2. Chạy test

```powershell
.\mvnw.cmd test
```

Chạy 67 unit/architecture test theo cấu hình hiện tại, không phải toàn bộ test trong repository.

## Các lệnh build/package

Full test và đóng gói JAR (cần Docker):

```powershell
.\mvnw.cmd clean verify
```

Chạy 67 unit/architecture test + 19 integration test. JAR được tạo tại `target\lms_platform-0.0.1-SNAPSHOT.jar`.

Đóng gói nhanh, bỏ qua test:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Chạy JAR đã đóng gói:

```powershell
java -jar "target\lms_platform-0.0.1-SNAPSHOT.jar" "--spring.config.import=optional:file:.env.dev[.properties]"
```
