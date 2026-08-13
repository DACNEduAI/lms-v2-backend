$ErrorActionPreference = 'Stop'

function Assert-NoMatch([string]$Pattern, [string[]]$Paths, [string]$Message) {
    $matches = rg -n --pcre2 $Pattern @Paths
    if ($LASTEXITCODE -eq 0) {
        $matches
        throw $Message
    }
    if ($LASTEXITCODE -gt 1) {
        throw "rg failed while checking: $Message"
    }
}

# The schema snapshot must never introduce a timezone-less instant.
Assert-NoMatch '\btimestamp\s+without\s+time\s+zone\b' @('src/main/resources/schema.sql') `
    'schema.sql contains TIMESTAMP WITHOUT TIME ZONE'

# Direct wall-clock calls and machine-zone scheduling reintroduce the exact
# UTC+7 timeout bug that this migration removes.
Assert-NoMatch 'LocalDateTime\.now\(\)' @('src/main/java') `
    'Use LocalDateTime.now(Clock.systemUTC()) during the legacy migration, or Instant with an injected UTC Clock'
Assert-NoMatch 'ZoneId\.systemDefault\(\)' @('src/main/java') `
    'Quartz scheduling must not use the machine timezone'

Write-Host 'UTC time-standard checks passed.'
