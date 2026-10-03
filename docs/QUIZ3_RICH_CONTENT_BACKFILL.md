# Quiz3 rich-content backfill

This command repairs Quiz3 question, answer, and attempt snapshot display content without changing the AI webhook contract. It is disabled during normal LMS startup and runs in dry-run mode unless apply is explicitly enabled.

## Prepare the database

Existing databases must apply `src/main/resources/db/manual/V20261002_01__quiz3_rich_content_backfill_audit.sql` before apply or restore mode. Development databases rebuilt from `schema.sql` already include the audit table.

## Dry run

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=false" \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--spring.sql.init.mode=never --spring.quartz.auto-startup=false --spring.quartz.jdbc.initialize-schema=never --quiz3.rich-content-backfill.enabled=true"
```

The report includes inspected, convertible, skipped, unchanged, and per-decision counts. Dry-run mode does not write content or audit rows.

## Apply

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=false" \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--spring.sql.init.mode=never --spring.quartz.auto-startup=false --spring.quartz.jdbc.initialize-schema=never --quiz3.rich-content-backfill.enabled=true --quiz3.rich-content-backfill.apply=true --quiz3.rich-content-backfill.batch-size=100"
```

Each update checks that content and format still match the values read by the command. Successful changes are recorded under the reported `runId`; concurrent changes are counted as conflicts and left untouched. Running apply again is idempotent.

## Restore a run

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=false" \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--spring.sql.init.mode=never --spring.quartz.auto-startup=false --spring.quartz.jdbc.initialize-schema=never --quiz3.rich-content-backfill.enabled=true --quiz3.rich-content-backfill.restore-run=<run-id> --quiz3.rich-content-backfill.batch-size=100"
```

Restore also uses optimistic checks: it restores only rows that still equal the audited post-backfill value. Conflicts remain unchanged for manual review.
