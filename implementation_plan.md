# Implementation Plan: Gradle & Dependency Upgrade

[Overview]
Update all build toolchain components and library dependencies in the 8VIM Android project to their latest stable versions.

This upgrade covers three tiers: (1) build toolchain (Gradle wrapper, Android Gradle Plugin, Kotlin, KSP), (2) AndroidX + Jetpack Compose stack, and (3) third-party libraries. The project currently uses Gradle 8.9 / AGP 8.7.2 / Kotlin 2.0.21, which are all one to several major releases behind.

A key decision in this plan is to **skip** the Arrow 1.x → 2.x upgrade, as Arrow 2.x introduces breaking API changes across the entire functional programming stack (including the dependent `arrow-integrations-jackson-module` and `kotest-arrow` extensions). That migration warrants its own dedicated plan. Similarly, Kotest 5.x is kept because Kotest 6.x is still at milestone (M4) status and not yet stable.

The upgrades are grouped into three sequential, independently-verifiable batches to follow the Baby Steps™ methodology and minimise breakage surface.

---

[Types]
No new types are introduced; this is a pure version-number and configuration change.

No Kotlin types, data classes, interfaces, or enums are added or removed. However, some AGP/Gradle DSL property names may change between versions (noted in the Functions section). The `compileSdk` and `targetSdk` values in `8vim/build.gradle.kts` are treated as configuration constants and are upgraded from 34 → 35 (Android 15), but this requires a code-level review of edge-to-edge handling.

---

[Files]
Two build files and one properties file require changes; no source files should need changes for the core upgrade.

### Files to Modify

| File | Change |
|------|--------|
| `gradle/wrapper/gradle-wrapper.properties` | Bump `distributionUrl` from Gradle 8.9 → 9.4.0 |
| `gradle/libs.versions.toml` | Update all version strings (see complete table below) |
| `8vim/build.gradle.kts` | Update `compileSdk`/`targetSdk` from 34 → 35; remove `@file:Suppress("DSL_SCOPE_VIOLATION")` (no longer needed in Gradle 8.1+); fix `enableUnitTestCoverage` DSL if needed |
| `build.gradle.kts` | Remove `@file:Suppress("DSL_SCOPE_VIOLATION")` |

### No Files Added or Deleted

---

[Functions]
No application logic functions change; one deprecated Gradle DSL call in `8vim/build.gradle.kts` may need correction.

### Potential DSL Change in `8vim/build.gradle.kts`

- **`enableUnitTestCoverage`** — Currently used as a bare property reference in the `debug` buildType block (`enableUnitTestCoverage`). In AGP 8.x this should be `enableUnitTestCoverage = true`. Verify and correct if AGP 8.13.x enforces this.
- **`@file:Suppress("DSL_SCOPE_VIOLATION")`** — This suppression was required for a Gradle 7.x TOML alias resolution bug. It is no longer needed in Gradle 8.1+ and must be removed to avoid warnings in Gradle 9.x.
- **`kotlinOptions { jvmTarget }`** — In Kotlin 2.x with AGP 8.x, the preferred way is via `compileOptions`/`kotlinOptions` or `compilerOptions { jvmTarget }`. Verify compiler warnings after upgrade and fix if needed.

---

[Classes]
No class changes are required for the version bump alone.

If `targetSdk` is raised to 35, the app must handle Android 15 edge-to-edge enforcement. For an IME (Input Method Service), this typically does not affect the service itself, but `MainActivity` and any Activity using `WindowCompat.setDecorFitsSystemWindows()` should be reviewed. No class additions are expected; this is a review/verification step only.

---

[Dependencies]
All version strings live in `gradle/libs.versions.toml`; the complete before/after table follows.

### Complete Version Upgrade Table

#### Build Toolchain

| Key in `[versions]` | Current | Target | Notes |
|---|---|---|---|
| `android-gradle-plugin` | `8.7.2` | `8.13.2` | Latest stable AGP 8.x |
| `kotlin` | `2.0.21` | `2.2.0` | Latest stable Kotlin |
| `ksp` | `2.0.21-1.0.26` | `2.2.0-2.0.2` | **Must match Kotlin version exactly** |
| `ktlint` | `12.1.1` | `12.1.1` | Keep — latest could not be confirmed |
| `mannodermaus-android-junit5` | `1.11.2.0` | `1.13.1.0` | Latest stable |

