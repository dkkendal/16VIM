# System Patterns: 8VIM

## Architecture Overview

8VIM follows a standard Android IME architecture with a clean Kotlin-first codebase. The system is organized into these primary layers:

```
┌─────────────────────────────────────────┐
│          VIM8Application (App)          │  Application-level singletons
├─────────────────────────────────────────┤
│       MainInputMethodService (IME)      │  Core Android IME service
├─────────────┬───────────────────────────┤
│   Views     │   ActionListeners         │  UI + Input handling
├─────────────┴───────────────────────────┤
│          IME Layout Engine              │  Gesture → action mapping
├─────────────────────────────────────────┤
│        YAML/CBOR Layout Parser          │  Layout data loading
├─────────────────────────────────────────┤
│        AppPrefs / Datastore             │  Preferences & persistence
└─────────────────────────────────────────┘
```

## Key Components

### 1. `MainInputMethodService` (`MainInputMethodService.kt`)
- Extends Android's `InputMethodService`
- Central orchestrator for all keyboard behaviour
- Manages keyboard view switching (main ↔ number ↔ selection ↔ clipboard ↔ symbols)
- Tracks **shift state** (`OFF` → `ON` → `ENGAGED` / Caps Lock), **ctrl state**
- Delegates input actions to the underlying `InputConnection`
- Lifecycle: `onCreate` → `onCreateInputView` → `onStartInputView` → `onFinishInput` → `onDestroy`

### 2. `VIM8Application` (`VIM8Application.kt`)
- Application singleton
- Lazily initialises `Cache` (CBOR-backed) and `YamlLayoutLoader`
- Initialises `KeyboardTheme` and applies theme mode on start
- Provides `Context.cache()` and `Context.layoutLoader()` extension functions

### 3. Gesture / Movement Model
```
FingerPosition enum:
  NO_TOUCH | INSIDE_CIRCLE | TOP | LEFT | BOTTOM | RIGHT | LONG_PRESS | LONG_PRESS_END

MovementSequence = List<FingerPosition>

KeyboardData:
  actionMap: Map<MovementSequence, KeyboardAction>
  characterSets: List<CharacterSet>   // one per visible layer
  info: LayoutInfo
```

A character is typed by the user completing a **MovementSequence**; when the sequence matches a key in `actionMap`, the corresponding `KeyboardAction` is executed.

### 4. Layer System
- **7 levels**: `HIDDEN`, `FIRST` (default), `SECOND` through `SIXTH`
- Layer switching uses a **rotation or prefix sequence** (e.g., BOTTOM→INSIDE_CIRCLE→BOTTOM enters Layer 2)
- `LayerLevel.MovementSequences` is a compile-time map of layer-unlock sequences
- `MainKeypadActionListener.findLayer()` determines the active layer from current movement state

### 5. `MainKeypadActionListener`
- Processes all touch events for the main keyboard view
- Maintains `movementSequence: MutableList<FingerPosition>` 
- Detects **long press** via `Handler` with 500ms initiation, 50ms continuation
- Detects **full rotation** (shift toggle) via a fixed set of 8 circular movement sequences
- On `movementEnds()`, looks up the completed sequence in `actionMap` and dispatches the action

### 6. Layout Loading Pipeline
```
YAML file (user / embedded)
        ↓
   YamlParser.readKeyboardData()
        ↓
   YamlLayoutLoader.loadKeyboardData()   (merges with common layout data)
        ↓
   Cache (CBOR serialization) ──→ reused on subsequent starts
        ↓
   KeyboardData (in-memory action map)
        ↓
   MainKeypadActionListener.rebuildKeyboardData()
```

- Common layout data (sector buttons, d-pad, special gestures) is loaded from raw resources
- User layout is layered on top with conflict checking

### 7. `AppPrefs` / Datastore
- `AppPrefs` extends `PreferenceModel` — a custom reactive preference system
- Preferences are grouped hierarchically: `layout`, `theme`, `clipboard`, `keyboard`, `inputFeedback`, `internal`
- Supports **migration** (currently version 3) for renaming/transforming old preference keys
- `observeAsState()` provides Compose-compatible reactive observation

### 8. Settings / App UI
- Built with **Jetpack Compose** + **Navigation Compose**
- `MainActivity` uses `Routes.AppNavHost` to navigate between:
  - `Routes.Setup.Screen` — First-run IME setup wizard
  - `Routes.Settings.Home` — Main settings screen
- `LocalNavController` is a `CompositionLocal` providing navigation throughout the UI
- `SettingsFragment` and `LayoutFileSelector` handle layout selection

### 9. Clipboard Service
- `ClipboardManagerService` manages clipboard history
- Implements `ClipboardHistoryListener` pattern
- `ClipboardKeypadView` displays history items

## Design Patterns Used

| Pattern | Where |
|---------|-------|
| **Singleton (WeakReference)** | `VIM8Application` global reference |
| **Lazy initialization** | `cache`, `layoutLoader` in `VIM8Application` |
| **Sealed class / enum state machine** | `MainInputMethodService.State` (shift), `MovementSequenceType` |
| **Observer pattern** | `PreferenceObserver`, `observeAsState()` |
| **Functional programming (Arrow.kt)** | `Either`, `Option`, optics throughout layout loading and action processing |
| **Strategy pattern** | `LayoutParser` interface with `YamlParser` / `CborParser` implementations |
| **Command pattern** | `KeyboardAction` containing `KeyboardActionType` + keycode/text |
| **Decorator/Merge** | `loadKeyboardData` merges common + user-specific layouts |

## Critical Data Flows

### Typing a Character
1. User touches screen → `MainKeyboardView` records `FingerPosition` changes
2. `MainKeypadActionListener.movementContinues()` appends to `movementSequence`
3. On `movementEnds()` → `processMovementSequence()` looks up `actionMap`
4. If `KeyboardActionType.INPUT_TEXT` → `handleInputText()` → `MainInputMethodService.sendText()`
5. If key event → `handleInputKey()` → `sendDownAndUpKeyEvent()`

### Loading a Layout
1. `MainInputMethodService.onCreateInputView()` calls `safeLoadKeyboardData(layoutLoader, this)`
2. `YamlLayoutLoader.loadKeyboardData(inputStream)` parses YAML + merges common data
3. Resulting `KeyboardData` passed to `MainKeypadActionListener.rebuildKeyboardData()`
4. All subsequent gesture lookups use the new `actionMap`

### Theme Application
1. `prefs.theme.mode.get()` read at app start → `AppCompatDelegate.setDefaultNightMode()`
2. `KeyboardTheme.initialize()` sets up color scheme
3. `KeyboardTheme.onChange {}` lambda updates navigation bar color dynamically
