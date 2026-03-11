# Progress: 8VIM

## What Works (Implemented & Functional)

### Core Keyboard Engine
- ✅ Radial gesture input via `FingerPosition` movement sequences
- ✅ Full 6-layer keyboard support (FIRST through SIXTH + HIDDEN)
- ✅ Long-press detection (500ms initiation, 50ms repeat)
- ✅ Full-rotation gesture for Shift toggle
- ✅ Shift state machine: OFF → ON (shift) → ENGAGED (caps lock) → OFF
- ✅ Ctrl state toggle (for word-based cursor movement)
- ✅ YAML layout files parsed and validated against JSON schema
- ✅ CBOR caching of parsed layouts for fast subsequent loads
- ✅ Layout conflict detection (prevents duplicate movement sequence mappings)
- ✅ Layout cache invalidation on app version change

### Keyboard Views / Modes
- ✅ **MainKeyboardView** — primary gesture input
- ✅ **NumberKeypadView** — numeric input, auto-activated for number/phone/datetime fields
- ✅ **SelectionKeypadView** — text selection with shift state
- ✅ **ClipboardKeypadView** — clipboard history viewing and pasting
- ✅ **SymbolKeypadView** — symbol / special character input

### Input Actions
- ✅ Text character input (`INPUT_TEXT`)
- ✅ Key event dispatching (`sendDownAndUpKeyEvent`, `sendKey`)
- ✅ Backspace with word-boundary awareness (`BreakIteratorGroup`)
- ✅ Cursor movement (single character and word-at-a-time)
- ✅ Cut / Copy / Paste via `InputConnection`
- ✅ IME action-aware Enter (`commitImeOptionsBasedEnter`)
- ✅ External emoticon keyboard switching
- ✅ Selection anchor switching

### App & Settings
- ✅ First-run IME setup wizard (`SetupScreen`)
- ✅ Full settings screen with Jetpack Compose + Navigation
- ✅ Custom layout file selector (`LayoutFileSelector`)
- ✅ Dark / Light / System theme support
- ✅ Keyboard height adjustment
- ✅ Sidebar visibility and side (left/right) preference
- ✅ Circle radius and center offset customization
- ✅ Trail visibility and color (random or fixed)
- ✅ Sector icons and letter-on-wheel display toggles
- ✅ Sound and haptic feedback toggles
- ✅ Emoticon keyboard selection
- ✅ Clipboard history enable/disable
- ✅ Preference migration system (version 3)

### Infrastructure
- ✅ Unit test suite (Kotest + JUnit 5 + MockK)
- ✅ Android instrumented tests (Espresso + JUnit 4)
- ✅ Jacoco code coverage reporting
- ✅ KtLint code formatting
- ✅ Checkstyle for Java sources
- ✅ Android Lint with configured suppression
- ✅ Fastlane CI/CD pipeline
- ✅ Codecov integration
- ✅ F-Droid and Google Play distribution

## What Is Left To Build / Known Issues

### Known Bugs (from recent changelogs)
- ✅ ~~IMEs listing not working on Android 14~~ — Fixed in v0.16.3 (#425)
- ✅ ~~Extra layers not displayed~~ — Fixed in v0.16.3 (#427)

### Outstanding / Potential Work
- ⬜ Additional language layout files (currently primarily English)
- ⬜ Accessibility improvements (TalkBack compatibility, content descriptions)
- ⬜ Dependency updates: Kotlin 1.8.21 → newer, Compose 1.5.2 → newer, Arrow 1.2.0 → newer
- ⬜ Android 15+ compatibility verification
- ⬜ Word suggestion / NLP integration (architecture has `ime/nlp/` package — only `BreakIteratorGroup` exists)
- ⬜ Potential Compose migration for keyboard views (currently all traditional Views)
- ⬜ UI tests for keyboard gestures (currently no Espresso/UI tests for gesture sequences)

## Current Status

**Version**: 0.17.0  
**Branch**: main (`a6139c0b`)  
**State**: Stable / maintenance mode — no active feature development detected  
**Distribution**: Available on F-Droid and Google Play

## Evolution of Key Decisions

| Decision | Rationale |
|----------|-----------|
| Arrow.kt for functional error handling | Avoids exception-based flow; `Either` makes error paths explicit in signatures |
| CBOR cache for layouts | YAML parsing is expensive; CBOR is faster for deserialization on app start |
| 6-layer system | Maximises character density on a gesture keyboard without increasing gesture complexity disproportionately |
| Compose for settings, Views for keyboard | Keyboard views need precise touch event control and canvas drawing — Compose not yet suitable for this use case |
| Custom `PreferenceModel` over `DataStore` directly | Provides reactive observation, type safety, and migration support in one API |
| KSP + Arrow optics | Enables safe immutable updates to `KeyboardData` without boilerplate — critical for the functional layout-merging pipeline |

## Test Coverage Areas

Tests exist in `8vim/src/test/kotlin/inc/flide/vim8/` covering:
- `arbitraries/` — Kotest property test arbitraries
- `datastore/nodel/` — PreferenceModel and PreferenceSerDe specs
- `geometry/` — Circle geometry spec
- `ime/` — KeyboardTheme, LayoutLoader, action listener specs
- `ime/layout/` — AvailableLayouts, Layout, models (CustomKeycode, Direction, FingerPosition, KeyboardData, Quadrant)
- `ime/layout/models/yaml/` — Action, ExtraLayer, Flags specs
- `ime/layout/parsers/` — CacheParser spec
- `ime/nlp/` — BreakIteratorGroup spec
- `ime/parsers/` — YAML parser spec
- `keyboardhelpers/` — InputMethodViewHelper spec
- `utils/` — GeometricUtilities spec

Test resources in `8vim/src/test/resources/`:
- `extra_layers.yaml`, `hidden_layer.yaml`, `multiple_layers.yaml`, `no_layers.yaml`, `one_layer.yaml` — layout fixture files
- `valid_file.yaml`, `invalid_file.yaml`, `invalid_file.xml` — parser validation fixtures
