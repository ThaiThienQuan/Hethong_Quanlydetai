<#
    reset-demo-data.ps1
    Nap lai SCHEMA + DU LIEU DEMO vao MySQL, sinh lai mat khau BCrypt cho tat ca tai khoan.

    YEU cau: MySQL 8 client (mysql.exe) co trong PATH hoac tro san.
    Mac dinh: DB_URL = jdbc:mysql://127.0.0.1:3306/student_topic_management
               DB_USERNAME = root, DB_PASSWORD = 123456

    Luu y: script se XOA TOAN BO du lieu trong database. Chi dung tren database demo.
#>
[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "student_topic_management",
    [string]$DbUser = "root",
    [string]$DbPassword = "123456",
    [string]$Password = "123456",
    [switch]$Drop,
    [switch]$SkipSchema,
    [switch]$SkipPassword
)

$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$sqlFile = Join-Path $project "database\HT_QLDT.sql"

function Get-MySqlExe {
    $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe"
    )
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    throw "Khong tim thay mysql.exe. Cai MySQL Client hoac them vao PATH."
}

$mysql = Get-MySqlExe
Write-Host "MySQL client: $mysql" -ForegroundColor Cyan

# 1) Import schema + du lieu mau
if (-not $SkipSchema) {
    if ($Drop) {
        Write-Host "[1/3] DROP DATABASE $DbName ..." -ForegroundColor Cyan
        $dropCmd = '"' + $mysql + '" -h ' + $DbHost + ' -P ' + $DbPort + ' -u ' + $DbUser +
            ' -p' + $DbPassword + ' -e "DROP DATABASE IF EXISTS ' + $DbName + ';" 2>&1'
        $null = cmd /c $dropCmd
        if ($LASTEXITCODE -ne 0) { throw "Khong xoa duoc database $DbName" }
    }
    Write-Host "[1/3] Import $sqlFile ..." -ForegroundColor Cyan
    # mysql.exe khong doc duoc -InputFile voi ky tu dac biet; dung cmd /c de chuyen doi redirect.
    $cmdLine = '"' + $mysql + '" -h ' + $DbHost + ' -P ' + $DbPort + ' -u ' + $DbUser +
        ' -p' + $DbPassword + ' < "' + $sqlFile + '" 2>&1'
    $out = cmd /c $cmdLine
    $code = $LASTEXITCODE
    $out | Where-Object { $_ -notmatch "Warning.*password on the command line" } | Write-Host
    if ($code -ne 0) { throw "Import SQL that bai (exit $code)" }
    Write-Host "      Da import schema + du lieu mau." -ForegroundColor Green
} else {
    Write-Host "[1/3] Bo qua import schema ( -SkipSchema )" -ForegroundColor Yellow
}

# 2) Tao tep bao cao mau (PDF that)
$storageDir = Join-Path $project "storage\reports"
if (-not (Test-Path (Join-Path $storageDir "demo"))) {
    Write-Host "[2/3] Tao tep bao cao mau PDF..." -ForegroundColor Cyan
    $py = Get-Command python -ErrorAction SilentlyContinue
    if ($py) {
        & $py.Source (Join-Path $project "database\tao_bao_cao_mau.py") $storageDir | Out-Null
        Write-Host "      Da tao tep bao cao mau." -ForegroundColor Green
    } else {
        Write-Host "      Bo qua (khong tim thay python). Can tai bao cao thu cong." -ForegroundColor Yellow
    }
} else {
    Write-Host "[2/3] Tep bao cao mau da ton tai." -ForegroundColor DarkGray
}

# 3) Sinh BCrypt cho tat ca tai khoan
if (-not $SkipPassword) {
    Write-Host "[3/3] Sinh mat khau BCrypt (password = '$Password')..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "reset-passwords.ps1") -Password $Password
    if ($LASTEXITCODE -ne 0) { throw "Sinh mat khau that bai" }
} else {
    Write-Host "[3/3] Bo qua sinh mat khau ( -SkipPassword )" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "DONE. Dang nhap: admin / truongkhoa / gv01..08 / sv01..21  -  mat khau: $Password" -ForegroundColor Green
