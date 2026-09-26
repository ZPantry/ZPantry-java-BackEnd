param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [Parameter(Mandatory = $true)]
    [string] $DatabaseContainer,

    [string] $OutputDirectory = "src/test/resources/contracts/users/evidence/captured-2026-09-18"
)

# TEST-ONLY evidence capture. Synthetic dev accounts only; tokens remain in memory.

$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null

function Login([string] $email, [string] $password) {
    $payload = @{ email = $email; password = $password } | ConvertTo-Json -Compress
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/Auth/login" `
        -ContentType "application/json" -Body $payload
    if (-not $response.success -or [string]::IsNullOrWhiteSpace($response.data.accessToken)) {
        throw "Legacy login failed for $email"
    }
    return $response.data.accessToken
}

function UserId([string] $email) {
    $id = docker exec $DatabaseContainer psql -U zpantry_contract -d zpantry_contract `
        -X -A -t -c "SELECT id FROM users WHERE email = '$email';"
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($id)) {
        throw "Could not resolve synthetic user ID for $email"
    }
    return $id.Trim()
}

function CaptureRow([string] $fileName, [string] $id) {
    $sql = @"
SELECT COALESCE(row_to_json(t)::text, 'null')
FROM (
  SELECT id, created_at, created_by, updated_at, updated_by, deleted_at, deleted_by,
         is_deleted, full_name, email, avatar_url, password_hashed, otp_code,
         otp_expired_at, otp_retry_count, is_email_confirmed, is_active, role,
         refresh_token_hash, refresh_token_expires_at
  FROM users WHERE id = '$id'::uuid
) t;
"@
    $row = docker exec $DatabaseContainer psql -U zpantry_contract -d zpantry_contract `
        -X -A -t -c $sql
    if ($LASTEXITCODE -ne 0) { throw "Database row capture failed" }
    [System.IO.File]::WriteAllText((Join-Path $outputPath $fileName), ($row.Trim() + "`n"), $utf8NoBom)
}

$manifest = [System.Collections.Generic.List[object]]::new()

function CaptureHttp(
    [string] $name,
    [string] $method,
    [string] $path,
    [string] $actor,
    [string] $token = "",
    [string] $body = ""
) {
    $bodyFile = Join-Path $outputPath "$name.body.json"
    $headerFile = Join-Path $outputPath "$name.headers.txt"
    $args = @("-sS", "-X", $method, "-D", $headerFile, "-o", $bodyFile,
        "-w", "%{http_code}", "$BaseUrl$path")
    if (-not [string]::IsNullOrWhiteSpace($token)) {
        $args += @("-H", "Authorization: Bearer $token")
    }
    if (-not [string]::IsNullOrWhiteSpace($body)) {
        $args += @("-H", "Content-Type: application/json", "--data-binary", $body)
    }
    $status = (& curl.exe @args).Trim()
    if ($LASTEXITCODE -ne 0) { throw "HTTP capture failed: $name" }
    $manifest.Add([ordered]@{
        name = $name
        method = $method
        endpoint = $path
        actor = $actor
        requestBody = if ([string]::IsNullOrWhiteSpace($body)) { $null } else { $body }
        status = [int]$status
        bodyFixture = "$name.body.json"
        headersFixture = "$name.headers.txt"
    })
}

$adminToken = Login "admin@test.dev" "Admin-Contract-2026!"
$ownerToken = Login "owner@test.dev" "Owner-Contract-2026!"
$otherToken = Login "other-user@test.dev" "Other-Contract-2026!"
$adminId = UserId "admin@test.dev"
$ownerId = UserId "owner@test.dev"
$otherId = UserId "other-user@test.dev"
$missingId = "00000000-0000-0000-0000-000000000099"

CaptureHttp "get-list-admin-default" "GET" "/api/users" "admin" $adminToken
CaptureHttp "get-list-admin-explicit-page" "GET" "/api/users?pageIndex=2&pageSize=1" "admin" $adminToken
CaptureHttp "get-list-admin-zero-boundary" "GET" "/api/users?pageIndex=0&pageSize=0" "admin" $adminToken
CaptureHttp "get-list-admin-size-over-max" "GET" "/api/users?pageIndex=1&pageSize=101" "admin" $adminToken
CaptureHttp "get-list-unauthenticated" "GET" "/api/users" "anonymous"
CaptureHttp "get-list-normal-user" "GET" "/api/users" "owner" $ownerToken

CaptureHttp "get-detail-admin-existing" "GET" "/api/users/$ownerId" "admin" $adminToken
CaptureHttp "get-detail-admin-missing" "GET" "/api/users/$missingId" "admin" $adminToken
CaptureHttp "get-detail-unauthenticated" "GET" "/api/users/$ownerId" "anonymous"
CaptureHttp "get-detail-normal-user" "GET" "/api/users/$ownerId" "owner" $ownerToken

