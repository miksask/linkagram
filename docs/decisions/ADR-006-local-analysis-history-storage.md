# ADR-006: Local analysis history storage

## Status

Accepted

Partially supersedes the persistence bullet of ADR-004 for opt-in local history
only. Logging, cleartext, TLS, clipboard, and permission rules in ADR-004 remain
in force.

## Context

Spec 006 requires optional on-device history of successful analyses with search,
date filters, single/multi delete, and a 5 000-row cap. The app previously
persisted nothing (ADR-004). We need a storage stack that fits the single-module
architecture, stays testable without a DI framework, and excludes history from
Android Auto Backup.

## Decision

1. **Room 2.8.4** for history rows, with KSP 2.3.10. Prefer Room 2.x over Room 3
   (`androidx.room3`) for this Android-only demo: mature API, smaller migration
   risk, sufficient for the schema.
2. **Preferences DataStore 1.2.1** for `historyEnabled` (default `false`).
3. **Two tables**: `history_entries` (snapshot fields + searchable normalized
   columns) and `history_redirects` (`historyEntryId`, ordinal, fromUrl, toUrl,
   status) with FK cascade delete.
4. **Identifiers**: UUID strings; completion time as epoch millis; `recordVersion = 1`.
5. **`HistoryRepository`** coordinates settings + DAO (saveIfEnabled, query,
   get, delete/restore, clear, deleteMatching). **`HistorySettingsRepository`**
   wraps DataStore. UI never touches DAO/DataStore.
6. **`LinkagramApplication` + `AppContainer`** hold singletons; ViewModels use
   constructor injection / factories. No Hilt/Koin.
7. **Schema export** enabled; DB version 1 committed. No
   `fallbackToDestructiveMigration`; real `Migration`s from the next version.
8. **Backup**: exclude the Room database domain and DataStore history
   preferences from cloud backup and device transfer via
   `backup_rules.xml` / `data_extraction_rules.xml`.
9. **Search**: store Unicode-normalized lowercase searchable fields; SQL
   escaped partial `LIKE`. Note that B-tree indexes do not accelerate
   leading-wildcard contains; the 5 000 cap keeps scans acceptable. Index
   `completedAtMillis` for date ranges.
10. **Insert + prune** and **delete matching** run in `@Transaction`s.
11. **Navigation Compose 2.9.8** for Analyze / History / Settings / Details.
12. **Tests**: host-side JVM unit tests with fakes; Compose preview screenshots.
    Instrumentation / emulator UI tests are an explicit follow-up, not a v1
    acceptance gate.

## Consequences

Advantages:

- Opt-in local history matches Spec 006 without a backend.
- Repository seam is justified by concrete multi-surface access.
- Backup exclusion reduces accidental cloud leakage of sensitive URLs.

Trade-offs:

- Room + KSP add build complexity under AGP built-in Kotlin.
- Without instrumentation tests, SQL is validated mainly by compilation and
  repository contracts against fakes.
- Absolute “nothing persisted” privacy copy must be rewritten.

## Alternatives considered

- **Room 3.0**: newest coroutine-first API; rejected for v1 due to new package
  and higher early-adopter risk.
- **DataStore-only history**: poor fit for relational search/filter/delete.
- **JSON file list**: weaker query/transaction story for Spec 006.
- **DI framework**: unnecessary at this size; Application container is enough.
- **Paging 3 now**: deferred; DAO APIs should stay compatible with later paging.
