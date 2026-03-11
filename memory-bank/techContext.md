# Tech Context: 8VIM

## Platform & Language

| Aspect | Detail |
|--------|--------|
| **Platform** | Android (minSdk 24 / Android 7.0 Nougat, targetSdk 34 / Android 14) |
| **Primary Language** | Kotlin 1.8.21 |
| **JVM Target** | Java 17 |
| **Build System** | Gradle 8 (Kotlin DSL, `build.gradle.kts`) |
| **Package Name** | `inc.flide.vim8` (app ID: `inc.flide.vi8`) |
| **Version** | 0.17.0 (versionCode computed from MAJOR/MINOR/PATCH/RC in `version.properties`) |
| **Build Variants** | `debug`, `release`, `rc` |

## Core Android Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.appcompat` | 1.6.1 | Activity/Fragment base classes |
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions for Android |
| `androidx.core:core-splashscreen` | 1.0.1 | Splash screen API |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.2 | Lifecycle-aware coroutine scopes |
| `androidx.activity:activity-compose` | 1.7.2 | Compose integration with Activity |
| `androidx.navigation:navigation-compose` | 2.7.3 | Compose navigation |
| `androidx.preference` | 1.2.1 | Preference storage |
| `androidx.recyclerview` | 1.3.1 | RecyclerView |
| `androidx.constraintlayout` | 2.1.4 | ConstraintLayout |

## UI Framework

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.compose.ui` | 1.5.2 | Core Compose UI |
| `androidx.compose.material3` | 1.1.2 | Material 3 components |
| `accompanist-systemuicontroller` | 0.30.1 | System bar colour control |
| `com.google.android.material` | 1.9.0 | Material design components |
| `keyboardview` (hijamoya) | 0.0.2 | Custom keyboard view base |
| `colorpreference` (kizitonwose) | 1.1.0 | Color picker preference |
| `mikepenz:aboutlibraries` | 10.2.0 | Open-source license screen |

## Functional Programming

| Library | Version | Purpose |
|---------|---------|---------|
| `arrow-core` | 1.2.0 | `Option`, `Either`, functional utilities |
| `arrow-optics` | 1.2.0 | Type-safe immutable data transformations |
| `arrow-optics-ksp-plugin` | 1.2.0 | KSP code generation for optics |
| `arrow-integrations-jackson-module` | 0.14.0 | Jackson integration with Arrow types |
| `kotlin-reflect` | 1.8.21 | Reflection for preference model |

## Data / Serialization

| Library | Version | Purpose |
|---------|---------|---------|
| `jackson-dataformat-yaml` | 2.13.5 | Parse YAML layout files |
| `jackson-dataformat-cbor` | 2.13.5 | Binary cache format for layouts |
| `jackson-module-kotlin` | 2.13.5 | Kotlin data class serialization |
| `json-schema-validator` | 1.0.73 | Validate YAML layout files against `schema.json` |
| `apache-commons-text` | 1.10.0 | Text utilities |
| `commons-codec` | 1.16.0 | Encoding utilities |

## Logging

| Library | Version | Purpose |
|---------|---------|---------|
| `slf4j-api` | 2.0.7 | Logging facade |
| `logback-android` | 3.0.0 | Android logging backend |
| `logback-classic` | 1.3.0 | Classic logback (test only) |

## Testing Stack

| Library | Version | Purpose |
|---------|---------|---------|
| **JUnit 5** (`junit-jupiter`) | 5.9.3 | Primary unit test runner |
| **JUnit 4** | 4.13.2 | Legacy / Android instrumented tests |
| **Kotest** | 5.6.2 | Behaviour specs, property-based testing |
| `kotest-assertions-arrow` | 1.3.3 | Arrow-specific Kotest assertions |
| `kotest-property` + arrow extensions | 5.6.2 / 1.3.3 | Property-based tests with Arrow |
| **MockK** | 1.13.5 | Kotlin-native mocking |
| `mannodermaus-android-junit5` | 1.9.3.0 | JUnit 5 on Android |
| `jacoco` | (Gradle built-in) | Code coverage reporting |
| `androidx.test.*` | various | Android instrumented test infra |
| `espresso-core` | 3.5.1 | Android UI tests |

## Code Quality & Tooling

| Tool | Config | Purpose |
|------|--------|---------|
| **KtLint** | `ktlint` plugin 11.5.0 | Kotlin linting & formatting |
| **Checkstyle** | `config/checkstyle/checkstyle.xml` | Java source checks |
| **Android Lint** | `android { lint {} }` in `build.gradle.kts` | Android-specific lint |
| **KSP** | 1.8.21-1.0.11 | Kotlin Symbol Processing (Arrow optics) |
| **Fastlane** | `fastlane/` dir | CI/CD automation |
| **Codecov** | `codecov.yml` | Coverage reporting |

## Development Setup

### Prerequisites
- Android Studio (latest stable)
- JDK 17
- Android SDK (API 34 + API 24 minimum)

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore env vars)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Lint check
./gradlew lint

# Ktlint check
./gradlew ktlintCheck

# Ktlint format
./gradlew ktlintFormat

# Checkstyle
./gradlew checkstyle
```

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
│   ├── src/main/java/             # Legacy Java source (some views)
│   ├── src/main/res/              # Android resources (layouts, drawables, strings)
│   ├── src/main/assets/           # Fonts, logback config
│   ├── src/main/resources/        # schema.json (layout validation)
│   ├── src/test/kotlin/           # Unit tests
│   └── src/test/resources/        # Test YAML fixtures
├── gradle/libs.versions.toml      # Version catalog
├── memory-bank/                   # Cline Memory Bank (this directory)
├── 8VIM.wiki/                     # Project wiki (submodule)
├── cicd_scripts/                  # CI/CD shell/Ruby scripts
├── fastlane/                      # Fastlane config
├── metadata/en-US/                # F-Droid / Play Store metadata & changelogs
└── config/checkstyle/             # Checkstyle config
```

## Kotlin Compiler Options
```kotlin
freeCompilerArgs = listOf(
    "-Xallow-result-return-type",
    "-opt-in=kotlin.contracts.ExperimentalContracts",
    "-Xjvm-default=all-compatibility"
)
```
