# Useful context for working on the Persistence layer

SQLDelight (`AppDatabase`) is the persistence layer. Key files:

- `.sq` schema/queries: `composeApp/src/commonMain/sqldelight/`
- `ColumnAdapters.kt` — type adapters for `LocalDate`, `LocalTime`, `Instant`, and JSON-serialized types
- `AppDatabaseFactory.kt` — wires adapters into `AppDatabase`; pass any `SqlDriver` to get an `AppDatabase`
- `DriverFactory` (expect/actual) — must be constructed at the DI boundary and injected; cannot be instantiated from `commonMain`

## Database Migrations

SQLDelight is configured with `verifyMigrations = true`. The initial schema snapshot is at `composeApp/src/commonMain/sqldelight/databases/1.db`.

**When modifying any `.sq` schema file** (adding a column, creating a table, etc.):

1. Create a migration file alongside the `.sq` files:
   `composeApp/src/commonMain/sqldelight/<package>/db/<old_version>_<new_version>.sqm`
   containing the required `ALTER TABLE` / `CREATE TABLE` statements.
2. Regenerate the schema snapshot so `verifyMigrations` has a baseline for the new version:
   ```shell
   ./gradlew generateCommonMainAppDatabaseSchema
   ```
3. Commit both the `.sqm` file and the updated `.db` snapshot.

Skipping this will cause a version-mismatch crash on existing installs.
