<#
    reset-passwords.ps1
    Sinh BCrypt hash (+ prefix {bcrypt}) va UPDATE toan bo bang users.
    Dung de chay ngay sau khi import database/HT_QLDT.sql.
#>

[CmdletBinding()]
param(
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $project 'pom.xml'))) {
    throw "Khong tim thay pom.xml. Script phai dat trong projectRoot/scripts/"
}

$m2 = "$env:USERPROFILE\.m2\repository"
$crypto = Get-ChildItem "$m2\org\springframework\security\spring-security-crypto" -Recurse -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*sources*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

$mysql = Get-ChildItem "$m2\com\mysql\mysql-connector-j" -Recurse -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*sources*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

$log = Get-ChildItem "$m2\commons-logging\commons-logging" -Recurse -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*sources*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if (-not ($crypto -and $mysql -and $log)) {
    throw "Khong tim thay spring-security-crypto / mysql-connector-j / commons-logging trong $m2"
}

$toolsDir = "$project\tools"
$outDir = "$toolsDir\out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host "[1/3] Compiling ResetPasswords..." -ForegroundColor Cyan
javac -cp "$crypto;$mysql;$log" -d "$outDir" "$toolsDir\ResetPasswords.java"
if ($LASTEXITCODE -ne 0) { throw "Compile that bai" }

Write-Host "[2/3] Updating password_hash cho tat ca user (password = '$Password')..." -ForegroundColor Cyan
java -cp "$crypto;$mysql;$log;$outDir" ResetPasswords "$Password"
if ($LASTEXITCODE -ne 0) { throw "Run that bai" }

Write-Host "[3/3] DONE - dang nhap admin / $Password" -ForegroundColor Green
