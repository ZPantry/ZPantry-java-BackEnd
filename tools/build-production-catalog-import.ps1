param(
  [string]$ZipPath = 'C:\Users\ASUS\Downloads\ZPantry_300_Ingredients_150_Recipes_CSV_SQL.zip',
  [string]$OutputPath = 'D:\zpantry\z_pantry_backend\tools\import-production-catalog.sql'
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
if (!(Test-Path -LiteralPath $ZipPath)) { throw "ZIP not found: $ZipPath" }
$zip = [IO.Compression.ZipFile]::OpenRead($ZipPath)
function Read-Csv($name) { $r=[IO.StreamReader]::new($zip.GetEntry($name).Open()); try { ConvertFrom-Csv $r.ReadToEnd() } finally { $r.Dispose() } }
function Q($value) { if ($null -eq $value -or $value -eq '') { 'NULL' } else { "'" + $value.Replace("'", "''") + "'" } }
function CanonicalAllergens($value) {
  if ([string]::IsNullOrWhiteSpace($value)) { return '' }
  return (($value -split '[,;]' | ForEach-Object {
    $code = $_.Trim().ToUpperInvariant()
    if ($code -eq 'TREE_NUTS') { 'TREE_NUT' } else { $code }
  } | Where-Object { $_ } | Sort-Object -Unique) -join ',')
}
try { $ingredients=Read-Csv ingredients.csv; $recipes=Read-Csv recipes.csv; $links=Read-Csv recipe_ingredients.csv } finally { $zip.Dispose() }
$known = @{}; $ingredients | ForEach-Object { $known[$_.normalized_name]=$true }
$missing = $links | Where-Object { !$known.ContainsKey($_.ingredient_normalized_name) }
if ($missing) { throw "CSV has $($missing.Count) recipe ingredients without a matching ingredient." }
$sql = [Collections.Generic.List[string]]::new(); $sql.Add('BEGIN;')
foreach($x in $ingredients) { $sql.Add("INSERT INTO ingredients (id,created_at,is_deleted,name,normalized_name,category,unit,calories_per_unit,protein_per_unit,fat_per_unit,carb_per_unit,default_quantity,allergens,gradient_from,gradient_to) SELECT ('a'||substr(md5('catalog:'||$(Q $x.normalized_name)),2,31))::uuid,now(),false,$(Q $x.name),$(Q $x.normalized_name),$(Q $x.category),$(Q $x.unit),$($x.calories_per_unit),$($x.protein_per_unit),$($x.fat_per_unit),$($x.carb_per_unit),$($x.default_quantity),$(Q (CanonicalAllergens $x.allergens)),'#EF9D1F','#FFF0DE' WHERE NOT EXISTS (SELECT 1 FROM ingredients i WHERE i.normalized_name=$(Q $x.normalized_name) AND NOT i.is_deleted);") }
foreach($x in $recipes) { $sql.Add("INSERT INTO recipes (id,created_at,is_deleted,name,description,allergens,cooking_time_minutes,difficulty,serving_size,instruction_text,image_url,source_type,gradient_from,gradient_to) SELECT ('b'||substr(md5('catalog:'||$(Q $x.name)),2,31))::uuid,now(),false,$(Q $x.name),$(Q $x.description),'',$($x.cooking_time_minutes),$(Q $x.difficulty),$($x.serving_size),$(Q $x.instruction_text),$(Q $x.image_url),'production-catalog-v1','#EF9D1F','#FFF0DE' WHERE NOT EXISTS (SELECT 1 FROM recipes r WHERE r.name=$(Q $x.name) AND NOT r.is_deleted);") }
foreach($x in $links) { $sql.Add("INSERT INTO recipe_ingredients (id,created_at,is_deleted,recipe_id,ingredient_id,quantity,unit,is_required,note) SELECT gen_random_uuid(),now(),false,r.id,i.id,$($x.quantity),$(Q $x.unit),$($x.is_required),$(Q $x.note) FROM recipes r JOIN ingredients i ON i.normalized_name=$(Q $x.ingredient_normalized_name) AND NOT i.is_deleted WHERE r.name=$(Q $x.recipe_name) AND NOT r.is_deleted AND NOT EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id=r.id AND ri.ingredient_id=i.id AND NOT ri.is_deleted);") }
$sql.Add("UPDATE recipes r SET allergens=COALESCE(a.allergens,''),updated_at=now() FROM (SELECT r.id,string_agg(DISTINCT trim(code),',' ORDER BY trim(code)) allergens FROM recipes r JOIN recipe_ingredients ri ON ri.recipe_id=r.id AND NOT ri.is_deleted JOIN ingredients i ON i.id=ri.ingredient_id AND NOT i.is_deleted CROSS JOIN LATERAL regexp_split_to_table(COALESCE(i.allergens,''), '[,;]') code WHERE r.source_type='production-catalog-v1' AND NOT r.is_deleted AND trim(code)<>'' GROUP BY r.id) a WHERE a.id=r.id;")
$sql.Add('COMMIT;'); $sql.Add("SELECT count(*) FROM recipes WHERE source_type='production-catalog-v1' AND NOT is_deleted;")
[IO.File]::WriteAllLines($OutputPath, $sql, [Text.UTF8Encoding]::new($false)); Write-Host "Created $OutputPath"
