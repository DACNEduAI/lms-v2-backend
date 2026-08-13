# Time standard

All system instants are UTC.

- PostgreSQL: use `TIMESTAMPTZ`; never add `TIMESTAMP WITHOUT TIME ZONE` for an instant.
- Java: use `Instant` and an injected UTC `Clock`; do not use `LocalDateTime` for an instant.
- HTTP: accept and emit RFC 3339 timestamps with an explicit offset; emitted values use `Z`.
- Quartz: schedule `Date.from(instant)`; never use `ZoneId.systemDefault()`.
- Presentation: clients convert UTC instants to `Asia/Ho_Chi_Minh` for the LMS UI.

`LocalDate` remains appropriate only for an intentional calendar date with no time-of-day.

Run `scripts/check-time-standard.ps1` in CI. It rejects a timezone-less schema
timestamp, a bare Java wall-clock call, a machine-zone Quartz conversion, and a
naive Python timestamp.
