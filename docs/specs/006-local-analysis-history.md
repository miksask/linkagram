# Spec 006: Local analysis history

## Goal

Optionally persist successful URL analyses on the device, let the user browse,
search, filter, reopen, and delete them, and keep failed or incomplete analyses
out of storage.

## Status

Accepted

## Input

- Successful analysis snapshots from Specs 001–004
- User preference `historyEnabled` (default off)
- Search text, date filters, and delete actions on the History screen
- Settings actions: toggle history, clear history

## Functional requirements

### Opt-in storage

- Settings toggle **Save analysis history**, default off.
- When off: new successes are not saved; existing rows remain; Analyze works
  unchanged.
- When on: every fully successful analysis is saved automatically with no
  confirmation. No manual save and no Favourites.
- Toggle helper when off:
  > New analyses will not be saved. Existing history stays on this device.
- Toggle helper when on: new successful analyses are saved locally on this
  device.
- Separate **Clear history** action on Settings (and History when needed) deletes
  all rows without changing the toggle.

### What is saved

Save only after a complete `ResolveResult.Success` plus map parsing, when all of
these are available:

- source URL (extracted candidate before normalization)
- normalized URL
- final URL
- final HTTP status code
- ordered redirect chain (`fromUrl`, `toUrl`, status, ordinal)
- map parse result (provider / place / address / coordinates when present)

Do not save validation errors, network/timeout/loop/limit/protocol/HTTP errors,
cancellations, or partial rows. Decide whether to save immediately before the
atomic insert, using the current toggle value. Identical URLs always create a
new row; never merge or overwrite.

After a successful opt-in geocode (Spec 008) for that same analysis, update the
just-saved row’s `latitude` / `longitude` in place. Do not create a second row
for the geocode alone.

### History screen

Bottom-nav destination **History**, newest first. List rows show shortened
source and final URLs, place, address, coordinates, provider, completion time,
and redirect count. Tapping a row opens details by `historyEntryId` only — no
network request. Details show the saved snapshot, **Copy coordinates** when
lat/lon are present (Spec 005), and **Analyze again**, which starts a new
analysis of the source URL. A new success creates a new row.

States: loading; empty; history disabled and empty; populated list; no search
matches; no filter matches; local read error. Disabled-with-existing-rows still
shows the list.

### Search and date filter

- Search (case-insensitive, partial, trimmed, ~300 ms debounce) over source URL,
  final URL, place name, and address. Not over redirect hops.
- Date filters: All, Today, Last 7 days, Last 30 days, Custom inclusive local
  dates. Store absolute timestamps; compute day bounds in the device zone.
- Search and date filter apply together. Show active indicators, result count,
  and Reset (clears search and sets date filter to All).

### Deletion

- Single delete from list or details, with Snackbar **Undo** that restores the
  same id and children. Details pops back to History after delete.
- Clear all: confirmation with count; no Undo; toggle unchanged; current Analyze
  result unchanged.
- Delete matching: confirmation with exact count; one atomic query; no Undo.
  When search is empty and date filter is All, expose only Clear history. Disable
  when count is zero.

### Limits and privacy

- Cap at 5 000 rows; prune oldest in the same transaction as insert.
- History and its setting stay on-device; exclude from cloud backup / device
  transfer. No production logs of URLs, chains, coordinates, place, or address.
- No headers, cookies, bodies, auth data, backend, sync, location permissions,
  WebView, or Maps SDK.

## Non-requirements

- Manual save, favourites, accounts, cloud sync, export/import
- Tags, folders, notes, editing saved rows
- Search over redirect chain; saving failures
- Auto-resolve when opening history
- User-configurable retention period
- Paging 3 (architecture must allow it later)
- Emulator / instrumentation UI tests in v1 (host-side unit + screenshot tests
  only; interactive UI tests are a documented follow-up)

## Result states

| Surface | States |
|---------|--------|
| Settings | Toggle on/off, clear confirmation |
| History | Loading, empty, disabled+empty, list, no search match, no filter match, error |
| Details | Loaded snapshot, missing id, delete confirmation / undo |

## Acceptance criteria

- Given history off, a successful analysis is not persisted.
- Given history on, a successful analysis is persisted atomically with chain and
  map fields.
- Given the same URL analysed twice, two distinct rows exist.
- Given a validation or resolve error, nothing is persisted.
- Given toggle flips before completion, the value at save time decides.
- Given search `Москва` and a matching place name, the row appears
  case-insensitively.
- Given Today filter, only rows completed in the local calendar day appear.
- Given delete one row + Undo, the row and redirects are restored.
- Given delete matching with count 18, exactly those 18 rows are removed.
- Given 5001 inserts with history on, only the newest 5000 remain.
- Opening a history row never issues a network request.

## Test expectations

- JVM unit tests for save gating, duplicates, search/date composition, delete /
  restore / clear / matching delete, prune-at-limit, date bounds (DST, midnight,
  timezone), and ViewModel states.
- Compose preview screenshot tests for History / Details / Settings / shell
  states.
- Deferred: interactive `androidTest` and on-device Room DAO suite.

## Notes

Storage choice: ADR-006. Privacy carve-out relative to ADR-004. Related:
Specs 001–005 for analysis pipeline; UI presentation Spec 004 for live results.
