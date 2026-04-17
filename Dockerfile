# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Runtime environment variables (injected at build time via --build-arg)
ARG PROFILE
ARG DATA_SOURCE
ARG DB_USERNAME
ARG DB_PASSWORD
ARG JWT_KEY
ARG JWT_EXPIRATION
ARG DATABASE_INITIALIZATION_OPTION
ARG REGION
ARG DO_END_POINT
ARG DO_ACCESS_KEY
ARG DO_SECRET_KEY
ARG DO_BUCKET

ENV PROFILE=${PROFILE} \
    DATA_SOURCE=${DATA_SOURCE} \
    DB_USERNAME=${DB_USERNAME} \
    DB_PASSWORD=${DB_PASSWORD} \
    JWT_KEY=${JWT_KEY} \
    JWT_EXPIRATION=${JWT_EXPIRATION} \
    DATABASE_INITIALIZATION_OPTION=${DATABASE_INITIALIZATION_OPTION} \
    REGION=${REGION} \
    DO_END_POINT=${DO_END_POINT} \
    DO_ACCESS_KEY=${DO_ACCESS_KEY} \
    DO_SECRET_KEY=${DO_SECRET_KEY} \
    DO_BUCKET=${DO_BUCKET}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
