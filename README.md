# LMS Backend - chạy local

Yêu cầu: JDK 25, PostgreSQL và `.env.dev`.

## Supabase Storage S3

Trong Supabase, bật S3 protocol và tạo S3 access key. Bucket trong `STORAGE_PRIVATE_BUCKET` phải tồn tại.

`.env.dev` cần có:

```properties
STORAGE_S3_ENDPOINT=https://<project-ref>.storage.supabase.co/storage/v1/s3
STORAGE_S3_REGION=<project-region>
STORAGE_S3_ACCESS_KEY_ID=<access-key>
STORAGE_S3_SECRET_ACCESS_KEY=<secret-key>
STORAGE_S3_FORCE_PATH_STYLE=true
STORAGE_PRIVATE_BUCKET=<private-bucket>
```

Các key chỉ dùng ở backend, không đưa vào frontend hoặc log.

## Chạy backend

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.config.import=optional:file:.env.dev[.properties]"
```

Backend chạy tại `http://localhost:8080`.

## Kiểm thử

```powershell
.\mvnw.cmd test
```

Full build và integration test cần Docker:

```powershell
.\mvnw.cmd clean verify
```

Đóng gói nhanh, bỏ qua test:

```powershell
.\mvnw.cmd clean package -DskipTests
```
