[CmdletBinding()]
param(
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeDirectory = Join-Path $repoRoot '.tooling\demo-tunnel'
$envFile = Join-Path $runtimeDirectory '.env.demo'
$projectName = 'bridgeflow-demo'
$composeFiles = @(
    '-f', (Join-Path $repoRoot 'compose.production.yaml'),
    '-f', (Join-Path $repoRoot 'compose.demo-tunnel.yaml')
)

function Invoke-Docker {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed with exit code $LASTEXITCODE."
    }
}

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return (($bytes | ForEach-Object { $_.ToString('x2') }) -join '')
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is required for the on-demand demo.'
}

New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $envFile)) {
    $template = Get-Content -LiteralPath (Join-Path $repoRoot '.env.demo.example')
    $password = New-RandomSecret
    $resolved = $template | ForEach-Object {
        if ($_ -like 'POSTGRES_PASSWORD=*') { "POSTGRES_PASSWORD=$password" } else { $_ }
    }
    [System.IO.File]::WriteAllLines(
        $envFile,
        [string[]]$resolved,
        [System.Text.UTF8Encoding]::new($false)
    )
}

$compose = @(
    'compose', '--project-name', $projectName,
    '--env-file', $envFile
) + $composeFiles + @('--profile', 'tunnel')

Push-Location $repoRoot
try {
    $up = $compose + @('up', '--detach')
    if (-not $NoBuild) { $up += '--build' }
    Invoke-Docker -Arguments $up

    $publicUrl = $null
    for ($attempt = 0; $attempt -lt 60 -and -not $publicUrl; $attempt++) {
        $logArguments = $compose + @('logs', '--no-color', 'tunnel')
        $logs = (& docker @logArguments 2>&1 | Out-String)
        $match = [regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($match.Success) {
            $publicUrl = $match.Value
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $publicUrl) {
        throw 'Cloudflare Quick Tunnel did not publish a URL within 120 seconds.'
    }

    Write-Output ''
    Write-Output 'BridgeFlow on-demand demo is running.'
    Write-Output "Public URL: $publicUrl"
    Write-Output 'The URL is temporary and remains reachable only while Docker and the tunnel are running.'
    Write-Output 'Stop it with: .\scripts\stop-demo-tunnel.ps1'
} catch {
    $downArguments = $compose + @('down', '--remove-orphans')
    & docker @downArguments | Out-Null
    throw
} finally {
    Pop-Location
}
