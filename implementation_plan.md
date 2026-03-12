# Implementation Plan: Word Prediction for 8VIM

[Overview]
Add word prediction (auto-suggestions) to the 8VIM keyboard using Android's built-in TextServicesManager/SpellCheckerSession API.

The 8VIM keyboard previously had a partial implementation of word prediction in the `predictive_text` branch (abandoned ~3 years ago). That branch was built on the old Java/XML-View architecture, used a bundled 333k-word English-only CSV dictionary loaded synchronously at startup, and wired everything through `onCreateCandidatesView()`. The current codebase is entirely Kotlin/Jetpack Compose, uses a service/manager pattern via `VIM8Application` lazy singletons, and manages all UI via composables inside `Vim8ImeService`.

This implementation rebuilds word prediction from scratch using the modern architecture. Instead of a bundled CSV, we use Android's `TextServicesManager` + `SpellCheckerSession` — a zero-APK-size approach that supports any language the device has a dictionary installed for. The suggestion strip is a Compose component embedded directly in the keyboard's `ImeUi` composable column, appearing above the xpad when typing in TEXT mode. Tapping a suggestion deletes the current partial word and commits the full suggestion + a trailing space. A toggle in Settings (Keyboard screen) lets users enable/disable the feature.

[Types]
No new sealed classes or enums are needed; the main data structures are Kotlin data flows and standard Android types.

- `StateFlow<List<String>>` — emitted by `SuggestionsManager` to hold the current 3 suggestion strings (empty list when no suggestions or prediction disabled)
- `TextInfo(word: String)` — Android built-in type passed to `SpellCheckerSession.getSuggestions()`
- `SuggestionsInfo[]` — Android built-in callback result type from `SpellCheckerSession`
- `currentWordLength: Int` — mutable int field on `SuggestionsManager` tracking how many characters of the current partial word are before the cursor (needed for deletion on suggestion tap)
- New preference: `wordPrediction.enabled: Boolean` — stored as `"prefs_word_prediction_enabled"` in SharedPreferences

[Files]
New files to create and existing files to modify.

**New Files:**
- `8vim/src/main/kotlin/inc/flide/vim8/ime/nlp/SuggestionsManager.kt`
  Purpose: Wraps Android's `TextServicesManager`/`SpellCheckerSession` lifecycle, exposes `suggestions: StateFlow<List<String>>` and `currentWordLength: Int`, provides `onTextBeforeCursor(text: CharSequence)` and `clearSuggestions()` methods.

- `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/view/SuggestionsBar.kt`
  Purpose: Jetpack Compose `@Composable` that renders a horizontal row of up to 3 suggestion chips. Observes `SuggestionsManager.suggestions` flow. Each chip calls `commitSuggestion()` which deletes the current partial word and commits the chosen word + space via `InputConnection`.

- `8vim/src/test/kotlin/inc/flide/vim8/ime/nlp/SuggestionsManagerSpec.kt`
  Purpose: Unit tests for word extraction logic inside `SuggestionsManager` (pure function — no Android dependencies).

**Modified Files:**
- `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt`
  Change: Add `inner class WordPrediction` with `enabled = boolean(key = "prefs_word_prediction_enabled", default = false)`. Add `val wordPrediction = WordPrediction()` field at the top of `AppPrefs`. Bump model version from `8` to `9`.

- `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`
  Change: Add `val suggestionsManager = lazy { SuggestionsManager(this) }`. Add `fun Context.suggestionsManager()` extension at the bottom of the file.

- `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt`
  Changes:
  1. Add `private val suggestionsManager by suggestionsManager()` property delegate.
  2. Override `onUpdateSelection()` to read `getTextBeforeCursor(500, 0)` from the current input connection and forward it to `suggestionsManager.onTextBeforeCursor()`. Only call this when `activeState.imeUiMode == ImeUiMode.TEXT`.
  3. In `ImeUi()` composable: observe `prefs.wordPrediction.enabled` and conditionally show `SuggestionsBar()` above the keyboard when enabled and in TEXT mode.
  4. Override `onFinishInput()` to call `suggestionsManager.clearSuggestions()`.

- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/KeyboardScreen.kt`
  Change: Add a new `PreferenceGroup` titled `"Word Prediction"` near the top of the screen content block, containing a single `SwitchPreference` wired to `prefs.wordPrediction.enabled`.

- `8vim/src/main/res/values/strings.xml`
  Change: Add 3 string resources:
  ```xml
  <string name="settings__keyboard__word_prediction__group__title">Word Prediction</string>
  <string name="settings__keyboard__word_prediction__enabled__title">Enable word prediction</string>
  <string name="settings__keyboard__word_prediction__enabled__summary__on">Word suggestions will appear above the keyboard</string>
  <string name="settings__keyboard__word_prediction__enabled__summary__off">No word suggestions shown</string>
  ```

- `memory-bank/activeContext.md` and `memory-bank/progress.md`
  Change: Update to reflect the new feature once implemented.

[Functions]
New functions and modifications to existing ones.

**New Functions:**

- `SuggestionsManager.onTextBeforeCursor(text: CharSequence): Unit`
  File: `ime/nlp/SuggestionsManager.kt`
  Extracts the current word being typed (everything after the last whitespace), stores its length in `currentWordLength`, and calls `spellCheckerSession?.getSuggestions(TextInfo(word), 3)` if the word is non-empty. If the word is empty, clears suggestions immediately.

- `SuggestionsManager.clearSuggestions(): Unit`
  File: `ime/nlp/SuggestionsManager.kt`
  Sets `_suggestions.value = emptyList()` and resets `currentWordLength = 0`.

- `SuggestionsManager.extractCurrentWord(text: String): String` (private)
  File: `ime/nlp/SuggestionsManager.kt`
  Pure function. Returns `""` if `text` is empty or ends with whitespace; otherwise returns the substring after the last whitespace character.

- `SuggestionsManager.initSession(): Unit` (private)
  File: `ime/nlp/SuggestionsManager.kt`
  Closes any existing `SpellCheckerSession` and opens a new one via `textServicesManager.newSpellCheckerSession(null, null, this, true)`.

- `SuggestionsManager.closeSession(): Unit` (private)
  File: `ime/nlp/SuggestionsManager.kt`
  Closes the `SpellCheckerSession` and calls `clearSuggestions()`.

- `SuggestionsBar(): Unit` (@Composable)
  File: `ime/keyboard/view/SuggestionsBar.kt`
  Reads `suggestionsManager().suggestions.collectAsState()`. If the list is empty, returns without rendering. Otherwise renders a `LazyRow` (or `Row`) of `SuggestionChip` composables, each calling `commitSuggestion()` on click.

- `commitSuggestion(context: Context, suggestionsManager: SuggestionsManager, suggestion: String): Unit` (private top-level)
  File: `ime/keyboard/view/SuggestionsBar.kt`
  Gets `Vim8ImeService.currentInputConnection()`, deletes `suggestionsManager.currentWordLength` characters before cursor, commits `"$suggestion "`, then calls `suggestionsManager.clearSuggestions()`.

**Modified Functions:**

- `Vim8ImeService.ImeUi()` (@Composable)
  File: `Vim8ImeService.kt`
  Add observation of `prefs.wordPrediction.enabled` (via `observeAsState()`). At the top of the composable block, before the `when` statement on `state.imeUiMode`, conditionally render `SuggestionsBar()` when enabled and `state.imeUiMode == ImeUiMode.TEXT`.

- `Vim8ImeService.onCreateCandidatesView()`
  File: `Vim8ImeService.kt`
  Currently returns `null`. No change needed — suggestions strip is embedded in the keyboard Compose UI, not the Android candidates area.

- `AppPrefs.migrate()` (override)
  File: `AppPrefs.kt`
  No migration needed for new keys; version bumped from `8` → `9`, and the `else -> entry.keepAsIs()` fallback handles the new version.

[Classes]
New classes and key modifications.

**New Classes:**

- `SuggestionsManager` in `inc/flide/vim8/ime/nlp/SuggestionsManager.kt`
  Implements: `SpellCheckerSession.SpellCheckerSessionListener`
  Constructor: `(private val context: Context)`
  Key fields:
  - `private val textServicesManager: TextServicesManager` — obtained from system services
  - `private var spellCheckerSession: SpellCheckerSession?` — nullable, recreated when locale changes
  - `private val _suggestions = MutableStateFlow<List<String>>(emptyList())`
  - `val suggestions: StateFlow<List<String>>` — public read-only view
  - `var currentWordLength: Int = 0` — how many chars of current partial word are before cursor
  - `private val mainHandler = Handler(Looper.getMainLooper())` — to post results to main thread
  Key methods:
  - `onTextBeforeCursor()`, `clearSuggestions()`, `extractCurrentWord()` (see Functions)
  - `onGetSuggestions(results: Array<SuggestionsInfo>?)` — SpellChecker callback, posts results to main thread via handler
  - `onGetSentenceSuggestions()` — SpellChecker callback, no-op
  Init block: observes `prefs.wordPrediction.enabled` to init/close session accordingly.

**Modified Classes:**

- `AppPrefs` in `AppPrefs.kt`
  Add nested `inner class WordPrediction` with `enabled: PreferenceData<Boolean>`.
  Add `val wordPrediction = WordPrediction()` at class body level.
  Bump `PreferenceModel(8)` → `PreferenceModel(9)`.

- `VIM8Application` in `VIM8Application.kt`
  Add `val suggestionsManager = lazy { SuggestionsManager(this) }`.

[Dependencies]
No new third-party dependencies are required.

The `android.view.textservice.*` package (TextServicesManager, SpellCheckerSession, TextInfo, SuggestionsInfo) is part of the Android SDK (available since API 14, well within minSdk 24). `READ_USER_DICTIONARY` permission is NOT required for `TextServicesManager` — the spell checker accesses the system dictionary internally without the app needing that permission. The `apache-commons-text` library (LevenshteinDistance from old branch) is already present in `libs.versions.toml` and `build.gradle.kts` as a production dependency — it can remain (it's used nowhere currently but adding a removal is out of scope). No CSV asset needs to be added.

[Testing]
Unit tests for the pure word-extraction function and integration spec for the manager's StateFlow behavior.

**New test file:**
`8vim/src/test/kotlin/inc/flide/vim8/ime/nlp/SuggestionsManagerSpec.kt`

Test cases (using Kotest BehaviorSpec pattern consistent with the rest of the test suite):
- `extractCurrentWord("")` → `""`
- `extractCurrentWord("hello ")` → `""` (ends with space — no current word)
- `extractCurrentWord("hello")` → `"hello"`
- `extractCurrentWord("hello world")` → `"world"`
- `extractCurrentWord("hello\nworld")` → `"world"` (newline is whitespace)
- `onTextBeforeCursor("hello ")` → `currentWordLength == 0`, `suggestions == emptyList()`
- `onTextBeforeCursor("hello wor")` → `currentWordLength == 3`, suggestions request queued
- `clearSuggestions()` → `suggestions == emptyList()`, `currentWordLength == 0`

The `SpellCheckerSession` should be mocked (MockK) to verify `getSuggestions()` is called with the correct `TextInfo` and suggestion count. The `TextServicesManager` system service call should be mocked at the context level.

[Implementation Order]
Steps ordered to minimize compile errors and allow incremental validation.

1. Add string resources to `strings.xml` (no code dependencies, compiles alone)
2. Add `WordPrediction` inner class and `wordPrediction` field to `AppPrefs.kt`; bump version to `9` (establishes the preference key before any observers)
3. Create `SuggestionsManager.kt` with full implementation (depends on `AppPrefs.wordPrediction`)
4. Add `val suggestionsManager = lazy { SuggestionsManager(this) }` to `VIM8Application.kt`; add `fun Context.suggestionsManager()` extension
5. Create `SuggestionsBar.kt` Compose component (depends on `SuggestionsManager` and `Vim8ImeService`)
6. Modify `Vim8ImeService.kt`: add `suggestionsManager` delegate, override `onUpdateSelection()`, integrate `SuggestionsBar()` into `ImeUi()`, override `onFinishInput()` to clear suggestions
7. Add `SwitchPreference` for word prediction to `KeyboardScreen.kt`
8. Write unit tests in `SuggestionsManagerSpec.kt`
9. Run `./gradlew testDebugUnitTest lintDebug ktlintCheck` to validate
10. Update memory-bank `activeContext.md` and `progress.md`
