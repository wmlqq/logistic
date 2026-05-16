# Initialize MySQL database and import seed data.
# Usage: .\scripts\init-db.ps1 -User root -Password your_password

param(
    [string]$User = "root",
    [string]$Password = "",
    [string]$Database = "mylogistic",
    [string]$SeedFile = "database/seed/backup.sql"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$seedPath = Join-Path $root $SeedFile

if (-not (Test-Path $seedPath)) {
    Write-Error "Seed file not found: $seedPath"
}

$createDb = "CREATE DATABASE IF NOT EXISTS $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Write-Host "Creating database '$Database'..."
if ($Password) {
    mysql -u $User "-p$Password" -e $createDb
    mysql -u $User "-p$Password" $Database -e "source $seedPath"
} else {
    mysql -u $User -e $createDb
    Get-Content $seedPath | mysql -u $User $Database
}

Write-Host "Done. Default admin: admin / admin"
