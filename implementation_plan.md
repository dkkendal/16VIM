# Implementation Plan: Automated Text Replacement

[Overview]
Add an Automated Text Replacement feature that lets users define abbreviation → expansion pairs in Settings, and automatically replaces typed abbreviations with their full text when a trigger character (space, period, comma, etc.) is typed.

This feature is similar to "Text Shortcuts" found on iOS and macOS keyboards. When a user defines "omw" → "On my way!", typing `omw` followed by a space will instantly replace it with `On my way! `. The feature integrates with the existing `KeyboardManager` text-commit pipeline, stores its rules persistently in the existing `AppPrefs` / SharedPreferences system, and exposes a dedicated settings screen that fits the existing Jetpack Compose + `Screen {}` UI pattern.

The implementation is split into four layers:
1. **Storage** — two new preference keys in a new `AppPrefs.TextReplacement` inner class
2. **Core logic** — a new `TextReplacementManager` class with pure-Kotlin helper methods (testable without Android)
3. **IME integration** — hook into `KeyboardManager.handleText()` and wire the manager through `VIM8Application`
4. **Settings UI** — a new `TextReplacementScreen` composable with an add-entry dialog, entry list with delete, and a master enable switch; new route and home-screen entry

[Types]
No new Kotlin types or sealed classes are needed; one `AppPrefs.TextReplacement` inner class with two `PreferenceData` fields is introduced.

**AppPrefs.TextReplacement (inner class)**
- `enabled: PreferenceData<Boolean>` — master on/off switch; key `prefs_text_replacement_enabled`; default `false`
- `entries: PreferenceData<Set<String>>` — set of encoded pairs; key `prefs_text_replacement_entries`; default `emptySet()`

**Encoding convention (not a Kotlin type)**
Each element in the `entries` stringSet is `"<abbreviation>|||<expansion>"` where `|||` is the fixed separator constant `ENTRY_SEPARATOR = "|||"`. Abbreviations must not contain `|||` (validated on add). This convention is internal to `TextReplacementManager`.

**TextReplacementManager companion constants**
- `ENTRY_SEPARATOR: String = "|||"` — field separator used in encoded entries
- `TRIGGER_CHARS: Set<Char> = setOf(' ', '.', ',', '!', '?')` — characters that end a word and trigger replacement check

[Files]
New and modified files for the complete feature implementation.

**New files to create:**
- `8vim/src/main/kotlin/inc/flide/vim8/ime/text/TextReplacementManager.kt`  
  Core logic: parse entries, encode entries, add/remove/list entries, find replacement, commit replacement via InputConnection.
- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/TextReplacementScreen.kt`  
  Compose settings screen: enable switch, list of shortcut entries, add/edit dialog, delete per entry.
- `8vim/src/main/res/drawable/ic_find_replace.xml`  
  Simple vector drawable for the HomeScreen navigation icon (24dp, find-replace style icon).
- `8vim/src/test/kotlin/inc/flide/vim8/ime/text/TextReplacementManagerSpec.kt`  
  Kotest `FunSpec` unit tests covering pure logic: `parseEntries`, `encodeEntry`, `findReplacement`, `addEntry`, `removeEntry`.

**Existing files to modify:**
- `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt`  
  Add `val textReplacement = TextReplacement()` property and new `inner class TextReplacement` with `enabled` and `entries` preferences.
- `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`  
  Add `val textReplacementManager = lazy { TextReplacementManager(this) }` lazy property and `fun Context.textReplacementManager()` extension function.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt`  
  Inject `textReplacementManager` via context extension, add trigger check at end of `handleText()`.
- `8vim/src/main/kotlin/inc/flide/vim8/app/Routes.kt`  
  Add `const val TEXT_REPLACEMENT = "settings/text-replacement"` route constant and `composable` registration.
- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/HomeScreen.kt`  
  Add `Preference` navigation item for Text Replacement screen, with `ic_find_replace` icon.
- `8vim/src/main/res/values/strings.xml`  
  Add all string resources for the Text Replacement settings screen.

[Functions]
New functions and modifications to existing ones.

**New functions in `TextReplacementManager.kt`:**

| Function | Visibility | Signature | Purpose |
|---|---|---|---|
| `parseEntries` | `internal` | `(rawSet: Set<String>): Map<String, String>` | Splits each `"abbr|||expansion"` string into a map. Skips malformed entries. |
| `encodeEntry` | `internal` | `(abbreviation: String, expansion: String): String` | Returns `"$abbreviation$ENTRY_SEPARATOR$expansion"`. |
| `findReplacement` | `internal` | `(textBeforeCursor: String, map: Map<String, String>): Pair<Int, String>?` | Returns `(charsToDelete, replacementText)` if the word ending at the last TRIGGER_CHAR matches a key in `map`, else null. `charsToDelete = abbreviation.length + 1` (for the boundary char). |
| `getEntries` | `fun` | `(): List<Pair<String, String>>` | Returns all entries as sorted list of (abbreviation, expansion) pairs. |
| `addEntry` | `fun` | `(abbreviation: String, expansion: String)` | Validates abbreviation is non-empty and does not contain `ENTRY_SEPARATOR`, removes any existing entry for the same abbreviation, appends the new encoded entry to the preference set. |
| `removeEntry` | `fun` | `(abbreviation: String)` | Removes the encoded entry whose abbreviation prefix matches from the preference set. |
| `checkAndReplace` | `fun` | `()` | Gets `currentInputConnection()` and `textBeforeCursor(500)`, calls `findReplacement`, then calls `deleteSurroundingText` and `commitText` on the InputConnection if a match is found. |

**Modified functions:**

| Function | File | Change |
|---|---|---|
| `handleText` | `KeyboardManager.kt` | After `editorInstance.commitText(text)` and shift-reset, add: if `prefs.textReplacement.enabled.get()` is true AND `text.isNotEmpty()` AND `text.last() in TextReplacementManager.TRIGGER_CHARS`, call `textReplacementManager.checkAndReplace()`. |
| `AppNavHost` | `Routes.kt` | Register new `composable(Settings.TEXT_REPLACEMENT) { TextReplacementScreen() }`. |
| `HomeScreen` | `HomeScreen.kt` | Add `Preference` block with `iconId = R.drawable.ic_find_replace`, title from string res, `onClick = { navController.navigate(Routes.Settings.TEXT_REPLACEMENT) }`. |

[Classes]
New and modified classes with complete descriptions.

**New class: `TextReplacementManager`**
- File: `8vim/src/main/kotlin/inc/flide/vim8/ime/text/TextReplacementManager.kt`
- Package: `inc.flide.vim8.ime.text`
- Constructor: `(context: Context)` — stores nothing from context; context is used only for `appPreferenceModel()` delegation
- Key property: `private val prefs by appPreferenceModel()` — follows the same delegation pattern as `KeyboardManager`
- Companion object: declares `ENTRY_SEPARATOR` and `TRIGGER_CHARS` constants
- Does **not** extend any base class or implement any interface
- All pure-logic methods (`parseEntries`, `encodeEntry`, `findReplacement`) are `internal` so they can be directly tested via a test-subject pattern matching `SuggestionsManagerSpec`

**Modified class: `AppPrefs`**
- File: `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt`
- Add `val textReplacement = TextReplacement()` field in the body (alongside `layout`, `keyboard`, etc.)
- Add `inner class TextReplacement` with `enabled` (`boolean`) and `entries` (`stringSet`) preference fields. No version bump required (new keys default gracefully on first access).

**Modified class: `VIM8Application`**
- File: `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`
- Add `val textReplacementManager = lazy { TextReplacementManager(this) }` lazy property
- Add companion extension function `fun Context.textReplacementManager() = this.vim8Application().textReplacementManager` (at bottom of file, alongside `suggestionsManager`, `editorInstance`, etc.)

**Modified class: `KeyboardManager`**
- File: `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt`
- Add `private val textReplacementManager by context.textReplacementManager()` field (same delegation pattern as `suggestionsManager`)
- Modify `handleText()` to call `textReplacementManager.checkAndReplace()` when appropriate (see Functions section)

**New composable: `TextReplacementScreen`**
- File: `8vim/src/main/kotlin/inc/flide/vim8/app/settings/TextReplacementScreen.kt`
- Pattern: `Screen { title = ...; previewFieldVisible = true; content { ... } }` matching all other settings screens
- Inside `content {}`: `PreferenceGroup` with `SwitchPreference(prefs.textReplacement.enabled, ...)`, then `PreferenceGroup(title = "Shortcuts")` containing a "Add shortcut" `Preference` item with a trailing `+` icon and per-entry items that are clickable (to edit) and have a trailing delete `IconButton`
- Dialog state managed with `remember { mutableStateOf(...) }` — a single reusable `AlertDialog` with two `OutlinedTextField` inputs (abbreviation, expansion) used for **both** adding new and editing existing entries
- Dialog mode tracking: `editingAbbreviation: String?` (`null` = add mode, non-null = edit mode with the original abbreviation). Dialog title reads "Add Shortcut" or "Edit Shortcut" accordingly
- On dialog save: if `editingAbbreviation != null && editingAbbreviation != abbreviationInput`, call `textReplacementManager.removeEntry(editingAbbreviation)` first (to handle abbreviation rename), then call `textReplacementManager.addEntry(abbreviationInput, expansionInput)`
- Entry list built from `prefs.textReplacement.entries.observeAsState()` → `remember(entries) { textReplacementManager.parseEntries(entries).toList().sortedBy { it.first } }` for reactive updates
- Context accessed via `LocalContext.current`, manager via `remember { context.textReplacementManager().value }`

[Dependencies]
No new external library dependencies are required.

All logic uses:
- Existing Kotlin stdlib (`Set`, `Map`, string operations)
- Existing Android SDK (`InputConnection`, `SharedPreferences` — already used throughout)
- Existing Compose Material3 (`AlertDialog`, `OutlinedTextField`, `IconButton` — already in the Compose dependency)
- The `ic_find_replace.xml` drawable is a new first-party vector resource (no external icon library needed)

No changes to `gradle/libs.versions.toml` or `8vim/build.gradle.kts` are required.

[Testing]
Unit tests covering all pure-Kotlin logic in `TextReplacementManager`; Android-dependent methods (`checkAndReplace`) are left for integration testing.

**New test file:**
`8vim/src/test/kotlin/inc/flide/vim8/ime/text/TextReplacementManagerSpec.kt`

Tests follow the `SuggestionsManagerSpec` pattern: a private `TextReplacementManagerTestSubject` class replicates the internal pure logic to avoid constructing the real class (which needs Android `Context` for `appPreferenceModel()`).

Test contexts:
- `parseEntries — valid entries` — `"btw|||By the way"` → map `{"btw" → "By the way"}`
- `parseEntries — malformed entries are skipped` — entries without separator are ignored
- `parseEntries — empty set returns empty map`
- `encodeEntry — round-trip` — `encodeEntry("btw", "By the way")` round-trips through `parseEntries`
- `findReplacement — no match returns null` — unknown abbreviation, no entries
- `findReplacement — match at end of text` — `"btw "` with map `{"btw" → "By the way"}` → `(4, "By the way ")`
- `findReplacement — match after other words` — `"hello btw "` → still matches `"btw"`
- `findReplacement — boundary char preserved` — period, comma, `!`, `?` all preserved in replacement
- `findReplacement — no match when text ends with non-trigger char` — `"btw"` (no trailing space) → null
- `findReplacement — case-sensitive` — `"BTW"` does not match `"btw"` mapping
- `addEntry / removeEntry logic` — via `parseEntries` after encode: add "omw", remove "omw", double-add overwrites

[Implementation Order]
Implement in the sequence below to ensure each change compiles and can be tested before the next depends on it.

1. **Add `AppPrefs.TextReplacement`** — Add `inner class TextReplacement` and `val textReplacement = TextReplacement()` to `AppPrefs.kt`. Build verifies the model compiles.

2. **Create `TextReplacementManager.kt`** — New file with all logic: `parseEntries`, `encodeEntry`, `findReplacement`, `getEntries`, `addEntry`, `removeEntry`, `checkAndReplace`. Companion constants.

3. **Write `TextReplacementManagerSpec.kt`** — Unit tests for all internal/pure-logic functions using `TextReplacementManagerTestSubject`. Run `./gradlew testDebugUnitTest` to verify all tests pass.

4. **Wire into `VIM8Application.kt`** — Add `textReplacementManager` lazy property and `Context.textReplacementManager()` extension function.

5. **Integrate into `KeyboardManager.kt`** — Add `private val textReplacementManager by context.textReplacementManager()` field and modify `handleText()` to call `checkAndReplace()` on trigger chars.

6. **Create `ic_find_replace.xml`** — New vector drawable for the HomeScreen icon.

7. **Add string resources to `strings.xml`** — All screen title, group titles, preference labels, dialog strings.

8. **Create `TextReplacementScreen.kt`** — Full Compose screen following `Screen {}` pattern with switch preference, entry list, and add-entry dialog.

9. **Register route in `Routes.kt`** — Add `TEXT_REPLACEMENT` constant and `composable` registration in `AppNavHost`.

10. **Add HomeScreen navigation entry in `HomeScreen.kt`** — Add `Preference` item that navigates to the new route.

11. **Full build + test verification** — Run `./gradlew testDebugUnitTest lintDebug ktlintCheck` and verify the build is clean.

12. **Update memory bank** — Update `activeContext.md` and `progress.md` to reflect the new feature and its patterns.
