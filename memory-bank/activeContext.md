# Active Context: 8VIM

## Current State (as of dependency upgrade session)

The project is at **version 0.17.0**. A full dependency upgrade has just been completed, bringing all toolchain and library versions to their latest compatible releases.

## Current Work Focus

**Completed:** Full dependency upgrade (Gradle, AGP, Kotlin, AndroidX, third-party libs, targetSdk).

### Summary of Changes Made
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

### Files Modified
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.4.0
- `gradle/libs.versions.toml` — all version bumps above
- `8vim/build.gradle.kts` — targetSdk 35, lint disable additions
- `8vim/src/test/kotlin/inc/flide/vim8/ime/layout/models/yaml/FlagsSpec.kt` — test fix

## Key Decisions & Considerations

### Architecture Decisions (unchanged)
- **Arrow.kt** is used pervasively for functional-style error handling (`Either`, `Option`). New code should follow this pattern rather than using exceptions for control flow.
- **Jetpack Compose** is the UI framework for settings/app screens, but the **keyboard views** (MainKeyboardView, NumberKeypadView, etc.) are still traditional Android Views.
- **CBOR caching** of parsed layouts is intentional for performance.

### Upgrade-Specific Notes
- **Jackson capped at 2.18.6** — Jackson 2.19+ requires Java 11 bytecode APIs unavailable below `minSdk 26`. If `minSdk` is ever raised to 26+, Jackson can be updated to 2.21+.
- **FlagsSpec test fix** — Jackson 2.16+ changed `StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` to disabled by default (security hardening). The two failing tests that asserted exact error messages with source snippets were changed to `shouldStartWith` on just the meaningful error prefix.
- **Lint disabled checks expanded** — AGP 8.13.2 introduced internal lint crashes when used with Kotlin 2.2.0's K2 compiler (affecting `UElementAsPsiDetector` and `CleanupDetector`). Five new lint checks now flag pre-existing code (`AndroidGradlePluginVersion`, `IntentFilterUniqueDataAttributes`, `ConfigurationScreenWidthHeight`, `LocalContextResourcesRead`, `UseKtx`) — all suppressed for this upgrade PR. These should be addressed in follow-up PRs.
- **IDE sync issue** — Android Studio shows "Unresolved reference 'material'" at the `libs.androidx.compose.material.icons.core` accessor in `build.gradle.kts`. This is an IDE version catalog accessor resolution bug and does **not** affect actual Gradle builds. Trigger `File > Sync Project with Gradle Files` to resolve.

### Known Patterns & Preferences (unchanged)
- Preference keys follow `prefs_<group>_<subgroup>_<name>` convention
- Test files are named `*Spec.kt` (Kotest style)
- The `@optics` annotation (Arrow) on `KeyboardData` must not be removed
- Build variants: `debug` (.debug suffix), `rc` (.rc suffix), `release` (no suffix)

## Important File Locations

| Purpose | File |
|---------|------|
| IME core service | `8vim/src/main/kotlin/inc/flide/vim8/MainInputMethodService.kt` |
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
- Re-enable `UElementAsPsi` and `Recycle` lint checks once AGP fixes the Kotlin 2.2.0 K2 UAST compatibility issue
