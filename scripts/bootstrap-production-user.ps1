[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Email,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$DisplayName,

    [string]$PasswordFile = "",
    [string]$EnvFile = "",
    [string]$ProjectName = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$composeFile = Join-Path $repoRoot 'compose.production.yaml'
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path $repoRoot '.env.production' }
if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) { throw "Production env file not found: $EnvFile" }

$temporaryPasswordFile = $null
$plainPassword = $null

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

try {
    if ([string]::IsNullOrWhiteSpace($PasswordFile)) {
        $first = Read-Host 'Initial production user password (16-128 characters)' -AsSecureString
        $second = Read-Host 'Confirm password' -AsSecureString
        $plainPassword = ConvertTo-PlainText $first
        $confirmation = ConvertTo-PlainText $second
        if ($plainPassword -cne $confirmation) { throw 'Password confirmation does not match.' }
        $confirmation = $null

        $temporaryPasswordFile = [IO.Path]::GetTempFileName()
        [IO.File]::WriteAllText(
            $temporaryPasswordFile,
            $plainPassword,
            [Text.UTF8Encoding]::new($false)
        )
        $resolvedPasswordFile = $temporaryPasswordFile
    } else {
        $resolvedPasswordFile = (Resolve-Path -LiteralPath $PasswordFile).Path
        if (-not (Test-Path -LiteralPath $resolvedPasswordFile -PathType Leaf)) {
            throw "Password file not found: $resolvedPasswordFile"
        }
        $plainPassword = [IO.File]::ReadAllText($resolvedPasswordFile, [Text.Encoding]::UTF8).Trim()
    }

    if ($plainPassword.Length -lt 16 -or $plainPassword.Length -gt 128) {
        throw 'Password must contain between 16 and 128 characters.'
    }
    if ($plainPassword.Contains("`n") -or $plainPassword.Contains("`r")) {
        throw 'Password file must contain exactly one line.'
    }

    $compose = @('compose')
    if (-not [string]::IsNullOrWhiteSpace($ProjectName)) {
        $compose += @('--project-name', $ProjectName.Trim())
    }
    $compose += @('--env-file', $EnvFile, '-f', $composeFile)
    $databaseContainer = (& docker @compose ps -q postgres).Trim()
    if ([string]::IsNullOrWhiteSpace($databaseContainer)) {
        throw 'Production postgres container is not running.'
    }
    $databaseHealth = (& docker inspect -f '{{.State.Health.Status}}' $databaseContainer).Trim()
    if ($databaseHealth -ne 'healthy') {
        throw "Production postgres container is not healthy: $databaseHealth"
    }

    $secretMount = "${resolvedPasswordFile}:/run/secrets/bridgeflow-bootstrap-password:ro"
    & docker @compose run --rm --no-deps `
        -e 'BRIDGEFLOW_BOOTSTRAP_ENABLED=true' `
        -e "BRIDGEFLOW_BOOTSTRAP_EMAIL=$($Email.Trim())" `
        -e "BRIDGEFLOW_BOOTSTRAP_DISPLAY_NAME=$($DisplayName.Trim())" `
        -e 'BRIDGEFLOW_BOOTSTRAP_PASSWORD_FILE=/run/secrets/bridgeflow-bootstrap-password' `
        -v $secretMount `
        backend --spring.main.web-application-type=none
    if ($LASTEXITCODE -ne 0) { throw "Production user bootstrap failed with exit code $LASTEXITCODE." }

    Write-Output 'Initial production user created. Remove any operator-managed password file and authenticate through the normal login endpoint.'
} finally {
    $plainPassword = $null
    if ($temporaryPasswordFile -and (Test-Path -LiteralPath $temporaryPasswordFile)) {
        Remove-Item -LiteralPath $temporaryPasswordFile -Force
    }
}
