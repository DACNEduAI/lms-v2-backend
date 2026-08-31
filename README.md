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

## Database local

Mặc định development dùng `DATABASE_INITIALIZATION_OPTION=always`. Mỗi lần
backend khởi động, `schema.sql` sẽ xoá và tạo lại schema `edu`, sau đó Spring
nạp `data.sql`. Vì vậy cấu trúc bảng luôn khớp code hiện tại (bao gồm
`quiz3_assessments.matrix_source_type`, `matrix3_id`, `matrix3_version`) và dữ
liệu test trong `edu` sẽ được reset.

`spring.quartz.jdbc.initialize-schema=always` cũng reset các bảng scheduler
trong `quartz_schema`; nó không liên quan đến bảng LMS trong `edu`. Schema
`ai_index` không bị reset. Ở production cả SQL initialization lẫn Quartz
initialization đều là `never` để bảo toàn dữ liệu.

Nếu cần giữ nguyên database local để điều tra dữ liệu, đổi tạm
`DATABASE_INITIALIZATION_OPTION=never`. Khi đó backend sẽ không tự cập nhật
schema; chỉ áp dụng migration đã được review trong `src/main/resources/db/manual`
cho database đó, rồi khởi động lại backend.

Với database có `material_index_jobs` cũ, dừng backend và chạy một lần
`src/main/resources/db/manual/V20260811_01__material_index_jobs_utc.sql` trước
khi khởi động lại.

Các migration trong `db/manual` là lịch sử nâng cấp dành cho database đã có dữ
liệu; chúng không phải file Spring tự đọc khi chạy ứng dụng. Với local reset
mode (`always`), `schema.sql` là nguồn cấu trúc duy nhất cần dùng. Riêng Quiz2
Question AI của một database được giữ lại yêu cầu:

```text
src/main/resources/db/manual/V20260813_02__quiz2_question_ai_jobs.sql
```

Backend kiểm tra các bảng, cột và unique index của Question AI khi startup. Nếu
migration trên còn thiếu hoặc chỉ chạy một phần, backend sẽ dừng với thông báo
liệt kê object thiếu thay vì tiếp tục chạy scheduler và lặp lỗi SQL.

## Chạy backend

```powershell
.\mvnw.cmd spring-boot:run
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
