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
STORAGE_PUBLIC_BUCKET=<public-bucket>
AI_SERVICE_URL=http://localhost:8000
AI_SERVICE_TOKEN=<same-value-as-AI_Pipeline/.env>
```

`STORAGE_PRIVATE_BUCKET` remains private for course materials. `STORAGE_PUBLIC_BUCKET`
must be an existing public bucket and is used only for rich-content question images.

`AI_SERVICE_TOKEN` là secret dùng riêng cho giao tiếp BE → AI. FE không được nhận
hoặc gửi token này. Giá trị phải trùng với `AI_Pipeline/.env`.

Các key chỉ dùng ở backend, không đưa vào frontend hoặc log.

Database đã có dữ liệu phải dùng `DATABASE_INITIALIZATION_OPTION=never`.
Giá trị `always` chỉ dành cho database development có thể xóa toàn bộ, vì
`schema.sql` drop và tạo lại schema ứng dụng khi backend khởi động.

Với database có `material_index_jobs` cũ, dừng backend và chạy một lần
`src/main/resources/db/manual/V20260811_01__material_index_jobs_utc.sql` trước
khi khởi động lại.

Các database đã có dữ liệu không tự chạy migration trong `db/manual`. Khi đổi
checkout, áp các migration chưa chạy theo thứ tự tên file trong
`src/main/resources/db/manual`, mỗi file trong một transaction, rồi mới khởi
động backend. Riêng Quiz2 Question AI yêu cầu:

```text
src/main/resources/db/manual/V20260813_02__quiz2_question_ai_jobs.sql
```

Backend kiểm tra các bảng, cột và unique index của Question AI khi startup. Nếu
migration trên còn thiếu hoặc chỉ chạy một phần, backend sẽ dừng với thông báo
liệt kê object thiếu thay vì tiếp tục chạy scheduler và lặp lỗi SQL.

## Chạy backend

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.config.import=optional:file:.env.dev[.properties]"
```

Backend chạy tại `http://localhost:8080`.

Backend ghi request API và lời gọi AI ra console kèm `X-Correlation-ID`. Chỉ đặt
`APP_LOG_LEVEL=DEBUG` khi cần chẩn đoán sâu ở local. Workflow log không ghi body,
credential, prompt hoặc signed URL.

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