#### Gradle Wrapper (separate file)

| File | Current | Target |
|---|---|---|
| `gradle-wrapper.properties` distributionUrl | `gradle-8.9-bin.zip` | `gradle-9.4.0-bin.zip` |

#### AndroidX / Jetpack

| Key in `[versions]` | Current | Target | Notes |
|---|---|---|---|
| `androidx-activity` | `1.9.3` | `1.12.4` | |
| `android-appcompat` | `1.7.0` | `1.7.1` | |
| `android-material` | `1.12.0` | `1.13.0` | |
| `androidx-compose` | `1.7.5` | `1.10.4` | Compose BOM version |
| `androidx-compose-material3` | `1.3.1` | `1.4.0` | |
| `androidx-core` | `1.13.1` | `1.17.0` | |
| `androidx-core-splashscreen` | `1.0.1` | `1.2.0` | |
| `androidx-lifecycle` | `2.8.7` | `2.10.0` | |
| `androidx-navigation` | `2.8.3` | `2.9.7` | |
| `androidx-preference` | `1.2.1` | `1.2.1` | Keep — latest stable not confirmed to differ |

#### Third-Party Libraries

| Key in `[versions]` | Current | Target | Notes |
|---|---|---|---|
| `apache-commons` (commons-text) | `1.12.0` | `1.15.0` | |
| `commons-codec` | `1.17.1` | `1.17.1` | Keep — Maven search returned wrong artifact |
| `jackson` | `2.13.5` | `2.19.0` | All three jackson modules share this version |
| `json-schema-validator` | `1.0.73` | `1.5.6` | |
| `logback-classic` | `1.5.12` | `1.5.18` | Test-only |
| `logback-android` | `3.0.0` | `3.0.0` | Keep — already latest |
| `mockk` | `1.13.10` | `1.14.3` | |
| `mikepenz-aboutlibraries` | `11.2.3` | `11.2.3` | Keep — already latest |
| `slf4j` | `2.0.16` | `2.0.16` | Keep — 2.1.x is alpha |
| `colorpicker` | `1.1.2` | `1.1.2` | Keep — JitPack, version unverified |

#### Intentionally Skipped (Breaking / Pre-stable)

| Key | Current | Latest | Reason to Skip |
|---|---|---|---|
| `arrow` | `1.2.4` | `2.1.2` | Arrow 2.x has breaking API changes across the codebase |
| `arrow-jackson` | `0.14.0` | archived | Depends on Arrow; skip with Arrow |
| `kotest` | `5.9.1` | `6.0.0.M4` | Kotest 6.x is milestone — not stable |
| `kotest-arrow` | `1.4.0` | `1.4.0` | Depends on Arrow; skip with Arrow |
| `kotest-runner-android` | `1.1.1` | `1.1.2` | Minor patch; defer with kotest group |
| `kotest-assertions-android` | `1.1.1` | `1.1.1` | Already latest |

#### SDK Version Changes (in `8vim/build.gradle.kts`, not `libs.versions.toml`)

| Property | Current | Target | Notes |
|---|---|---|---|
| `compileSdk` | `34` | `35` | Android 15 stable |
| `targetSdk` | `34` | `35` | Edge-to-edge enforcement review required |

---

[Testing]
After each batch, run the full test suite and a debug build to confirm no regressions.

### Validation Commands (run after each batch)

```bash
# Clean build to flush caches
./gradlew clean

# Compile check only (fast)
./gradlew assembleDebug

# Full unit test suite
./gradlew testDebugUnitTest

# Lint
./gradlew lint

# Ktlint
./gradlew ktlintCheck
```

### Test Files Affected
- No test files need modification for a pure version bump.
- If `targetSdk` moves to 35 and edge-to-edge handling changes, any `MainActivityTest` or UI snapshot tests may need review.
- MockK 1.14.x and Kotest 5.9.x are kept, so test APIs remain stable.

---

[Implementation Order]
Upgrades are executed in 6 Baby Steps™, with Gradle 8.9 as the stable base for all library/toolchain work, and Gradle 9.4.0 applied last to isolate the biggest risk.

