#!/bin/sh
set -eu

echo "Initializing LocalStack S3 buckets..."

# Keep these names aligned with application-dev.properties.  Course documents
# use lms-private; generated rich-content images use lms-public.
for bucket in lms-private lms-public; do
  if ! awslocal s3api head-bucket --bucket "$bucket" >/dev/null 2>&1; then
    awslocal s3 mb "s3://$bucket"
  fi

  # The frontend uploads directly through a browser presigned URL, so CORS is
  # required on every local bucket.
  awslocal s3api put-bucket-cors --bucket "$bucket" --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
    "MaxAgeSeconds": 3600
  }]
}'
  echo "S3 bucket '$bucket' is ready."
done
