param (
    [switch]$InitSchema,
    [switch]$Migrate,
    [switch]$SeedData,
    [switch]$RecreateFixture,
    [string]$DbUrl = "postgresql://postgres:postgres@localhost:5432/lms"
)

if (-not $InitSchema -and -not $Migrate -and -not $SeedData -and -not $RecreateFixture) {
    Write-Host "LMS Database Setup Script"
    Write-Host "Usage: .\setup-db.ps1 [-InitSchema] [-Migrate] [-SeedData] [-RecreateFixture] [-DbUrl <connection_string>]"
    Write-Host "Example: .\setup-db.ps1 -RecreateFixture"
    Write-Host "Default DbUrl: $DbUrl"
    exit
}

function Invoke-PsqlCommand {
    param([string]$Command)
    psql -v ON_ERROR_STOP=1 $DbUrl -c $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Database command failed. Setup aborted."
        exit $LASTEXITCODE
    }
}

function Invoke-Psql {
    param([string]$File)
    Write-Host "Executing $File..."
    psql -v ON_ERROR_STOP=1 $DbUrl -f $File
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to execute $File. Setup aborted."
        exit $LASTEXITCODE
    }
}

if ($RecreateFixture) {
    Write-Warning "Recreating the fixture drops the edu and ai_index schemas in the target database."
    Invoke-PsqlCommand "DROP SCHEMA IF EXISTS edu CASCADE; DROP SCHEMA IF EXISTS ai_index CASCADE;"
    $InitSchema = $true
    $Migrate = $true
    $SeedData = $true
}

if ($InitSchema) {
    Write-Host "Initializing schema..."
    Invoke-Psql "src/main/resources/schema.sql"
}

if ($Migrate) {
    Write-Host "Running manual migrations..."
    foreach ($file in Get-ChildItem "src/main/resources/db/manual" -Filter "V*.sql" | Sort-Object Name) {
        Invoke-Psql $file.FullName
    }
}

if ($SeedData) {
    $fixturePresent = psql -v ON_ERROR_STOP=1 $DbUrl -tAc "SELECT EXISTS (SELECT 1 FROM edu.users WHERE id = '11111111-1111-1111-1111-111111111111')"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Could not check existing fixture. Setup aborted."
        exit $LASTEXITCODE
    }
    if ($fixturePresent.Trim() -eq 't') {
        Write-Host "Seed fixture already exists; no changes made. Use -RecreateFixture to replace it."
    } else {
        Write-Host "Seeding data fixture..."
        Invoke-Psql "src/main/resources/data.sql"
    }

    $invalidPublishedCodeRunner = psql -v ON_ERROR_STOP=1 $DbUrl -tAc @"
SELECT COUNT(*)
FROM edu.questions q
JOIN edu.quizzes quiz ON quiz.id = q.quiz_id
LEFT JOIN edu.quiz_releases release ON release.quiz_id = quiz.id AND release.active
LEFT JOIN edu.quiz_release_code_runner_bindings binding
       ON binding.quiz_release_id = release.id AND binding.question_id = q.id
LEFT JOIN edu.code_runner_configs config ON config.id = binding.code_runner_config_id
WHERE q.type = 'CODE_RUNNER'
  AND quiz.lifecycle_status = 'PUBLISHED'
  AND (release.id IS NULL OR binding.code_runner_config_id IS NULL
       OR config.contract_status <> 'VERIFIED_EXECUTION_GOLD');
"@
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Could not validate the Code Runner fixture. Setup aborted."
        exit $LASTEXITCODE
    }
    if ([int]$invalidPublishedCodeRunner.Trim() -ne 0) {
        Write-Error "Fixture contains a published Code Runner question without an active VERIFIED_EXECUTION_GOLD binding."
        exit 1
    }
    Write-Host "Code Runner fixture validation passed."
}

Write-Host "Setup complete!"
