# Active Context: 8VIM

## Current State (as of text-replacement session)

The project is at **version 0.17.0**. A full dependency upgrade was completed in the previous session. This session resolved all build warnings introduced by that upgrade.

## Current Work Focus

**Completed:** Automated Text Replacement feature fully implemented and tested.

### Summary of Changes Made This Session

| Component | File(s) | Detail |
|-----------|---------|--------|
| Preferences | `AppPrefs.kt` | Added `TextReplacement` inner class: `enabled` (Boolean) + `entries` (String — JSON-serialised Map) |
| Core manager | `ime/text/TextReplacementManager.kt` | Application-scoped service; persists abbreviation→expansion map; exposes `checkAndReplace(text)`, `addEntry`, `removeEntry`, `getEntries` |
| Application wiring | `VIM8Application.kt` | Registered `textReplacementManager` via `singletonOf(::TextReplacementManager)` in `appModules` Koin list |
| Keyboard integration | `ime/keyboard/text/KeyboardManager.kt` | `handleText()` calls `manager.checkAndReplace(text)` when feature is enabled; replaces with deletion + new commit |
| Drawable | `res/drawable/ic_find_replace.xml` | Material `find_replace` vector icon for settings row |
| Strings | `res/values/strings.xml` | All UI strings: title, enabled label, trigger hint, empty-state, add/save/cancel/delete, field labels/placeholders, blank-abbreviation error |
| Settings screen | `app/settings/TextReplacementScreen.kt` | Compose screen with SwitchPreference enable toggle, LazyColumn list, FAB (add), swipe-to-delete via IconButton, AlertDialog for add |
| Navigation | `app/Routes.kt` | Route constant `TEXT_REPLACEMENT = "settings/text-replacement"` + `composable()` registered in `AppNavHost` |
| Home entry | `app/settings/HomeScreen.kt` | `Preference` row with `ic_find_replace` icon navigates to `TEXT_REPLACEMENT` route |
| Tests — new | `ime/text/TextReplacementManagerSpec.kt` | Unit tests: addEntry, removeEntry, checkAndReplace (enabled/disabled, match/no-match, blank abbreviation guard) |
| Tests — updated | `ime/keyboard/text/KeyboardManagerSpec.kt` | Mocked `Context::textReplacementManager`; stubbed `prefs.textReplacement.enabled.get()` → false |

