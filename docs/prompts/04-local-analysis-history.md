# Linkagram — Local Analysis History

Continue development of the existing **Linkagram** project.

Specs 001–005 and screenshot testing (ADR-005) are already implemented. Before
changing behaviour, read Spec 006, ADR-006, ADR-004 (as partially superseded),
`AGENTS.md`, and the current analysis / resolver / UI code. Preserve established
conventions unless Spec 006 or ADR-006 requires a change. Do not rename the app.

Implement opt-in local analysis history in the following order.

## Sources of truth

1. `docs/specs/006-local-analysis-history.md`
2. `docs/decisions/ADR-006-local-analysis-history-storage.md`
3. `docs/product.md`
4. `AGENTS.md`

Older specs (001–005) and prompts (01–03) stay historical for their phases.
Do not rewrite them to pretend history always existed.

## Fixed decisions (do not reopen)

- Room **2.8.4** + KSP **2.3.10**; Preferences DataStore **1.2.1**; Navigation
  Compose **2.9.8**.
- History toggle default **off**. Save only complete `ResolveResult.Success`.
- Bottom nav: **Analyze** / **History**. **Settings** via TopAppBar action.
- Source URL = extracted candidate **before** normalization.
- `ResolveResult.Success` includes **finalStatusCode**; show it in live results
  and saved details.
- Redirect hops store `fromUrl`, `toUrl`, status, ordinal.
- Sort newest-first only in v1. No Paging 3. No emulator UI tests in v1.
- No DI framework, no multi-module split, no backend/sync/favourites/export.

## Implementation order

### 1. Domain and analysis pipeline seams

- Extend URL normalization so success carries `sourceUrl` + `normalizedUrl`.
- Add `finalStatusCode` to successful resolve results; update resolver + tests.
- Add domain types: `CompletedAnalysis`, `HistoryEntry`, `HistoryRedirect`,
  `HistoryResultType`, `HistoryQuery`.
- Snapshot only after map parsing. Never persist UI state objects.

### 2. Storage and app container

- Add Gradle deps/plugins per ADR-006; enable Room schema export; commit schema.
- Implement `HistoryEntryEntity`, `HistoryRedirectEntity`, `HistoryDao`,
  `LinkagramDatabase`, mappers, searchable normalized fields.
- Implement `HistorySettingsRepository` and coordinating `HistoryRepository`
  (`saveIfEnabled`, observe/query/count/get, delete/restore, clear,
  deleteMatching). Insert+prune and matching delete are `@Transaction`s.
- Cap at 5 000 rows; prune oldest with the insert.
- Add `LinkagramApplication` + `AppContainer` + ViewModel factories.

### 3. Save hook

- From `AnalysisViewModel` Success path only, call `saveIfEnabled`.
- Read toggle immediately before the DB write.
- Failures/timeouts/loops/limits/HTTP errors/cancellations never save.
- Duplicate URLs always create a new UUID row.
- Local save failure must not convert a successful network analysis into an
  error; surface a non-fatal notice if needed.

### 4. Navigation and screens

- `NavHost` with Analyze, History, Settings, Details(`historyEntryId`).
- Shared scaffold: bottom bar Analyze/History; settings icon in TopAppBar.
- Incoming SHARE/VIEW intents always land on Analyze with the draft filled.
- Opening details never issues HTTP. **Analyze again** loads source URL into
  Analyze and starts a new analysis; success may create a new history row.

### 5. History / Details / Settings behaviour

Cover Spec 006 states: loading, empty, disabled+empty, list, no search match,
no filter match, read error. Search debounce ~300 ms. Date filters All / Today /
7d / 30d / custom inclusive local dates via injected clock/zone. Reset clears
search and date filter. Single delete + Undo; clear all and delete matching with
count confirmations; when query empty and filter All, show only Clear history.

### 6. Privacy

- Exclude Room DB and history DataStore from backup / device transfer.
- No production logs of URLs, chains, coordinates, place, address.
- Do not store headers, cookies, bodies, or auth data.

### 7. Tests and screenshots

Required host-side coverage:

- save gating and toggle timing
- duplicates and 5000 prune
- Unicode / case-insensitive / partial search; not redirect chain
- date bounds including DST, midnight, timezone
- delete / restore / clear / matching delete
- History / Details / Settings ViewModel states

Add Compose `@PreviewTest` baselines for History, Details, Settings, shell,
dialogs, long URLs, dark theme, large font. Update Analysis baselines if the
shell or final-status UI changes.

Explicitly deferred (document, do not fake as done): interactive `androidTest`
and on-device Room DAO instrumentation.

### 8. Docs wrap-up

Update `docs/architecture.md`, `README.md`, `CONTRIBUTING.md`, testing rules,
and the feature-request template so agents no longer treat local history as
forbidden. Keep ADR-004 body historically intact with the supersession note.

## Privacy and non-goals

Do not add accounts, analytics, ads, backend, cloud sync, export/import,
favourites, WebView, Maps SDK, location permissions, DI frameworks, or extra
Gradle modules.

## Definition of done

- Spec 006 acceptance criteria met for host-side behaviour.
- ADR-006 storage choices reflected in code.
- `./gradlew test`, `./gradlew lint`, `./gradlew validateDebugScreenshotTest`,
  and `./gradlew assembleDebug` succeed.
- Screenshot baselines updated only after intentional UI review.
- Deferred instrumentation gap stated in Spec 006 / ADR-006 / README.

At the end, summarize implemented functionality, decisions, tests, known
limitations, and files changed.
