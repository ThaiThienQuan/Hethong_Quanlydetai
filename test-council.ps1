$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# --- 1. Lay CSRF token tu trang dang nhap ---
$loginPage = Invoke-WebRequest -Uri "$base/login" -WebSession $session -UseBasicParsing
$csrf = [regex]::Match($loginPage.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
Write-Output "CSRF token: $(if($csrf){'OK'}else{'THIEU'})"

# --- 2. Dang nhap ---
$body = @{ username = "truongkhoa"; password = "123456"; _csrf = $csrf }
try {
    $resp = Invoke-WebRequest -Uri "$base/login" -Method POST -Body $body -WebSession $session -UseBasicParsing -MaximumRedirection 5
    Write-Output "Dang nhap OK -> $($resp.BaseResponse.ResponseUri.AbsoluteUri)"
} catch {
    Write-Output "Dang nhap that bai: $($_.Exception.Message)"
    exit 1
}

# --- 3. Mo trang quan ly hoi dong ---
$page = Invoke-WebRequest -Uri "$base/head/councils" -WebSession $session -UseBasicParsing
Write-Output "Trang hoi dong -> HTTP $($page.StatusCode)"

# --- 4. Kiem tra o ma hoi dong co readonly khong ---
$field = [regex]::Match($page.Content, '(?s)<input[^>]*id="previewCouncilCode"[^>]*>')
Write-Output "--- O MA XEM TRUOC ---"
if ($field.Success) { Write-Output $field.Value } else { Write-Output "KHONG TIM THAY o previewCouncilCode" }

$hidden = [regex]::Match($page.Content, '(?s)<input[^>]*name="councilCode"[^>]*>')
Write-Output "--- O HIDDEN councilCode ---"
if ($hidden.Success) { Write-Output $hidden.Value } else { Write-Output "KHONG TIM THAY o hidden councilCode" }

# --- 5. Lay danh sach dot ---
$periodIds = [regex]::Matches($page.Content, '<option value="(\d+)"') | ForEach-Object { $_.Groups[1].Value }
Write-Output "Dot trong dropdown: $($periodIds -join ', ')"
$firstName = [regex]::Match($page.Content, '<option value="\d+"[^>]*>([^<]+)</option>').Groups[1].Value
Write-Output "Ten dot dau: $firstName"

# --- 6. Xem truoc ma qua endpoint ---
if ($periodIds.Count -gt 0) {
    $pid0 = $periodIds[0]
    $preview = Invoke-WebRequest -Uri "$base/head/councils/preview-code?periodId=$pid0" -WebSession $session -UseBasicParsing
    Write-Output "Ma xem truoc dot $pid0 : [$($preview.Content.Trim())]"

    # --- 7. That bai co duoc bao loi ro rang khong ---
    $formCsrf = [regex]::Match($page.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
    if (-not $formCsrf) { $formCsrf = $csrf }
    $bad = @{ councilCode = ""; councilName = ""; periodId = "$pid0"; _csrf = $formCsrf }
    $r = Invoke-WebRequest -Uri "$base/head/councils" -Method POST -Body $bad -WebSession $session -UseBasicParsing
    Write-Output "POST thieu ten -> HTTP $($r.StatusCode)"
    $errs = [regex]::Matches($r.Content, 'class="field-error"[^>]*>([^<]+)<') | ForEach-Object { $_.Groups[1].Value.Trim() }
    Write-Output "Loi hien thi tren form: $($errs -join ' | ')"
}