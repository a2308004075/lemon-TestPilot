# Start TestPilot backend (Spring Boot 2.7.18, port 3344)
# Requires: JDK 8+, Maven, local MySQL on 127.0.0.1:3306
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root 'backend')

# First-run bootstrap: create application-local.yml from the example
$localCfg = Join-Path $root 'backend\src\main\resources\application-local.yml'
$example = Join-Path $root 'backend\src\main\resources\application-local.yml.example'
if (-not (Test-Path $localCfg)) {
    Copy-Item $example $localCfg
    Write-Output '[Hint] application-local.yml created from example.'
    Write-Output '[Hint] Edit backend/src/main/resources/application-local.yml and set your MySQL password, then restart.'
}

Write-Output 'Starting backend on port 3344 (Flyway will auto-create database lemon_testpilot on first run)...'
mvn spring-boot:run
