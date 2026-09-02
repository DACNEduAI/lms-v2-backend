#!/bin/sh
set -eu

echo "Initializing LocalStack S3 buckets..."

for bucket in lms-private lms-public; do
  # Create buckets only when absent, so container restarts preserve local data.
  if ! awslocal s3api head-bucket --bucket "$bucket" >/dev/null 2>&1; then
    awslocal s3api create-bucket \
      --bucket "$bucket" \
      --region us-east-1
  fi

  # Apply CORS on every startup so browser uploads through signed URLs work.
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
