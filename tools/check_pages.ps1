$ErrorActionPreference = "Continue"
$base = "http://localhost:8080"
$user = if ($args.Count -ge 1) { $args[0] } else { "truongkhoa" }
$pass = if ($args.Count -ge 2) { $args[1] } else { "123456" }
$urls = @(
  "/", "/topics", "/announcements",
  "/periods", "/periods/new", "/periods/1/edit",
  "/head/topics", "/head/topics/1/supervisors", "/head/reviewers",
  "/head/councils", "/head/councils/1", "/head/results", "/head/emails", "/head/announcements",
  "/admin/departments", "/admin/departments/new",
  "/admin/profiles/lecturers", "/admin/profiles/lecturers/1", "/admin/profiles/lecturers/1/edit",
  "/admin/profiles/students", "/admin/profiles/students/1", "/admin/profiles/students/1/edit",
  "/account/profile", "/account/password",
  "/admin/users", "/admin/users/new", "/roles", "/roles/new", "/roles/1/edit", "/roles/1/permissions",
  "/admin/announcements",
  "/lecturer/topics", "/lecturer/topics/new", "/lecturer/topics/1/edit", "/lecturer/reviews",
  "/lecturer/reports", "/lecturer/registrations", "/lecturer/council-work",
  "/student/groups", "/student/registrations", "/student/reports", "/student/results"
)
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Invoke-WebRequest -Uri "$base/login" -WebSession $session -UseBasicParsing
$token = [regex]::Match($r.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
$body = @{ username = $user; password = $pass; _csrf = $token }
try {
  Invoke-WebRequest -Uri "$base/login" -Method Post -Body $body -WebSession $session -UseBasicParsing -MaximumRedirection 5 | Out-Null
} catch { Write-Host "LOGIN FAIL: $($_.Exception.Message)" }
Write-Host "=== User: $user ==="
$fail = 0
foreach ($u in $urls) {
  try {
    $res = Invoke-WebRequest -Uri "$base$u" -WebSession $session -UseBasicParsing -MaximumRedirection 5
    $m = [regex]::Match($res.Content, '<title>([^<]*)</title>')
    $t = if ($m.Success) { $m.Groups[1].Value } else { "(no title)" }
    $flag = if ($res.StatusCode -eq 200) { "  " } else { "! " }
    if ($res.StatusCode -ne 200) { $fail++ }
    Write-Host ("{0}{1,-42} {2}  {3}" -f $flag, $u, $res.StatusCode, $t)
  } catch {
    $code = ""
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    if ($code -ne 403) { $fail++ }
    Write-Host ("! {0,-42} {1}  {2}" -f $u, $code, $_.Exception.Message)
  }
}
Write-Host "Non-403 failures: $fail"
