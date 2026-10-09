[CmdletBinding()]
param(
    [string]$OutputRoot = "",
    [string]$EnvFile = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$composeFile = Join-Path $repoRoot 'compose.production.yaml'
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path $repoRoot '.env.production' }
if ([string]::IsNullOrWhiteSpace($OutputRoot)) { $OutputRoot = Join-Path $repoRoot 'backups' }
if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) { throw "Production env file not found: $EnvFile" }

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDirectory = Join-Path ([System.IO.Path]::GetFullPath($OutputRoot)) $timestamp
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$documentsDirectory = Join-Path $backupDirectory 'documents'
New-Item -ItemType Directory -Path $documentsDirectory -Force | Out-Null
$compose = @('compose', '--env-file', $EnvFile, '-f', $composeFile)

function Invoke-Docker([string[]]$Arguments) {
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "docker command failed with exit code $LASTEXITCODE" }
}

$databaseContainer = (& docker @compose ps -q postgres).Trim()
$backendContainer = (& docker @compose ps -a -q backend).Trim()
if ([string]::IsNullOrWhiteSpace($databaseContainer)) { throw 'Production postgres container is not running.' }
if ([string]::IsNullOrWhiteSpace($backendContainer)) { throw 'Production backend container has not been created.' }
$backendWasRunning = ((& docker inspect -f '{{.State.Running}}' $backendContainer).Trim() -eq 'true')

try {
    if ($backendWasRunning) { Invoke-Docker -Arguments ($compose + @('stop', 'backend')) }
    Invoke-Docker -Arguments ($compose + @(
        'exec', '-T', 'postgres', 'sh', '-c',
        'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump --clean --if-exists --create --no-owner --no-privileges --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --file /tmp/bridgeflow-backup.sql'
    ))
    Invoke-Docker -Arguments @('cp', "${databaseContainer}:/tmp/bridgeflow-backup.sql", (Join-Path $backupDirectory 'database.sql'))
    Invoke-Docker -Arguments @('exec', $databaseContainer, 'rm', '-f', '/tmp/bridgeflow-backup.sql')
    Invoke-Docker -Arguments @('cp', "${backendContainer}:/var/lib/bridgeflow/documents/.", $documentsDirectory)

    $databaseFile = Join-Path $backupDirectory 'database.sql'
    $manifest = [ordered]@{
        schemaVersion = 1
        createdAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
        databaseSha256 = (Get-FileHash -LiteralPath $databaseFile -Algorithm SHA256).Hash.ToLowerInvariant()
        documentFileCount = @(Get-ChildItem -LiteralPath $documentsDirectory -Recurse -File).Count
    }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backupDirectory 'manifest.json') -Encoding utf8
    Write-Output "Backup completed: $backupDirectory"
} finally {
    if ($backendWasRunning) { Invoke-Docker -Arguments ($compose + @('start', 'backend')) }
}
