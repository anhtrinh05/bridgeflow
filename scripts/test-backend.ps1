param(
    [string]$JavaHome,
    [string]$MavenCommand,
    [string]$PostgresBin,
    # Keep the temporary cluster directory even when verification succeeds.
    [switch]$KeepCluster
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $JavaHome) {
    $JavaHome = $env:JAVA_HOME
    if (-not $JavaHome) { $JavaHome = Join-Path $env:USERPROFILE '.jdks\temurin-21.0.12.1' }
}
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
    throw 'JDK not found. Pass -JavaHome with the path to your JDK 21.'
}
if (-not $MavenCommand) {
    $portableMaven = Join-Path $repoRoot '.tooling\apache-maven-3.10.0\bin\mvn.cmd'
    if (Test-Path -LiteralPath $portableMaven) { $MavenCommand = $portableMaven }
    else { $MavenCommand = (Get-Command mvn.cmd -ErrorAction Stop).Source }
}
if (-not $PostgresBin) {
    $postgresRoot = Join-Path $env:ProgramFiles 'PostgreSQL'
    if (Test-Path -LiteralPath $postgresRoot) {
        $PostgresBin = Get-ChildItem -LiteralPath $postgresRoot -Directory |
            Where-Object { $_.Name -match '^\d+(\.\d+)?$' } |
            Sort-Object { [version]($_.Name + '.0') } -Descending |
            ForEach-Object { Join-Path $_.FullName 'bin' } |
            Where-Object { Test-Path -LiteralPath (Join-Path $_ 'initdb.exe') } |
            Select-Object -First 1
    }
}
foreach ($executable in @('initdb.exe', 'pg_ctl.exe', 'createdb.exe')) {
    if (-not $PostgresBin -or -not (Test-Path -LiteralPath (Join-Path $PostgresBin $executable))) {
        throw "PostgreSQL executable $executable not found. Pass -PostgresBin with the installed bin directory."
    }
}
$initdb = Join-Path $PostgresBin 'initdb.exe'
$pgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
$createdb = Join-Path $PostgresBin 'createdb.exe'

# Each invocation owns a new cluster and never connects to existing servers or databases.
$runDirectory = Join-Path $repoRoot ('.tooling\postgres-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDirectory | Out-Null
$dataDirectory = Join-Path $runDirectory 'data'
$postgresLog = Join-Path $runDirectory 'postgres.log'
$listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()

$variableNames = @('JAVA_HOME', 'MAVEN_SKIP_RC', 'BRIDGEFLOW_TEST_DB_URL',
    'BRIDGEFLOW_TEST_DB_USERNAME', 'BRIDGEFLOW_TEST_DB_PASSWORD')
$previousValues = @{}
foreach ($name in $variableNames) { $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }

$succeeded = $false
try {
    Write-Host "Initializing disposable PostgreSQL cluster in $dataDirectory"
    & $initdb -D $dataDirectory -U bridgeflow --encoding=UTF8 --no-locale `
        --auth-local=trust --auth-host=trust --no-sync --no-instructions
    if ($LASTEXITCODE -ne 0) { throw 'Failed to initialize the isolated PostgreSQL cluster.' }

    # Listen on loopback only. Durability settings are relaxed because the cluster is thrown away.
    $serverOptions = "-h 127.0.0.1 -p $port -c fsync=off -c synchronous_commit=off -c full_page_writes=off"
    & $pgCtl -D $dataDirectory -l $postgresLog -o $serverOptions -w -t 60 start
    if ($LASTEXITCODE -ne 0) { throw "Failed to start PostgreSQL. See $postgresLog" }

    & $createdb -h 127.0.0.1 -p $port -U bridgeflow bridgeflow_test
    if ($LASTEXITCODE -ne 0) { throw 'Failed to create the test database.' }

    $env:JAVA_HOME = $JavaHome
    $env:MAVEN_SKIP_RC = 'true'
    $env:BRIDGEFLOW_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$port/bridgeflow_test"
    $env:BRIDGEFLOW_TEST_DB_USERNAME = 'bridgeflow'
    $env:BRIDGEFLOW_TEST_DB_PASSWORD = ''
    & $MavenCommand --batch-mode --no-transfer-progress "-Dmaven.repo.local=$repoRoot\.m2\repository" -f "$repoRoot\backend\pom.xml" clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed. See backend/target/surefire-reports and the Maven output.' }
    $succeeded = $true
} finally {
    # Stop the server whenever it left a pid file, even if pg_ctl start reported a failure.
    if (Test-Path -LiteralPath (Join-Path $dataDirectory 'postmaster.pid')) {
        & $pgCtl -D $dataDirectory -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning "Could not stop test PostgreSQL. Cluster: $dataDirectory" }
    }
    foreach ($name in $variableNames) { [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process') }

    $serverStillRunning = Test-Path -LiteralPath (Join-Path $dataDirectory 'postmaster.pid')
    if ($succeeded -and -not $KeepCluster -and -not $serverStillRunning) {
        Remove-Item -LiteralPath $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host 'Temporary PostgreSQL cluster stopped and removed.'
    } else {
        Write-Host "Test cluster and logs retained at $runDirectory"
    }
}
