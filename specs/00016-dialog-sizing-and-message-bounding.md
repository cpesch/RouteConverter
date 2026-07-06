---
name: 00016-dialog-sizing-and-message-bounding
status: planned
phases_done: [grill-plan]
phases_next: [p1-windowbounds, p2-list-formatter, p3-windowhelper-wrappers, p4-joptionpane-migration, manual-ui-verification]
last_touched: 2026-07-06
---

# 00016 - Dialog sizing cleanup + message-content bounding

## Goal

Every dialog opens at a good default (all components visible), the user can
resize/move it, and the new size **and** position are stored and restored on the
next open — with a floor so a dialog can never be shrunk small enough to become
un-findable, and a ceiling so content can never grow a dialog past the screen.

Decided as a **real cleanup** (code changes, not an audit) after a grill pass on
2026-07-06. Behaviour target and every design fork below were chosen by the
maintainer during that grill.

## Context — three dialog-sizing surfaces, three failure modes

An inventory of all dialog-producing code found **three distinct surfaces**, each
with different current behaviour and a different target:

| Surface | Count | Today | Target |
|---|---|---|---|
| `SimpleDialog` subclasses | 17 | pack-to-fit; **size-restore silently broken**; no min-size | resizable + persist size/pos + min floor |
| `JOptionPane.showXXX` | 85 call sites / ~26 files | raw static calls; non-resizable; no persist; size fully content-driven | route through one wrapper; content bounded so never exceeds screen |
| Content feeders (`asDialogString`, `WindowHelper.handleThrowable` stacktrace) | 2 | unbounded lines → unbounded height; English-only | cap lists / scroll free-text; localize |

`min-size` only helps tier 1 (too-small). Tiers 2–3 are the **ceiling** problem
(too-big / off-screen).

### The tier-1 bug (root cause)

`common-gui/.../SimpleDialog.java` and `common-gui/.../SingleFrameApplication.java`
hand-roll near-identical restore/persist logic (`crop`, `getPreferencesX/Y`,
`putPreferencesSize`, the `width>120 && height>60` guard). The **frame** copy is
correct. The **dialog** copy is a broken fork:

- `SimpleDialog.getPreferenceWidth()` / `getPreferenceHeight()` are hardcoded
  `return -1` (`SimpleDialog.java:87-93`).
- `showWithPreferences()` writes size on `dispose()` (`putPreferencesSize`,
  key `name-width`/`name-height`) but the read stub returns `-1`, so `crop`
  yields `-1` and the packed size always stands.
- Net: dialogs **write their size to prefs but never read it back**. Only X/Y
  position is genuinely restored. (`crop(getName()+"width", …)` — the `"width"`
  there is only the crop *log-name*, not a pref key; the value comes from the
  `-1` stub.)

`SingleFrameApplication` does it right: `getPreferenceWidth/Height` actually read
prefs (`:180-182`) and it installs a `ComponentListener` for live persist on
move/resize (`:156-165`). Dialogs have no such listeners — divergent copies are
*why* the size-restore silently broke.

### The tier-3 overflow (content feeders)

- `Files.asDialogString(list, shorten)` (`common/.../io/Files.java:479`) joins a
  list as `'a',\n'b' and\n'c'` — **one `\n` per item, no cap**. Select 200 files →
  200-line label → packed dialog taller than the screen → title bar off-screen =
  the same "un-findable" failure, from the top end. `shorten` only caps per-item
  *width* (path-shorten to 60), not line *count*, and it applies `shortenPath()`
  (a path algorithm) to non-paths (URLs, error strings, hosts, theme names) →
  mis-shortens. `"and"/"none"/"null"` are hardcoded English in a user-facing
  string; the rest of the app is bundle-driven.
- `WindowHelper.handleThrowable` stuffs a full `printStackTrace()` into a
  `showMessageDialog` message — an unbounded-height, non-resizable dialog. Same
  overflow class. `WindowHelper` is thus both the natural home for the fix **and**
  an offender; it already centralizes `getFrame()` + `getFrame().getTitle()` as
  the standard parent/title.

## Decisions (locked in grill, 2026-07-06)

1. **Behaviour** — pack for default → user resize/move → persist size+position →
   restore next open, with a min-size floor. (Not audit-only; real cleanup.)
2. **Content ceiling = two primitives** — **cap** long *lists* (`first N` +
   localized "…and {0} more"); **scroll** long *free text* (stacktraces, multi-
   cause errors) in a `JScrollPane(JTextArea)` with a max preferred size
   (~600×400). Right feel per case: glanceable "are you sure?" vs full detail.
3. **min-size = packed preferred size.** After `pack()`, `setMinimumSize(getSize())`
   before applying persisted size; persisted restore clamps to
   `max(persisted, minimum)`. Encodes "every component visible" from the layout
   itself — zero per-dialog magic numbers. Plus a tiny absolute safety floor
   (never below 200×120) for any dialog that packs oddly small.
4. **FindPlaceDialog** — **remove** the hardcoded
   `contentPane.setPreferredSize(new Dimension(900, 540))`
   (`FindPlaceDialog.java:88`). Move the size intent to the **content component**:
   `tableResult.setPreferredScrollableViewportSize(...)` (or a `preferred-size` on
   its scrollpane in the `.form`), so pack yields a good default *from the table*
   and the min-size follows. General principle for the campaign: **size the
   content component, not the window.**
