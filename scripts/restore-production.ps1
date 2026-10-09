[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BackupDirectory,
    [Parameter(Mandatory = $true)][ValidateSet('RESTORE')][string]$Confirmation,
    [string]$EnvFile = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$composeFile = Join-Path $repoRoot 'compose.production.yaml'
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path $repoRoot '.env.production' }
$resolvedBackup = (Resolve-Path -LiteralPath $BackupDirectory).Path
$databaseFile = Join-Path $resolvedBackup 'database.sql'
$documentsDirectory = Join-Path $resolvedBackup 'documents'
$manifestFile = Join-Path $resolvedBackup 'manifest.json'
foreach ($required in @($EnvFile, $databaseFile, $documentsDirectory, $manifestFile)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required restore input not found: $required" }
}

$manifest = Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json
$actualHash = (Get-FileHash -LiteralPath $databaseFile -Algorithm SHA256).Hash.ToLowerInvariant()
if ($manifest.schemaVersion -ne 1) { throw "Unsupported backup manifest version: $($manifest.schemaVersion)" }
if ($actualHash -ne $manifest.databaseSha256) { throw 'Database backup SHA-256 does not match manifest.' }

$compose = @('compose', '--env-file', $EnvFile, '-f', $composeFile)
function Invoke-Docker([string[]]$Arguments) {
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "docker command failed with exit code $LASTEXITCODE" }
}

$databaseContainer = (& docker @compose ps -q postgres).Trim()
$backendContainer = (& docker @compose ps -a -q backend).Trim()
if ([string]::IsNullOrWhiteSpace($databaseContainer)) { throw 'Production postgres container is not running.' }
if ([string]::IsNullOrWhiteSpace($backendContainer)) { throw 'Production backend container has not been created.' }

Invoke-Docker -Arguments ($compose + @('stop', 'backend'))
Invoke-Docker -Arguments @('cp', $databaseFile, "${databaseContainer}:/tmp/bridgeflow-restore.sql")
Invoke-Docker -Arguments ($compose + @(
    'exec', '-T', 'postgres', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql --set ON_ERROR_STOP=on --username "$POSTGRES_USER" --dbname postgres --file /tmp/bridgeflow-restore.sql'
))
Invoke-Docker -Arguments @('exec', $databaseContainer, 'rm', '-f', '/tmp/bridgeflow-restore.sql')
$restoreMount = "${documentsDirectory}:/restore:ro"
Invoke-Docker -Arguments ($compose + @(
    'run', '--rm', '--no-deps', '--entrypoint', 'sh', '-v', $restoreMount, 'backend', '-c',
    'find /var/lib/bridgeflow/documents -mindepth 1 -delete && cp -R /restore/. /var/lib/bridgeflow/documents/'
))
Invoke-Docker -Arguments ($compose + @('start', 'backend'))

for ($attempt = 1; $attempt -le 30; $attempt++) {
    & docker @compose exec -T backend curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Output "Restore completed and readiness passed: $resolvedBackup"
        exit 0
    }
    Start-Sleep -Seconds 2
}
throw 'Restore completed, but backend readiness did not pass within 60 seconds.'
