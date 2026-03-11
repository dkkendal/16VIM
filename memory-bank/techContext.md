# Tech Context: 8VIM

## Platform & Language

| Aspect | Detail |
|--------|--------|
| **Platform** | Android (minSdk 24 / Android 7.0 Nougat, targetSdk 35 / Android 15) |
| **Primary Language** | Kotlin 2.2.0 |
| **JVM Target** | Java 17 |
| **Build System** | Gradle 9.4.0 (Kotlin DSL, `build.gradle.kts`) |
| **Package Name** | `inc.flide.vim8` (app ID: `inc.flide.vi8`) |
| **Version** | 0.17.0 (versionCode computed from MAJOR/MINOR/PATCH/RC in `version.properties`) |
| **Build Variants** | `debug`, `release`, `rc` |

## Toolchain Versions

| Tool | Version |
|------|---------|
| Gradle | 9.4.0 |
| Android Gradle Plugin (AGP) | 8.13.2 |
| Kotlin | 2.2.0 |
| KSP | 2.2.0-2.0.2 |
| compileSdk | 36 |
| targetSdk | 35 |
| minSdk | 24 |

## Core Android Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.appcompat` | 1.7.1 | Activity/Fragment base classes |
| `androidx.core:core-ktx` | 1.17.0 | Kotlin extensions for Android |
| `androidx.core:core-splashscreen` | 1.2.0 | Splash screen API |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.10.0 | Lifecycle-aware coroutine scopes |
| `androidx.activity:activity-compose` | 1.12.4 | Compose integration with Activity |
| `androidx.navigation:navigation-compose` | 2.9.7 | Compose navigation |
| `androidx.preference` | 1.2.1 | Preference storage |

## UI Framework

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.compose.ui` | 1.10.4 | Core Compose UI |
| `androidx.compose.material3` | 1.4.0 | Material 3 components |
| `androidx.compose.material:material-icons-core` | 1.7.8 | Material icons |
| `com.google.android.material` | 1.13.0 | Material design components |
| `colorpicker-compose` (skydoves) | 1.1.2 | Color picker for Compose |
| `mikepenz:aboutlibraries-compose-m3` | 11.2.3 | Open-source license screen |

## Functional Programming

| Library | Version | Purpose |
|---------|---------|---------|
| `arrow-core` | 1.2.4 | `Option`, `Either`, functional utilities |
| `arrow-optics` | 1.2.4 | Type-safe immutable data transformations |
| `arrow-optics-ksp-plugin` | 1.2.4 | KSP code generation for optics |
| `arrow-integrations-jackson-module` | 0.14.0 | Jackson integration with Arrow types |
| `kotlin-reflect` | 2.2.0 | Reflection for preference model |

## Data / Serialization

| Library | Version | Purpose |
|---------|---------|---------|
| `jackson-dataformat-yaml` | 2.18.6 | Parse YAML layout files |
| `jackson-dataformat-cbor` | 2.18.6 | Binary cache format for layouts |
| `jackson-module-kotlin` | 2.18.6 | Kotlin data class serialization |
| `json-schema-validator` | 1.0.73 | Validate YAML layout files against schema |
| `apache-commons-text` | 1.15.0 | Text utilities |
| `commons-codec` | 1.17.1 | Encoding utilities |

> ⚠️ **Jackson version cap**: Jackson 2.19+ targets Java 11 APIs requiring `minSdk ≥ 26`. 
> Upgrade to 2.21+ only after raising `minSdk` from 24 → 26.

## Logging

| Library | Version | Purpose |
|---------|---------|---------|
| `slf4j-api` | 2.0.16 | Logging facade |
| `logback-android` | 3.0.0 | Android logging backend |
| `logback-classic` | 1.5.32 | Classic logback (test only) |

## Testing Stack

| Library | Version | Purpose |
|---------|---------|---------|
| **JUnit 5** (`junit-jupiter`) | via mannodermaus | Primary unit test runner |
| **JUnit 4** | 4.x | Android instrumented tests |
| **Kotest** | 5.9.1 | Behaviour specs, property-based testing |
| `kotest-assertions-arrow` | 1.4.0 | Arrow-specific Kotest assertions |
| `kotest-property` + arrow extensions | 5.9.1 / 1.4.0 | Property-based tests with Arrow |
| **MockK** | 1.14.9 | Kotlin-native mocking |
| `mannodermaus-android-junit5` | 1.13.1.0 | JUnit 5 on Android |
| `jacoco` | (Gradle built-in) | Code coverage reporting |
| `androidx.test.*` | 1.6.1 / 1.6.2 | Android instrumented test infra |

## Code Quality & Tooling

| Tool | Config | Purpose |
|------|--------|---------|
| **KtLint** | plugin 12.1.1 | Kotlin linting & formatting |
| **Checkstyle** | `config/checkstyle/checkstyle.xml` | Java source checks |
| **Android Lint** | `android { lint {} }` in `8vim/build.gradle.kts` | Android-specific lint |
| **KSP** | 2.2.0-2.0.2 | Kotlin Symbol Processing (Arrow optics) |
| **Fastlane** | `fastlane/` dir | CI/CD automation |
| **Codecov** | `codecov.yml` | Coverage reporting |

### Lint Disabled Checks (as of 2026-03 upgrade)
```kotlin
disable += listOf(
    "ObsoleteLintCustomCheck",   // pre-existing
    "ClickableViewAccessibility", // pre-existing
    "VectorPath",                 // pre-existing
    "UnusedResources",            // pre-existing
    "GradleDependency",           // pre-existing
    "OldTargetApi",               // pre-existing
    // AGP 8.13.2 + Kotlin 2.2.0 K2 UAST crashes (internal lint engine bugs):
    "UElementAsPsi",
    "Recycle",
    // Pre-existing code violations newly detected by AGP 8.13.2 (fix in separate PRs):
    "AndroidGradlePluginVersion",
    "IntentFilterUniqueDataAttributes",
    "ConfigurationScreenWidthHeight",
    "LocalContextResourcesRead",
    "UseKtx"
)
```

## Development Setup

### Prerequisites
- Android Studio (latest stable — needs Gradle sync after clone)
- JDK 17
- Android SDK (API 36 compileSdk + API 24 minimum)

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore env vars)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run with coverage
./gradlew jacocoTestReport

# Lint check
./gradlew lintDebug

# Ktlint check
./gradlew ktlintCheck

# Ktlint format (auto-fix)
./gradlew ktlintFormat

# Checkstyle
./gradlew checkstyle

# Full pipeline (same as CI)
./gradlew clean assembleRelease testDebugUnitTest lintDebug ktlintCheck
```

