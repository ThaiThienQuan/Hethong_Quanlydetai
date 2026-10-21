# Script kiem thu nhanh cac trang bang HTTP (PowerShell)
# Dung: .\scripts\smoke.ps1 [-User truongkhoa] [-Pass 123456] [-BaseUrl http://localhost:8080] -Paths "/a","/b"
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$User = "truongkhoa",
    [string]$Pass = "123456",
    [string[]]$Paths = @(),
    [switch]$Dump,
    [string]$DumpMatch = "",
    [hashtable]$Post = @{}
)

$ErrorActionPreference = "Stop"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$loginPage = Invoke-WebRequest -Uri ($BaseUrl + "/login") -WebSession $session -UseBasicParsing
$csrf = ([regex]::Match($loginPage.Content, 'name="_csrf"[^>]*value="([^"]+)"')).Groups[1].Value
if (-not $csrf) {
    $csrf = ([regex]::Match($loginPage.Content, 'value="([^"]+)"[^>]*name="_csrf"')).Groups[1].Value
}
$null = Invoke-WebRequest -Uri ($BaseUrl + "/login") -Method Post -WebSession $session -UseBasicParsing `
    -Body @{ _csrf = $csrf; username = $User; password = $Pass } -MaximumRedirection 5

# Neu can CSRF moi (sau khi dang nhap, token co the doi)
if ($Post.Count -gt 0) {
    $homePage = Invoke-WebRequest -Uri ($BaseUrl + "/") -WebSession $session -UseBasicParsing
    $csrf2 = ([regex]::Match($homePage.Content, 'name="_csrf"[^>]*value="([^"]+)"')).Groups[1].Value
    if ($csrf2) { $csrf = $csrf2 }
    $body = @{ _csrf = $csrf }
    foreach ($k in $Post.Keys) { $body[$k] = $Post[$k] }
    try {
        $r = Invoke-WebRequest -Uri ($BaseUrl + $Post["__url"]) -Method Post -WebSession $session -UseBasicParsing `
            -Body $body -MaximumRedirection 5
        Write-Host ("POST {0} -> {1}" -f $Post["__url"], $r.StatusCode) -ForegroundColor Green
        if ($Dump -and $DumpMatch) {
            foreach ($x in [regex]::Matches($r.Content, $DumpMatch)) {
                Write-Host ("      | " + ($x.Value -replace '<[^>]+>', ' ' -replace '\s+', ' ').Trim())
            }
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host ("POST {0} -> FAIL ({1})" -f $Post["__url"], $code) -ForegroundColor Red
    }
    exit 0
}

$fail = 0
foreach ($p in $Paths) {
    try {
        $r = Invoke-WebRequest -Uri ($BaseUrl + $p) -WebSession $session -UseBasicParsing
        Write-Host ("OK   {0}  ({1})" -f $p, $r.StatusCode) -ForegroundColor Green
        if ($Dump) {
            if ($DumpMatch) {
                foreach ($x in [regex]::Matches($r.Content, $DumpMatch)) {
                    Write-Host ("      | " + ($x.Value -replace '<[^>]+>', ' ' -replace '\s+', ' ').Trim())
                }
            } else { Write-Host $r.Content }
        }
    } catch {
        $fail++
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host ("FAIL {0}  ({1})" -f $p, $code) -ForegroundColor Red
    }
}
Write-Host "--- loi: $fail / $($Paths.Count)"
exit $fail