CaptureRow "put-partial.before.json" $ownerId
CaptureHttp "put-owner-partial" "PUT" "/api/users/$ownerId" "owner" $ownerToken '{"fullName":"Contract Owner Updated"}'
CaptureRow "put-partial.after.json" $ownerId

CaptureRow "put-empty-object.before.json" $ownerId
CaptureHttp "put-owner-empty-object" "PUT" "/api/users/$ownerId" "owner" $ownerToken '{}'
CaptureRow "put-empty-object.after.json" $ownerId

CaptureRow "put-empty-strings.before.json" $ownerId
CaptureHttp "put-owner-empty-strings" "PUT" "/api/users/$ownerId" "owner" $ownerToken '{"fullName":"","avatarUrl":""}'
CaptureRow "put-empty-strings.after.json" $ownerId

CaptureRow "put-password.before.json" $ownerId
CaptureHttp "put-owner-password" "PUT" "/api/users/$ownerId" "owner" $ownerToken '{"password":"Owner-Contract-Updated-2026!"}'
CaptureRow "put-password.after.json" $ownerId
$ownerTokenAfterRejectedPasswordPut = Login "owner@test.dev" "Owner-Contract-2026!"

CaptureHttp "put-wrong-owner" "PUT" "/api/users/$otherId" "owner" $ownerTokenAfterRejectedPasswordPut '{"fullName":"Forbidden"}'
CaptureHttp "put-admin-other-user" "PUT" "/api/users/$ownerId" "admin" $adminToken '{"fullName":"Forbidden Admin Update"}'
CaptureHttp "put-unauthenticated" "PUT" "/api/users/$ownerId" "anonymous" "" '{"fullName":"Unauthenticated"}'

CaptureHttp "delete-normal-user" "DELETE" "/api/users/$ownerId" "other-user" $otherToken
CaptureHttp "delete-unauthenticated" "DELETE" "/api/users/$ownerId" "anonymous"
CaptureRow "delete-admin.before.json" $ownerId
CaptureHttp "delete-admin-success" "DELETE" "/api/users/$ownerId" "admin" $adminToken
CaptureRow "delete-admin.after.json" $ownerId
CaptureHttp "get-detail-admin-after-delete" "GET" "/api/users/$ownerId" "admin" $adminToken
CaptureHttp "put-owner-after-delete" "PUT" "/api/users/$ownerId" "owner" $ownerTokenAfterRejectedPasswordPut '{}'
CaptureHttp "delete-admin-missing" "DELETE" "/api/users/$ownerId" "admin" $adminToken

$passwordBefore = Get-Content -Raw (Join-Path $outputPath "put-password.before.json") | ConvertFrom-Json
$passwordRow = Get-Content -Raw (Join-Path $outputPath "put-password.after.json") | ConvertFrom-Json
$payload = [Convert]::FromBase64String($passwordRow.password_hashed)
$passwordFormat = [ordered]@{
    syntheticOnly = $true
    putReachedPasswordMutation = $false
    hashUnchangedAfterRejectedPut = $passwordBefore.password_hashed -eq $passwordRow.password_hashed
    loginWithOriginalPasswordStillSucceeded = -not [string]::IsNullOrWhiteSpace($ownerTokenAfterRejectedPasswordPut)
    payloadBytes = $payload.Length
    formatMarker = $payload[0]
    saltBytes = 16
    subkeyBytes = 32
    iterations = 1000
    prf = "PBKDF2-HMAC-SHA1"
}
[System.IO.File]::WriteAllText(
    (Join-Path $outputPath "password-update-verification.json"),
    ($passwordFormat | ConvertTo-Json -Depth 5) + "`n",
    $utf8NoBom)

$provenance = [ordered]@{
    legacyCommit = "a010fdc5894176596bb195e4fef66db2c09496f1"
    capturedAtUtc = [DateTime]::UtcNow.ToString("O")
    environment = "Disposable Docker: official .NET 10 legacy image + pgvector/pgvector:0.8.2-pg16"
    databaseContainer = $DatabaseContainer
    database = "zpantry_contract"
    dataPolicy = "Synthetic test accounts only; no real credentials/data; bearer tokens not persisted"
    accounts = @(
        [ordered]@{ email = "admin@test.dev"; id = $adminId; role = "admin"; active = $true; confirmed = $true },
        [ordered]@{ email = "owner@test.dev"; id = $ownerId; role = "user"; active = $true; confirmed = $true },
        [ordered]@{ email = "other-user@test.dev"; id = $otherId; role = "user"; active = $true; confirmed = $true }
    )
    accountCreation = "Legacy EnsureDemoAccountsAsync bootstrap; actual Microsoft.AspNet.Identity.PasswordHasher"
    cases = $manifest
}
[System.IO.File]::WriteAllText(
    (Join-Path $outputPath "manifest.json"),
    ($provenance | ConvertTo-Json -Depth 10) + "`n",
    $utf8NoBom)

Write-Output "Captured $($manifest.Count) HTTP cases to $outputPath"
