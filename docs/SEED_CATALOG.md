# Manual catalog seed

Run [the seed script](../tools/seed-vietnamese-catalog.sql) only on an isolated development or staging PostgreSQL database:

```powershell
psql "$env:SPRING_DATASOURCE_URL" -U "$env:SPRING_DATASOURCE_USERNAME" -f tools/seed-vietnamese-catalog.sql
```

It inserts 300 catalog ingredients and 150 recipes without schema changes, deletion, or automatic Flyway execution. The script can be re-run without duplicating active rows.

Do not run it against production or a shared legacy database. The generated catalog names and recipe relationships are a development fixture and must be replaced with reviewed culinary and nutritional data before production use.
