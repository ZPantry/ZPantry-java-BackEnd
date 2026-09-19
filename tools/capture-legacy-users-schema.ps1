param(
    [Parameter(Mandatory = $true)]
    [string] $DatabaseContainer,

    [string] $OutputDirectory = "src/test/resources/contracts/users/evidence/captured-2026-09-18"
)

# TEST-ONLY catalog capture for the disposable PostgreSQL container.

$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null

function Query([string] $sql) {
    $result = docker exec $DatabaseContainer psql -U zpantry_contract -d zpantry_contract `
        -X -A -t -c $sql
    if ($LASTEXITCODE -ne 0) { throw "Catalog query failed" }
    return $result.Trim()
}

$columns = Query @"
SELECT json_agg(row_to_json(x) ORDER BY x.ordinal_position)
FROM (
 SELECT table_schema, table_name, ordinal_position, column_name, data_type,
        udt_schema, udt_name, is_nullable, column_default,
        character_maximum_length, numeric_precision, numeric_scale,
        datetime_precision, is_identity, identity_generation
 FROM information_schema.columns
 WHERE table_schema = 'public' AND table_name = 'users'
) x;
"@

$constraints = Query @"
SELECT json_agg(row_to_json(x) ORDER BY x.constraint_name)
FROM (
 SELECT tc.constraint_name, tc.constraint_type,
        array_agg(kcu.column_name ORDER BY kcu.ordinal_position)
            FILTER (WHERE kcu.column_name IS NOT NULL) AS columns
 FROM information_schema.table_constraints tc
 LEFT JOIN information_schema.key_column_usage kcu
   ON tc.constraint_catalog = kcu.constraint_catalog
  AND tc.constraint_schema = kcu.constraint_schema
  AND tc.constraint_name = kcu.constraint_name
 WHERE tc.table_schema = 'public' AND tc.table_name = 'users'
   AND tc.constraint_type IN ('PRIMARY KEY', 'UNIQUE', 'FOREIGN KEY')
 GROUP BY tc.constraint_name, tc.constraint_type
) x;
"@

$indexes = Query @"
SELECT json_agg(row_to_json(x) ORDER BY x.indexname)
FROM (
 SELECT schemaname, tablename, indexname, indexdef
 FROM pg_indexes
 WHERE schemaname = 'public' AND tablename = 'users'
) x;
"@

$catalog = [ordered]@{
    source = "Disposable pgvector/pgvector:0.8.2-pg16 migrated by pinned legacy backend"
    legacyCommit = "a010fdc5894176596bb195e4fef66db2c09496f1"
    databaseVersion = Query "SHOW server_version;"
    currentDatabase = Query "SELECT current_database();"
    currentSchema = Query "SELECT current_schema();"
    columns = $columns | ConvertFrom-Json
    constraints = $constraints | ConvertFrom-Json
    indexes = $indexes | ConvertFrom-Json
}

[System.IO.File]::WriteAllText(
    (Join-Path $outputPath "disposable-users-catalog.json"),
    ($catalog | ConvertTo-Json -Depth 10) + "`n",
    $utf8NoBom)

Write-Output "Captured disposable users catalog to $outputPath"
