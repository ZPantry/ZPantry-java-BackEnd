UPDATE ingredients
SET default_quantity = CASE
    WHEN unit IN ('g', 'ml') THEN 100
    ELSE 1
END
WHERE default_quantity IS NULL;
