# Controlled Judge0 CE for Code Runner

This stack pins Judge0 CE `1.13.1`, exposes it only on `127.0.0.1:2358`, and requires
the `X-Judge0-Token` header.

```powershell
cd infrastructure\judge0
docker compose up -d
$env:JUDGE0_AUTH_TOKEN='lms-local-judge0-token'
```

The local default token is for development only. Override `JUDGE0_AUTH_TOKEN`,
`JUDGE0_REDIS_PASSWORD`, and `JUDGE0_DB_PASSWORD` for a shared deployment.

Docker Desktop uses cgroup v2 while the `isolate` bundled with Judge0 1.13.1 expects
cgroup v1 for aggregate limits. The LMS adapter therefore asks Judge0 to enforce CPU
and memory limits per process/thread (RLIMIT), while Judge0 still provides filesystem,
process, network, output, wall-time, and compiler isolation.

Verification gate:

```powershell
.\mvnw.cmd -Dtest=Judge0CodeRunnerLiveIT test
.\mvnw.cmd -Dtest=StudentCodeRunnerAttemptLiveIT test
```
