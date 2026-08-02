#!/bin/sh
set -eu

echo "Initializing LocalStack S3 buckets..."

# Create the bucket only when it is absent, so container restarts are safe.
if ! awslocal s3api head-bucket --bucket lms-materials >/dev/null 2>&1; then
  awslocal s3 mb s3://lms-materials
fi

# Apply CORS on every startup so the configuration remains correct.
awslocal s3api put-bucket-cors --bucket lms-materials --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
    "MaxAgeSeconds": 3600
  }]
}'

echo "S3 bucket 'lms-materials' is ready."