5. **Tier-1 code home = extract `WindowBounds` helper.** One impl taking
   `(Window, Preferences, keyPrefix)` for restore + live-persist + min clamp; both
   `SingleFrameApplication` and `SimpleDialog` call it. Kills the divergent fork
   (the thing that caused the bug). Maximized-state logic stays frame-only; the
   helper stays `Window`-shaped, key-prefix its only differentiator. (Chosen over
   in-place patch because divergent copies are the root cause; falls back to
   in-place only if the seam starts dragging frame-only state across.)
6. **Tier-2 strategy = migrate all 85 `JOptionPane.showXXX` sites** through
   `WindowHelper` wrappers (full migration, not staged/opportunistic).
7. **Wrapper API = semantic methods + overloads** — `showError / showWarning /
   showInformation / showConfirm(...)→int / showInput(...)→String`. Defaults:
   parent = `getFrame()`, title = owner's title; overloads for explicit
   `Window owner` and explicit title. Message typed `Object` (so both `String`
   and `JLabel` sites work); long **String** messages auto-wrapped in the scroll
   primitive, `Component` messages passed through untouched. Oddballs:
   `RouteConverter.java:250` pre-frame Java-version check → **null-safe overload**;
   `CatalogMirrorFrame` custom titles → migrate via the explicit-title overload,
   English titles kept (internal dev/mirror tool, no bundle).
8. **Tier-3 home = new GUI-side list formatter in `common-gui`** (has bundle
   access): localized joins, item cap + "…and {0} more", path-only shortening.
   Migrate the ~6 dialog-facing callers (the `shorten=true` sites: download
   actions, `FileOperations`, `RouteConverter`). Leave `Files.asDialogString` for
   **logging only** (English, uncapped) — rename it `asLogString` to make the
   log-vs-dialog split explicit. (`common` stays non-GUI/no-bundle by rule.)
9. **Verification = extract pure seams + unit-test them, manual-verify the Swing
   shell.** Codecov is advisory and GUI is untestable headless, so pull the
   bug-prone math out of the paint layer:
   - Unit-testable (no display): `WindowBounds.computeBounds(packed, min, saved,
     screen) → Rectangle` (crop/clamp); the GUI list formatter (cap + "…and N
     more" + path-only shorten) as pure strings like `FilesExtensionTest`; the
     message-bounding *decision* (`String too long → build JScrollPane` vs
     pass-through) as a pure function.
   - Manual: run the app (Java 21), open **all 17** SimpleDialogs + **one
     JOptionPane per type** (error/info/warn/confirm/input) + one long-list + one
     long-stacktrace. Check: default all-visible; resize→reopen persists;
     min-floor holds; long list caps; long stacktrace scrolls. Drive via
     `/verify`.
10. **Delivery = spec 00016 + 4 phased, squash-merged PRs** in order P1→P4 (bug
    fix lands first, big mechanical migration last after infra is proven).

## Phase plan

### P1 — tier 1: `WindowBounds` + size-restore fix + min-size + FindPlaceDialog
- Extract `WindowBounds(Window, Preferences, keyPrefix)`: `restore()` (pack →
  set min = packed → apply clamped saved size → apply clamped saved location),
  `installPersistence()` (ComponentListener writing size+location live).
- Pure `computeBounds(...)` seam for unit tests.
- Rewire `SingleFrameApplication.openFrame()` and `SimpleDialog.showWithPreferences()`
  to call it; delete the dialog's `-1` stubs and its bespoke persist code.
  Maximized-state stays in `SingleFrameApplication`.
- FindPlaceDialog: drop the 900×540; set the table viewport preferred size.
- **Ships the actual bug fix; self-contained.**

### P2 — tier 3a: GUI list formatter
- New `common-gui` formatter: localized join + item cap ("…and {0} more") +
  path-only shorten. Bundle keys for the join words + "more".
- Rename `Files.asDialogString` → `asLogString`; update its ~4 logging callers
  and `FilesExtensionTest`/`FilesFilesystemTest`.
- Migrate the ~6 dialog-facing (`shorten=true`) callers to the new formatter.
- Pure-logic, fully unit-tested.

### P3 — tier 2 infra: `WindowHelper` wrappers
- `showError/showWarning/showInformation/showConfirm/showInput` with defaults +
  owner/title overloads + null-safe overload; message `Object`.
- Scroll primitive for long `String` messages (bounded `JScrollPane(JTextArea)`).
- Convert `handleThrowable` / `handleOutOfMemoryError` to use it (dogfood).
- No mass migration yet.

### P4 — tier 2 bulk: migrate all 85 `JOptionPane` sites
- Replace every `JOptionPane.showXXX(...)` with the matching wrapper (parent
  varies: `getFrame()`, `getDialog()`, local `frame`, `null`, component messages —
  all covered by the P3 overloads).
- CatalogMirrorFrame via explicit-title overload (English kept).
- Mechanical, large diff; every site now bounded + centralized.

## Risks / notes

- **Stale prefs (back-compat):** existing users have `name-width`/`name-height`
  written-but-never-read for years. P1 starts reading them → first post-upgrade
  open may restore an old size. The min/screen clamp bounds it both ways —
  acceptable, no migration needed.
- **Frame blast radius:** P1 touches `SingleFrameApplication` (the app's main
  window). Behaviour is preserved because the frame path is the *reference* the
  helper is modelled on; maximized-state stays out of the helper. Manual-verify
  the main frame's restore/maximize alongside the dialogs.
- **85-site migration churn:** P4 is large but purely mechanical; kept last so the
  wrapper API is settled before the bulk edit.
- **`shortenPath` on non-paths** is fixed as a side effect of the GUI formatter
  (P2) shortening only actual paths.
