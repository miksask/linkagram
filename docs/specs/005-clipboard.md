# Spec 005: Clipboard

## Goal

Let the user paste a URL from the system clipboard and copy extracted
coordinates back to it.

## Status

Accepted

## Input

- System clipboard contents, read on explicit user action
- Coordinates from Spec 003, when available

## Functional requirements

### Reading

- Read the clipboard only when the user taps the paste action. Never read it on
  launch, on resume, or in the background.
- Accept `text/plain`, `text/html`, and other `text/*` clip items; ignore
  non-text clips.
- Put the clipboard text into the draft field without analysing it
  automatically; the user still presses Analyze.
- Pass the text through the Spec 001 extractor, so shared text containing a URL
  among other words still works.
- Empty or blank clipboard produces the same validation error as empty input.

### Writing

- Copy coordinates in the exact `lat, lon` format from `CoordinateFormatter`.
- Use the label `coordinates` for the clip.
- Give visible confirmation that the copy happened.
- Copy nothing when coordinates are absent; the action is not shown then.
- Offer the same copy action on history details when the saved snapshot has
  coordinates (Spec 006), not only on the live analysis result.

## Non-requirements

- Clipboard change listeners or monitoring
- Copying the final URL or the redirect chain
- Clipboard history

## Privacy

- Clipboard content never leaves the device except as the URL being resolved.
- Clipboard content is not logged.
- Android 12 and later shows a system toast when an app reads the clipboard;
  this is expected and not suppressed.

## Edge cases

- Clipboard empty, or holding an image or file: treated as no usable text.
- Clipboard holds text without any URL: Spec 001 reports `NoUrlFound`.
- Clipboard access denied because the app lacks focus: treated as empty.

## Acceptance criteria

- Given a URL in the clipboard, tapping paste fills the input field.
- Given blank clipboard text, tapping paste shows the empty-input error.
- Given a successful analysis with coordinates, tapping copy places
  `lat, lon` on the clipboard and the button confirms the copy.
- Given a history details snapshot with coordinates, tapping copy places
  `lat, lon` on the clipboard and the button confirms the copy.
- Given no coordinates, no copy action is shown.

## Test expectations

- ViewModel test: blank clipboard text sets the empty validation error.
- ViewModel test: clipboard text is trimmed into the draft.
- Formatter test: coordinates render as `lat, lon`.

## Notes

Reading lives in `core/clipboard/ClipboardUrlReader.kt`; writing is done by the
analysis and history-details screens through `ClipboardManager`.
