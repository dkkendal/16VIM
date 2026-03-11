# Active Context: 8VIM

## Current State (as of Memory Bank initialization)

The project is at **version 0.17.0**, which was recently bumped after a series of bug-fix releases (v0.16.x). The most recent meaningful changes were:

- **fix**: IMEs listing not working on Android 14 (#425)
- **fix**: Extra layers not displayed (#427)
- **chore**: Switch to Matrix notification for CI (#431)
- **chore**: Fix typo (HEAD — `a6139c0b`)

The codebase is in a **stable maintenance state** — recent commits are primarily chores (CI config, notifications, version bumps) with no active feature work visible in the recent log.

## Current Work Focus

No active feature branch is currently in progress (based on `git log`). The project is on the `main` branch at `a6139c0b`.

The Memory Bank is being **initialized for the first time** during this session.

## Recent Changes (v0.16.x → v0.17.0)
- Bumped version to 0.17.0 (MAJOR=0, MINOR=17, PATCH=0, RC=0)
- Fixed Android 14 (API 34) IME listing regression
- Fixed extra layers not being displayed in the keyboard
- Migrated CI notifications to Matrix

## Key Decisions & Considerations

### Architecture Decisions
- **Arrow.kt** is used pervasively for functional-style error handling (`Either`, `Option`). New code should follow this pattern rather than using exceptions for control flow.
- **Jetpack Compose** is the UI framework for settings/app screens, but the **keyboard views** (MainKeyboardView, NumberKeypadView, etc.) are still traditional Android Views. Do not attempt to migrate keyboard views to Compose without careful planning.
- **Lazy initialization** for `Cache` and `YamlLayoutLoader` in `VIM8Application` — these are expensive and should remain lazy.
- **CBOR caching** of parsed layouts is intentional for performance — cached layouts are invalidated when the version code changes (`postInitialize` in `AppPrefs`).

### Known Patterns & Preferences
- Preference keys follow the naming convention `prefs_<group>_<subgroup>_<name>` (e.g., `prefs_keyboard_trail_color`)
- Test files are named `*Spec.kt` (Kotest style)
- The `@optics` annotation (Arrow) is used on `KeyboardData` — **do not remove this** as it enables optics-based immutable updates throughout the codebase
- Build variants: `debug` has `.debug` suffix, `rc` has `.rc` suffix, `release` has no suffix

### Active Considerations
- Android 14 was a pain point (API 34 IME listing fix) — any new Android API work should be carefully version-gated using `AndroidVersion.ATLEAST_API28_P` pattern
- The `PreferenceModel` migration system is at version 3 — any new preference keys added should follow the migration pattern if old keys need to be renamed

## Important File Locations

| Purpose | File |
|---------|------|
| IME core service | `8vim/src/main/kotlin/inc/flide/vim8/MainInputMethodService.kt` |
| Application entry | `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt` |
| All preferences | `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt` |
| Gesture processing | `8vim/src/main/kotlin/inc/flide/vim8/ime/actionlisteners/MainKeypadActionListener.kt` |
| Data model (keyboard) | `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/KeyboardData.kt` |
| Layer definitions | `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/LayerLevel.kt` |
| Finger positions | `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/FingerPosition.kt` |
| Layout loading | `8vim/src/main/kotlin/inc/flide/vim8/ime/LayoutLoader.kt` |
| App navigation | `8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt` |
| Version | `8vim/version.properties` |
| Dependency versions | `gradle/libs.versions.toml` |

## Next Steps (suggested, not committed)
- No immediate tasks identified; project is in stable maintenance mode
- Potential areas for future work: dependency upgrades (Kotlin, Compose, Arrow), additional language layouts, accessibility improvements
