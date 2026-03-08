# 🚀 Deployment Guide – LMS Platform

> Step-by-step instructions for configuring GitHub Actions secrets, building a
> Docker image, and deploying to a **DigitalOcean Droplet**.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Prerequisites](#2-prerequisites)
3. [Store Secrets in GitHub](#3-store-secrets-in-github)
4. [Dockerfile Explained](#4-dockerfile-explained)
5. [GitHub Actions Workflows](#5-github-actions-workflows)
6. [Prepare the DigitalOcean Droplet](#6-prepare-the-digitalocean-droplet)
7. [End-to-End Flow](#7-end-to-end-flow)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Overview

The CI/CD pipeline consists of **two GitHub Actions workflows**:

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| **CI** | `.github/workflows/maven.yml` | push / PR to `main` | Compile, unit test, integration test |
| **Deploy** | `.github/workflows/deploy.yml` | CI workflow succeeds on `main` | Build Docker image → push to Docker Hub → deploy to Droplet |

```
push to main
     │
     ▼
┌──────────┐   success   ┌─────────────────┐        ┌──────────────────┐
│  CI (maven.yml)  │ ──────────▶ │  Build & Push    │ ──────▶ │  Deploy to       │
│  compile + test  │            │  Docker image    │        │  DO Droplet      │
└──────────┘            └─────────────────┘        └──────────────────┘
```

---

## 2. Prerequisites

| Requirement | Notes |
|---|---|
| **GitHub repository** | The LMS source code hosted on GitHub |
| **Docker Hub account** | Free account at <https://hub.docker.com> (or use DigitalOcean Container Registry) |
| **DigitalOcean Droplet** | Ubuntu 22.04+ with Docker installed |
| **SSH key pair** | Used by GitHub Actions to connect to the Droplet |
| **Production PostgreSQL** | e.g. Supabase, managed DO database, or self-hosted |
| **DigitalOcean Spaces** | S3-compatible object storage for file uploads |

---

## 3. Store Secrets in GitHub

GitHub Actions **secrets** are encrypted values that are injected into workflows
at runtime. They never appear in logs or workflow files.

### 3.1 Navigate to Secrets Settings

1. Go to your repository on GitHub: `https://github.com/DACNEduAI/lms-v2`
2. Click **Settings** (top tab bar)
3. In the left sidebar click **Secrets and variables → Actions**
4. Click the **"New repository secret"** button

### 3.2 Required Secrets

Add **each** of the following secrets one by one:

#### Docker Hub Credentials

| Secret Name | Example Value | Description |
|---|---|---|
| `DOCKERHUB_USERNAME` | `myuser` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | `dckr_pat_abc123…` | Docker Hub personal access token ([create one here](https://hub.docker.com/settings/security)) |

#### DigitalOcean Droplet (SSH)

| Secret Name | Example Value | Description |
|---|---|---|
| `DROPLET_HOST` | `164.90.xxx.xxx` | Public IPv4 address of your Droplet |
| `DROPLET_USERNAME` | `root` | SSH user on the Droplet |
| `DROPLET_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----…` | Full contents of your **private** SSH key (see [§6.1](#61-ssh-access)) |

#### Application – Database

| Secret Name | Example Value | Description |
|---|---|---|
| `DATA_SOURCE` | `jdbc:postgresql://db.example.com:5432/lms` | JDBC connection URL |
| `DB_USERNAME` | `lms_user` | PostgreSQL username |
| `DB_PASSWORD` | `s3cureP@ss!` | PostgreSQL password |

#### Application – JWT

| Secret Name | Example Value | Description |
|---|---|---|
| `JWT_KEY` | `a-very-long-random-base64-string` | HMAC signing key (≥ 256 bits recommended) |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in **milliseconds** (86 400 000 = 24 h) |

#### Application – DigitalOcean Spaces (S3-compatible storage)

| Secret Name | Example Value | Description |
|---|---|---|
| `REGION` | `sgp1` | Spaces region |
| `DO_END_POINT` | `https://sgp1.digitaloceanspaces.com` | Spaces endpoint URL |
| `DO_ACCESS_KEY` | `DO00XXXXXXXXXX` | Spaces access key |
| `DO_SECRET_KEY` | `wJalrXUtn…` | Spaces secret key |
| `DO_BUCKET` | `lms-materials` | Spaces bucket name |

> **Tip:** You can also use **GitHub Environments** (e.g. `Production`) to
> restrict which branches can use certain secrets and require manual approval.

---

## 4. Dockerfile Explained

The Dockerfile at the repository root uses a **multi-stage build**:

```dockerfile
# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key design decisions:**

| Aspect | Detail |
|---|---|
| **Multi-stage** | The build stage (JDK + Maven) is discarded; only the slim JRE layer ships |
| **Dependency caching** | `dependency:go-offline` is run before copying source so dependencies are cached across builds |
| **Non-root user** | The container runs as `appuser` for security |
| **No secrets baked in** | All secrets are injected via environment variables at **runtime** (`docker run -e …`) |

---

## 5. GitHub Actions Workflows

### 5.1 CI Workflow (`maven.yml`) – Existing

Compiles the project and runs unit + integration tests on every push/PR to
`main`. **No changes required** – it stays as-is.

### 5.2 Deploy Workflow (`deploy.yml`) – New

Triggered **automatically** when the CI workflow completes successfully on
`main` (via `workflow_run`).

**Job 1 – `build-and-push`**

1. Checks out the code.
2. Logs in to Docker Hub using `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` secrets.
3. Builds the Docker image with BuildKit and layer caching.
4. Pushes two tags: `latest` and the commit SHA (`${{ github.sha }}`).

**Job 2 – `deploy`**

1. SSHes into the Droplet using `appleboy/ssh-action`.
2. Pulls the newly pushed image.
3. Stops and removes the old container.
4. Runs a new container with all production secrets injected as environment
   variables (`-e SPRING_PROFILES_ACTIVE=prod -e DATA_SOURCE=… …`).
5. Prunes old Docker images to save disk space.

---

## 6. Prepare the DigitalOcean Droplet

### 6.1 SSH Access

Generate a dedicated deploy key (if you haven't already):

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key -N ""
```

- Copy the **public** key to the Droplet:

  ```bash
  ssh-copy-id -i ~/.ssh/deploy_key.pub root@<DROPLET_IP>
  ```

- Copy the **private** key contents into the GitHub secret `DROPLET_SSH_KEY`:

  ```bash
  cat ~/.ssh/deploy_key
  # Copy everything (including the BEGIN/END lines) into GitHub
  ```

### 6.2 Install Docker on the Droplet

```bash
ssh root@<DROPLET_IP>

# Update packages
apt-get update && apt-get upgrade -y

# Install Docker (official convenience script)
curl -fsSL https://get.docker.com | sh

# Verify
docker --version
```

### 6.3 (Optional) Configure a Firewall

```bash
ufw allow OpenSSH
ufw allow 8080/tcp    # Application port
ufw enable
```

### 6.4 (Optional) Log In to Docker Hub on the Droplet

If your Docker Hub image is **private**, pre-authenticate on the Droplet:

```bash
docker login -u <DOCKERHUB_USERNAME>
```

Or let the GitHub Actions workflow handle it by adding a `docker login` step in
the SSH script.

---

## 7. End-to-End Flow

1. **Developer** pushes code to the `main` branch.
2. **CI workflow** (`maven.yml`) runs: compile → unit tests → integration tests.
3. If CI **passes**, the **Deploy workflow** (`deploy.yml`) is triggered.
4. GitHub Actions **builds** a Docker image from the `Dockerfile`.
5. The image is **pushed** to Docker Hub as `<username>/lms-platform:latest`.
6. GitHub Actions **SSHes** into the Droplet.
7. The Droplet **pulls** the new image and **starts** a container with
   production environment variables injected at runtime.
8. The LMS API is available at `http://<DROPLET_IP>:8080`.

---

## 8. Troubleshooting

| Problem | Solution |
|---|---|
| Deploy workflow never triggers | Make sure CI workflow name in `deploy.yml` exactly matches `"Java CI with Maven"` and CI passes on `main` |
| Docker push fails | Verify `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets are set correctly |
| SSH connection refused | Check `DROPLET_HOST`, `DROPLET_USERNAME`, and `DROPLET_SSH_KEY` secrets; ensure the Droplet firewall allows port 22 |
| Container starts but app crashes | SSH into Droplet and run `docker logs lms-platform` to inspect Spring Boot logs |
| Database connection error | Confirm `DATA_SOURCE`, `DB_USERNAME`, `DB_PASSWORD` secrets contain the correct production values and the DB allows connections from the Droplet IP |
| File uploads fail | Verify `REGION`, `DO_END_POINT`, `DO_ACCESS_KEY`, `DO_SECRET_KEY`, `DO_BUCKET` are correct and the Spaces bucket exists |
| Port already in use | Run `docker ps` on the Droplet to check for lingering containers; stop them with `docker stop <id>` |

---

*Last Updated: 2026-03-08*
