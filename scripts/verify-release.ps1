[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $repoRoot
try {
    & cmd.exe /d /c scripts\test-backend.cmd
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
    & npm.cmd run lint
    if ($LASTEXITCODE -ne 0) { throw 'Frontend lint failed.' }
    & npm.cmd exec tsc -- --noEmit
    if ($LASTEXITCODE -ne 0) { throw 'Frontend type-check failed.' }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend production build failed.' }
    & npm.cmd run ops:validate
    if ($LASTEXITCODE -ne 0) { throw 'Production Compose semantic validation failed.' }
    foreach ($script in @(
        'scripts/backup-production.ps1',
        'scripts/restore-production.ps1',
        'scripts/verify-release.ps1'
    )) {
        $tokens = $null
        $parseErrors = $null
        [System.Management.Automation.Language.Parser]::ParseFile(
            (Resolve-Path $script), [ref]$tokens, [ref]$parseErrors
        ) | Out-Null
        if ($parseErrors.Count -gt 0) {
            throw "PowerShell syntax validation failed for $script`: $($parseErrors[0].Message)"
        }
    }
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        & docker compose --env-file .env.production.example -f compose.production.yaml config --quiet
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose runtime validation failed.' }
    } else {
        Write-Warning 'Docker is unavailable; run Docker Compose config/build validation on the deployment host.'
    }
    Write-Output 'RELEASE VERIFICATION PASSED'
} finally {
    Pop-Location
}
