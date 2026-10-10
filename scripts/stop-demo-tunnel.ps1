[CmdletBinding()]
param(
    [switch]$RemoveData
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeDirectory = Join-Path $repoRoot '.tooling\demo-tunnel'
$envFile = Join-Path $runtimeDirectory '.env.demo'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'The demo runtime file does not exist; no managed demo stack was found.'
}

$compose = @(
    'compose', '--project-name', 'bridgeflow-demo',
    '--env-file', $envFile,
    '-f', (Join-Path $repoRoot 'compose.production.yaml'),
    '-f', (Join-Path $repoRoot 'compose.demo-tunnel.yaml'),
    '--profile', 'tunnel',
    'down', '--remove-orphans'
)
if ($RemoveData) { $compose += '--volumes' }

Push-Location $repoRoot
try {
    & docker @compose
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose cleanup failed with exit code $LASTEXITCODE."
    }
    if ($RemoveData) {
        Remove-Item -LiteralPath $envFile -Force
        Write-Output 'Demo containers, networks, volumes, and generated environment file were removed.'
    } else {
        Write-Output 'Demo containers and networks were removed. Local demo data remains for the next run.'
    }
} finally {
    Pop-Location
}
