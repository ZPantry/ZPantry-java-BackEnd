param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [Parameter(Mandatory = $true)]
    [string] $DatabaseContainer,

    [Parameter(Mandatory = $true)]
    [string] $SigningKey,

    [string] $OutputDirectory = "src/test/resources/contracts/users/evidence/claims-diagnosis-2026-09-19"
)

# TEST-ONLY evidence capture for the disposable instrumented legacy runtime.
# Bearer and refresh tokens remain in memory and are never written to disk.

$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null

function Write-Json([string] $name, [object] $value) {
    [System.IO.File]::WriteAllText(
        (Join-Path $outputPath $name),
        (($value | ConvertTo-Json -Depth 20) + "`n"),
        $utf8NoBom)
}

function ConvertFrom-Base64Url([string] $value) {
    $padded = $value.Replace('-', '+').Replace('_', '/')
    while ($padded.Length % 4) { $padded += '=' }
    return [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded))
}

function ConvertTo-Base64Url([byte[]] $value) {
    return [Convert]::ToBase64String($value).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-SignedTestToken([string] $ownerId) {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = [ordered]@{ alg = 'HS256'; typ = 'JWT' }
    $payload = [ordered]@{
        sub = 'owner@test.local'
        email = 'owner@test.local'
        jti = ([Guid]::NewGuid().ToString('N'))
        userId = $ownerId
        nameid = $ownerId
        role = 'user'
        nbf = $now
        exp = $now + 3600
        iss = 'zpantry-contract-test'
        aud = 'zpantry-contract-test-client'
    }
    $headerPart = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes(($header | ConvertTo-Json -Compress)))
    $payloadPart = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Compress)))
    $unsigned = "$headerPart.$payloadPart"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($SigningKey))
    try {
        $signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::ASCII.GetBytes($unsigned)))
    }
    finally {
        $hmac.Dispose()
    }
    return "$unsigned.$signature"
}

function Redact-Claims([object] $value) {
    $copy = $value | ConvertTo-Json -Depth 20 | ConvertFrom-Json
    if ($null -ne $copy.jti) { $copy.jti = '<redacted:32-hex-jti>' }
    if ($null -ne $copy.claims) {
        foreach ($claim in $copy.claims) {
            if ($claim.type -eq 'jti') { $claim.value = '<redacted:32-hex-jti>' }
        }
    }
    return $copy
}

function Capture-Put([string] $prefix, [string] $ownerId, [string] $token) {
    $requestBody = '{"fullName":"Claims Diagnostic Owner"}'
    Write-Json "$prefix.request.json" ([ordered]@{
        method = 'PUT'
        path = "/api/users/$ownerId"
        headers = [ordered]@{
            Authorization = 'Bearer <redacted>'
            'Content-Type' = 'application/json'
        }
        body = ($requestBody | ConvertFrom-Json)
    })

    $headerFile = Join-Path $outputPath "$prefix.response.headers.txt"
    $bodyFile = Join-Path $outputPath "$prefix.response.body.json"
    $status = (& curl.exe -sS -X PUT -D $headerFile -o $bodyFile -w '%{http_code}' `
        "$BaseUrl/api/users/$ownerId" -H "Authorization: Bearer $token" `
        -H 'Content-Type: application/json' --data-binary $requestBody).Trim()
    if ($LASTEXITCODE -ne 0) { throw "PUT capture failed for $prefix" }
    return [int]$status
}

$loginPayload = @{ email = 'owner@test.local'; password = 'Owner-Contract-2026!' } | ConvertTo-Json -Compress
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/Auth/login" -ContentType 'application/json' -Body $loginPayload
if (-not $login.success -or [string]::IsNullOrWhiteSpace($login.data.accessToken)) {
    throw 'Legacy login failed for synthetic owner'
}
$loginToken = $login.data.accessToken
$ownerId = (docker exec $DatabaseContainer psql -U zpantry_contract -d zpantry_contract `
    -X -A -t -c "SELECT id FROM users WHERE email = 'owner@test.local';").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($ownerId)) {
    throw 'Could not resolve synthetic owner UUID'
}

$rawLoginPayload = ConvertFrom-Base64Url $loginToken.Split('.')[1] | ConvertFrom-Json
$loginDiagnostic = Invoke-RestMethod -Uri "$BaseUrl/__diagnostics/claims/$ownerId" `
    -Headers @{ Authorization = "Bearer $loginToken" }
Write-Json 'login-token.raw-claims.redacted.json' (Redact-Claims $rawLoginPayload)
Write-Json 'login-token.principal.redacted.json' (Redact-Claims $loginDiagnostic)
$loginPutStatus = Capture-Put 'login-token.owner-put' $ownerId $loginToken

$signedToken = New-SignedTestToken $ownerId
$rawSignedPayload = ConvertFrom-Base64Url $signedToken.Split('.')[1] | ConvertFrom-Json
$signedDiagnostic = Invoke-RestMethod -Uri "$BaseUrl/__diagnostics/claims/$ownerId" `
    -Headers @{ Authorization = "Bearer $signedToken" }
Write-Json 'signed-token.raw-claims.redacted.json' (Redact-Claims $rawSignedPayload)
Write-Json 'signed-token.principal.redacted.json' (Redact-Claims $signedDiagnostic)
$signedPutStatus = Capture-Put 'signed-token.owner-put' $ownerId $signedToken

Write-Json 'provenance.json' ([ordered]@{
    legacyCommit = 'a010fdc5894176596bb195e4fef66db2c09496f1'
    capturedAtUtc = [DateTime]::UtcNow.ToString('O')
    environment = 'Disposable instrumented .NET 10 legacy image + pgvector/pgvector:0.8.2-pg16'
    packageVersions = [ordered]@{
        'Microsoft.AspNetCore.Authentication.JwtBearer' = '10.0.9'
        'Microsoft.IdentityModel.JsonWebTokens' = '8.0.1'
        'System.IdentityModel.Tokens.Jwt' = '8.0.1'
    }
    database = 'zpantry_contract'
    account = [ordered]@{ email = 'owner@test.local'; id = $ownerId; role = 'user' }
    loginToken = [ordered]@{ source = 'POST /api/Auth/login'; ownerPutStatus = $loginPutStatus }
    signedToken = [ordered]@{
        source = 'Locally HS256-signed equivalent using configured disposable key'
        rawOwnerClaim = 'nameid'
        ownerPutStatus = $signedPutStatus
    }
    diagnosticInstrumentation = 'Temporary legacy copy only; not present in repository production code'
    dataPolicy = 'Synthetic account only; bearer/refresh tokens and signing key not persisted; JTI values redacted'
})

Write-Output "Captured claim diagnostics to $outputPath"
