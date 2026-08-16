param (
    [switch]$InitSchema,
    [switch]$Migrate,
    [switch]$SeedData,
    [string]$DbUrl = "postgresql://postgres:postgres@localhost:5432/lms"
)

if (-not $InitSchema -and -not $Migrate -and -not $SeedData) {
    Write-Host "LMS Database Setup Script"
    Write-Host "Usage: .\setup-db.ps1 [-InitSchema] [-Migrate] [-SeedData] [-DbUrl <connection_string>]"
    Write-Host "Example: .\setup-db.ps1 -InitSchema -SeedData"
    Write-Host "Default DbUrl: $DbUrl"
    exit
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
    Write-Host "Seeding data fixture..."
    Invoke-Psql "src/main/resources/data.sql"
}

Write-Host "Setup complete!"
