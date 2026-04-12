# Full DocuRAG reactor build + E2E (same behaviour as full-build-and-e2e.sh).
# Usage:
#   .\scripts\full-build-and-e2e.ps1
#   .\scripts\full-build-and-e2e.ps1 -TeardownVolumes

param(
    [switch] $TeardownVolumes
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ComposeDir = Join-Path $RepoRoot "docu-rag"
$ParentPom = Join-Path $RepoRoot "docu-rag-parent\pom.xml"
$E2eTarget = Join-Path $RepoRoot "docu-rag-e2e\target"
$E2eCucumber = Join-Path $RepoRoot "docu-rag-e2e\build\cucumber-reports"

function Invoke-ComposeDownV {
    Write-Host ">>> Compose down (remove volumes) in $ComposeDir"
    if (Test-Path $ComposeDir) {
        Push-Location $ComposeDir
        try {
            & docker compose down --remove-orphans -v 2>$null
        } catch { }
        try {
            & docker-compose down --remove-orphans -v 2>$null
        } catch { }
        Pop-Location
    }
}

function Show-E2eReport {
    Write-Host ""
    Write-Host "================ E2E / TEST REPORTS ================"
    $sf = Join-Path $E2eTarget "surefire-reports"
    if (Test-Path $sf) {
        Write-Host "--- Surefire ($sf) ---"
        Get-ChildItem $sf -File | Where-Object { $_.Extension -in ".txt", ".xml" } | ForEach-Object { Write-Host "  $($_.Name)" }
        $summary = Get-ChildItem $sf -Filter "*.txt" -File | Select-Object -First 1
        if ($summary) {
            Write-Host "--- First Surefire summary (tail) ---"
            Get-Content $summary.FullName -Tail 40
        }
    } else {
        Write-Host "No surefire-reports at $sf (build may have failed early)."
    }
    $html = Join-Path $E2eCucumber "e2e-report.html"
    if (Test-Path $html) {
        Write-Host "--- Cucumber HTML ---"
        Write-Host "  $html"
    }
    $junit = Join-Path $E2eCucumber "cucumber.xml"
    if (Test-Path $junit) {
        Write-Host "--- Cucumber JUnit XML ---"
        Write-Host "  $junit"
    }
    $clog = Join-Path $RepoRoot "docu-rag-e2e\target\docurag-e2e-compose.log"
    $dlog = Join-Path $RepoRoot "docu-rag-e2e\target\docurag-e2e-docker-info.log"
    if (Test-Path $clog) {
        Write-Host "--- Docker compose log (tail) ---"
        Get-Content $clog -Tail 25 -ErrorAction SilentlyContinue
    }
    if (Test-Path $dlog) {
        Write-Host "--- docker info log (tail) ---"
        Get-Content $dlog -Tail 15 -ErrorAction SilentlyContinue
    }
    Write-Host "======================================================"
}

if ($TeardownVolumes) {
    Invoke-ComposeDownV
}

$mvnArgs = @("-f", $ParentPom, "clean", "verify")
if ($TeardownVolumes) {
    $mvnArgs += "-Pe2e-teardown-volumes"
}

Write-Host ">>> mvn $($mvnArgs -join ' ')"
$mvnExit = 1
Push-Location $RepoRoot
try {
    & mvn @mvnArgs
    if ($null -ne $LASTEXITCODE) { $mvnExit = $LASTEXITCODE } else { $mvnExit = 0 }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)"
    $mvnExit = 1
} finally {
    Pop-Location
}

Show-E2eReport

if ($TeardownVolumes) {
    Invoke-ComposeDownV
}

Write-Host ""
if ($mvnExit -eq 0) {
    Write-Host "STATUS: SUCCESS (mvn exit 0)"
} else {
    Write-Host "STATUS: FAILED (mvn exit $mvnExit) - fix failures above and re-run."
}
exit $mvnExit
