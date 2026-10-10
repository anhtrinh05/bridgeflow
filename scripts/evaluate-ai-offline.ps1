[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $repoRoot
try {
    & node scripts/evaluate-ai-offline.mjs
    if ($LASTEXITCODE -ne 0) {
        throw "Offline AI evaluation failed."
    }
} finally {
    Pop-Location
}