### Known IDE Issue
Android Studio may show `Unresolved reference 'material'` at `libs.androidx.compose.material.icons.core` in `8vim/build.gradle.kts`. This is an IDE version catalog accessor resolution bug — run `File > Sync Project with Gradle Files` to resolve. Actual Gradle builds are unaffected.

### Signing (Release)
Controlled via environment variables:
- `VIM8_BUILD_KEYSTORE_FILE`
- `VIM8_BUILD_KEYSTORE_PASSWORD`
- `VIM8_BUILD_KEY_ALIAS`
- `VIM8_BUILD_KEY_PASSWORD`

## File Layout

```
8VIM/
├── 8vim/                          # Main app module
│   ├── src/main/kotlin/           # Kotlin sources
│   │   └── inc/flide/vim8/
│   │       ├── app/               # MainActivity, Routes, Setup, Settings
│   │       ├── datastore/         # Preference model & Datastore
│   │       ├── geometry/          # Circle, Dimension utilities
│   │       ├── ime/               # Core IME: theme, loader, layout engine, action listeners
│   │       ├── lib/               # Utilities: Android extensions, Compose helpers
│   │       ├── theme/             # Theme definitions
│   │       ├── utils/             # Dialogs, InputMethod utilities
│   │       ├── AppPrefs.kt        # All application preferences
│   │       ├── MainInputMethodService.kt  # Core IME service
│   │       └── VIM8Application.kt # Application class
│   ├── src/main/res/              # Android resources (layouts, drawables, strings)
│   ├── src/main/assets/           # Fonts, logback config
│   ├── src/main/resources/        # schema.json (layout validation)
│   ├── src/test/kotlin/           # Unit tests
│   └── src/test/resources/        # Test YAML fixtures
├── gradle/
│   ├── libs.versions.toml         # Version catalog (single source of truth for versions)
│   └── wrapper/gradle-wrapper.properties  # Gradle 9.4.0
├── memory-bank/                   # Cline Memory Bank (this directory)
├── 8VIM.wiki/                     # Project wiki (submodule)
├── fastlane/                      # Fastlane config
├── metadata/en-US/                # F-Droid / Play Store metadata & changelogs
└── config/checkstyle/             # Checkstyle config
```

## Kotlin Compiler Options
```kotlin
freeCompilerArgs = listOf(
    "-opt-in=kotlin.contracts.ExperimentalContracts",
    "-Xjvm-default=all-compatibility"
)
```
