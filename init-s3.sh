#!/bin/bash

echo "🚀 Initializing LocalStack S3 buckets..."

# 1. Create the bucket
awslocal s3 mb s3://lms-materials

# 2. Apply the CORS configuration
awslocal s3api put-bucket-cors --bucket lms-materials --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
    "MaxAgeSeconds": 3600
  }]
}'

echo "✅ S3 bucket 'lms-materials' created successfully!"