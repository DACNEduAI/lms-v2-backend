# UTC migration runbook

Run `V20260814_01__normalize_edu_instants_to_utc.sql` only in a maintenance
window. It changes each `edu` `timestamp without time zone` column to
`TIMESTAMPTZ` by interpreting the existing literal wall-clock value as UTC.
It never shifts a value by seven hours.

## Before the window

1. Block frontend writes and stop the LMS scheduler/workers and AI worker.
2. Confirm no active jobs. Adapt the list if another worker table has been
   added since this runbook was written.

```sql
select 'quiz_generation_jobs' as source, status, count(*)
from edu.quiz_generation_jobs
where status in ('PENDING', 'PROCESSING')
group by status
union all
select 'material_index_jobs', status, count(*)
from edu.material_index_jobs
where status in ('PENDING', 'PROCESSING')
group by status
union all
select 'ccct_ai_jobs', status, count(*)
from edu.ccct_ai_jobs
where status in ('QUEUED', 'RUNNING')
group by status
union all
select 'assessment_generation_batches', status, count(*)
from edu.assessment_generation_batches
where status in ('QUEUED', 'PROCESSING')
group by status
union all
select 'assessment_generation_batch_items', status, count(*)
from edu.assessment_generation_batch_items
where status in ('QUEUED', 'PROCESSING')
group by status
union all
select 'question_image_generation_jobs', status, count(*)
from edu.question_image_generation_jobs
where status in ('PENDING', 'PROCESSING')
group by status
union all
select 'question_ai_jobs', status, count(*)
from edu.question_ai_jobs
where status in ('QUEUED', 'RUNNING')
group by status;
```

3. Create and verify a restorable Supabase backup. Export the `edu` schema and
   data, and retain its checksum with the maintenance ticket.
4. Record the pre-migration inventory:

```sql
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'edu' and data_type = 'timestamp without time zone'
order by table_name, ordinal_position;
```

## Migration and acceptance

1. Rehearse on a restored copy first. Run the manual migration once.
2. Compare the audit table's row/null/min/max values with the rehearsal export.
3. Verify there are zero remaining naked timestamps:

```sql
select count(*) as remaining_naked_timestamps
from information_schema.columns
where table_schema = 'edu' and data_type = 'timestamp without time zone';

show timezone;
```

`remaining_naked_timestamps` must be `0`, and the application connection
timezone must be `UTC` before restart.

4. Restart backend, then AI worker, then re-enable frontend writes. Generate
one quiz and check its initial attempt is `1`, its job is not immediately
timed out, and AI receives `/quiz-gen/generate`.

## Rollback

If rehearsal or acceptance fails, keep writers stopped and restore the backup
made before the migration. Do not attempt a reverse type conversion on the
production database.