**Rationale:** AGP 8.13.2 requires Gradle > 8.9 (minimum ~8.11), so the Gradle wrapper upgrade and AGP upgrade must happen together. All other upgrades (Kotlin, AndroidX, third-party libs) are done first while still on the known-stable Gradle 8.9 + AGP 8.7.2 base. `targetSdk` 35 is applied as a final, isolated step.

---

### Step 1 — Baseline Validation (no changes)
Confirm the current build is clean before touching anything.
```bash
./gradlew clean assembleDebug testDebugUnitTest
```
Expected: green build. If not, stop and fix first.

---

### Step 2 — Kotlin + KSP + mannodermaus (still on Gradle 8.9 + AGP 8.7.2)
**File:** `gradle/libs.versions.toml`, update `[versions]`:
```toml
kotlin = "2.2.0"
ksp = "2.2.0-2.0.2"
mannodermaus-android-junit5 = "1.13.1.0"
```
**Validate:**
```bash
./gradlew assembleDebug testDebugUnitTest
```

---

### Step 3 — AndroidX + Compose Stack (still on Gradle 8.9 + AGP 8.7.2)
**File:** `gradle/libs.versions.toml`, update `[versions]`:
```toml
androidx-activity = "1.12.4"
android-appcompat = "1.7.1"
android-material = "1.13.0"
androidx-compose = "1.10.4"
androidx-compose-material3 = "1.4.0"
androidx-core = "1.17.0"
androidx-core-splashscreen = "1.2.0"
androidx-lifecycle = "2.10.0"
androidx-navigation = "2.9.7"
```
**Validate:**
```bash
./gradlew assembleDebug testDebugUnitTest
```

---

### Step 4 — Third-Party Library Versions (still on Gradle 8.9 + AGP 8.7.2)
**File:** `gradle/libs.versions.toml`, update `[versions]`:
```toml
apache-commons = "1.15.0"
jackson = "2.19.0"
json-schema-validator = "1.5.6"
logback-classic = "1.5.18"
mockk = "1.14.3"
```
**Validate:**
```bash
./gradlew assembleDebug testDebugUnitTest lint
```

---

### Step 5 — Gradle 8.9 → 9.4.0 + AGP 8.7.2 → 8.13.2 (the big toolchain jump)
These two must be done together because AGP 8.13.2 requires Gradle > 8.9.

**File:** `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
```
**File:** `gradle/libs.versions.toml`:
```toml
android-gradle-plugin = "8.13.2"
```
**File:** `build.gradle.kts` — Remove the line `@file:Suppress("DSL_SCOPE_VIOLATION")`  
**File:** `8vim/build.gradle.kts` — Remove the line `@file:Suppress("DSL_SCOPE_VIOLATION")`; check if `enableUnitTestCoverage` (bare reference) needs to be `enableUnitTestCoverage = true`.

**Validate:**
```bash
./gradlew --version   # should show 9.4.0
./gradlew assembleDebug testDebugUnitTest
```

---

### Step 6 — compileSdk + targetSdk 34 → 35 (Android 15)
**File:** `8vim/build.gradle.kts`:
```kotlin
compileSdk = 35
// ...
targetSdk = 35
```
Review `MainActivity` for edge-to-edge implications (Android 15 enforces edge-to-edge for apps targeting API 35). For an IME service, this is lower risk but should be checked.

**Validate:**
```bash
./gradlew clean assembleDebug testDebugUnitTest lint ktlintCheck
```
Confirm:
- [ ] Debug APK builds successfully
- [ ] All unit tests pass
- [ ] No new lint errors
- [ ] KtLint passes

---

### Future Work (Out of Scope for This Plan)
- **Arrow 1.x → 2.x migration**: Requires systematic API replacement across all functional code (a dedicated plan)
- **Kotest 5.x → 6.x migration**: Await stable release
- **Gradle 9.x configuration cache enforcement**: Review and fix if configuration cache issues surface during this upgrade
- **Android 15 edge-to-edge**: If `targetSdk = 35` causes visual regressions in `MainActivity`, add `WindowCompat.setDecorFitsSystemWindows(window, false)` guard or adopt the edge-to-edge APIs
