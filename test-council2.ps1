$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"

function Login {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $p = Invoke-WebRequest -Uri "$base/login" -WebSession $s -UseBasicParsing
    $c = [regex]::Match($p.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
    Invoke-WebRequest -Uri "$base/login" -Method POST -WebSession $s -UseBasicParsing `
        -Body @{ username="truongkhoa"; password="123456"; _csrf=$c } -MaximumRedirection 5 | Out-Null
    return $s
}

function GetCsrf($s, $url) {
    $p = Invoke-WebRequest -Uri $url -WebSession $s -UseBasicParsing
    $m = [regex]::Match($p.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
    return @{ Session = $s; Csrf = $m; Page = $p }
}

$ctx = GetCsrf (Login) "$base/head/councils"
$periodId = [regex]::Match($ctx.Page.Content, '<option value="(\d+)"').Groups[1].Value

Write-Output "=== Tao 3 hoi dong tren cung dot $periodId ==="
for ($i = 1; $i -le 3; $i++) {
    $code = (Invoke-WebRequest -Uri "$base/head/councils/preview-code?periodId=$periodId" -WebSession $ctx.Session -UseBasicParsing).Content.Trim()
    Write-Output "  Lan $i - ma se duoc cap: [$code]"

    $r = Invoke-WebRequest -Uri "$base/head/councils" -Method POST -WebSession $ctx.Session -UseBasicParsing `
        -Body @{ councilCode=""; councilName="Hoi dong test $i"; periodId="$periodId"; reportDate="2026-12-20T09:00"; _csrf=$ctx.Csrf }
    $errs = [regex]::Matches($r.Content, 'class="field-error"[^>]*>([^<]+)<') | ForEach-Object { $_.Groups[1].Value.Trim() }
    if ($errs.Count -gt 0) { Write-Output "    LOI: $($errs -join ' | ')" }
    else { Write-Output "    OK - da tao" }
    $ctx.Csrf = [regex]::Match($r.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
    if (-not $ctx.Csrf) { $ctx.Csrf = $ctx.Csrf }
}

Write-Output ""
Write-Output "=== Danh sach ma hoi dong vua tao ==="
$p = Invoke-WebRequest -Uri "$base/head/councils" -WebSession $ctx.Session -UseBasicParsing
$codes = [regex]::Matches($p.Content, '<td>(HD-[^<]+)</td>') | ForEach-Object { $_.Groups[1].Value }
$codes | Select-Object -Unique | ForEach-Object { Write-Output "  $_" }