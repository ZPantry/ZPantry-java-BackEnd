ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS allergens text;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS allergens text;
