#!/bin/bash

echo "🚀 Initializing LocalStack S3 buckets..."

# Create the bucket
awslocal s3 mb s3://lms-materials

echo "✅ S3 bucket 'lms-materials' created successfully."