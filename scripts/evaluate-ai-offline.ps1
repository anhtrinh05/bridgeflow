[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $repoRoot
try {
    & (Join-Path $PSScriptRoot 'test-backend.ps1')
    if ($LASTEXITCODE -ne 0) {
        throw "Backend evidence generation failed."
    }
    & node scripts/evaluate-ai-offline.mjs
    if ($LASTEXITCODE -ne 0) {
        throw "Offline AI evaluation failed."
    }
} finally {
    Pop-Location
}