### Key Implementation Notes
- **Trigger:** replacement fires when the user commits a space (` `), newline, or punctuation character — the preceding word is checked against the abbreviation map.
- **Replace mechanism:** `EditorInstance.deleteSurroundingText(abbr.length + 1)` + `commitText(expansion + trigger)` keeps the trigger character.
- **Storage:** abbreviation map stored as `prefs.textReplacement.entries` (a JSON string via the app's custom `PreferenceModel`). This means entries survive app restart and are included in backup/restore.
- **Screen DSL note:** In the `Screen { }` builder lambda, `prefs` is only accessible inside the `content {}` block (it is the `PreferenceLayoutScope` receiver). State/composables that need to be shared with the FAB slot or AlertDialog must be declared in the outer `@Composable` builder scope. Composable string lookups (`stringRes(...)`) must be hoisted *out* of non-composable `onClick` lambdas.

### Pipeline Result
**BUILD SUCCESSFUL** — `compileDebugKotlin` + `testDebugUnitTest` (all tests pass)

## Dependency Upgrade Summary (from previous session)
| Component | Before | After |
|-----------|--------|-------|
| Gradle wrapper | 8.9 | 9.4.0 |
| Android Gradle Plugin | 8.7.2 | 8.13.2 |
| Kotlin | 2.0.21 | 2.2.0 |
| KSP | 2.0.21-1.0.28 | 2.2.0-2.0.2 |
| mannodermaus android-junit5 | 1.11.2.0 | 1.13.1.0 |
| AndroidX Activity | (prev) | 1.12.4 |
| AndroidX Core | (prev) | 1.17.0 |
| AndroidX Core Splashscreen | (prev) | 1.2.0 |
| AndroidX Lifecycle | (prev) | 2.10.0 |
| AndroidX Navigation | (prev) | 2.9.7 |
| AndroidX Compose | (prev) | 1.10.4 |
| AndroidX Compose Material3 | (prev) | 1.4.0 |
| AndroidX Compose Material Icons | (prev) | 1.7.8 |
| android-material | (prev) | 1.13.0 |
| androidx-appcompat | (prev) | 1.7.1 |
| Jackson | 2.13.5 | 2.18.6 (capped — 2.19+ requires minSdk 26) |
| MockK | 1.13.10 | 1.14.9 |
| commons-text | 1.12.0 | 1.15.0 |
| logback-classic (test) | 1.5.12 | 1.5.32 |
| targetSdk | 34 | 35 |

## Key Decisions & Considerations

### Architecture Decisions (unchanged)
- **Arrow.kt** is used pervasively for functional-style error handling (`Either`, `Option`). New code should follow this pattern rather than using exceptions for control flow.
- **Jetpack Compose** is the UI framework for settings/app screens, but the **keyboard views** (MainKeyboardView, NumberKeypadView, etc.) are still traditional Android Views.
- **CBOR caching** of parsed layouts is intentional for performance.

### Warning-Fix Notes
- **`android.newDsl=false`** kept intentionally — `android.newDsl=true` causes a `ClassCastException` in KGP 2.2.0 (`BaseAppModuleExtension` cast). The `@Suppress("DEPRECATION")` silences the resulting IDE warning.
- **`kotlin { compilerOptions { } }`** is the AGP 8+ / KGP 2+ replacement for `android { kotlinOptions { } }`. Both produce identical bytecode; the new DSL is the forward-compatible form.
- **Flaky test root cause** — `InputFeedbackController.performHapticFeedback` and `KeyboardController.interruptLongPress` both launch background coroutines on `Dispatchers.Default`. Tests verified mock calls without awaiting the coroutine. Fixed with `verify(timeout=2000)` and an extra `delay(200)` in the relevant test cases.
- **`InvalidManifestAttribute`** — `launchMode` and `theme` on `<activity-alias>` are silently ignored by Android (these attributes are inherited from `targetActivity`). The attributes were removed from `SettingsLauncherAlias`.

### Upgrade-Specific Notes (from previous session)
- **Jackson capped at 2.18.6** — Jackson 2.19+ requires Java 11 bytecode APIs unavailable below `minSdk 26`. If `minSdk` is ever raised to 26+, Jackson can be updated to 2.21+.
- **Lint disabled checks expanded** — AGP 8.13.2 + Kotlin 2.2.0 K2 UAST compatibility crashes affect `UElementAsPsi` and `Recycle`. Five pre-existing violations (`AndroidGradlePluginVersion`, `IntentFilterUniqueDataAttributes`, `ConfigurationScreenWidthHeight`, `LocalContextResourcesRead`, `UseKtx`) also suppressed. Address in follow-up PRs.

### Known Patterns & Preferences (unchanged)
- Preference keys follow `prefs_<group>_<subgroup>_<name>` convention
- Test files are named `*Spec.kt` (Kotest style)
- The `@optics` annotation (Arrow) on `KeyboardData` must not be removed
- Build variants: `debug` (.debug suffix), `rc` (.rc suffix), `release` (no suffix)

## Important File Locations

| Purpose | File |
|---------|------|
| IME core service | `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt` |
| Application entry | `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt` |
| All preferences | `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt` |
| Gesture processing | `8vim/src/main/kotlin/inc/flide/vim8/ime/actionlisteners/MainKeypadActionListener.kt` |
| Data model (keyboard) | `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/KeyboardData.kt` |
| Dependency versions | `gradle/libs.versions.toml` |
| App build config | `8vim/build.gradle.kts` |

## Next Steps (suggested)
- Fix lint violations currently suppressed in `8vim/build.gradle.kts`:
  - `ConfigurationScreenWidthHeight` — migrate `FloatingScope.kt` to `LocalWindowInfo`
  - `LocalContextResourcesRead` — fix `ImeSizing.kt` context resource access
  - `UseKtx` — migrate `ColorPreference.kt`, `LaunchUtils.kt`, `Layout.kt` to KTX extensions
  - `IntentFilterUniqueDataAttributes` — split data tags in `AndroidManifest.xml`
- Consider raising `minSdk` from 24 → 26 to unlock Jackson 2.19+ and other Java 11+ libraries
- Re-enable `UElementAsPsi` and `Recycle` lint checks once AGP fixes the Kotlin 2.2.0 K2 UAST issue
- Proper coroutine test infrastructure: inject `TestDispatcher` via DI so haptic/keyboard tests don't need `delay`/`timeout` workarounds
