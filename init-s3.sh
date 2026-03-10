#!/bin/bash

echo "🚀 Initializing LocalStack S3 buckets..."

# Create the bucket
awslocal s3api put-bucket-cors --bucket lms-materials --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["http://localhost:3000"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
    "MaxAgeSeconds": 3600
  }]
}'

echo "✅ S3 bucket 'lms-materials' created successfully with CORS configuration"