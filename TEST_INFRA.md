# E2E Test Infra: Apple TV (tvOS) Style Android TV Launcher

## Test Philosophy
- **Opaque-Box & Requirement-Driven**: Tests interact exclusively with the launcher via Android TV remote control KeyEvents (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER`, `ENTER`, `BACK`, `MENU`) and verify user-observable behavior (focused card, visual borders, launched intents, SharedPreferences). Zero dependency on internal implementation details.
- **Headless JVM Execution**: Uses Robolectric 4.12+ with TV qualifiers (`w1920dp-h1080dp-television-long-notround-land-xhdpi`) simulating a 1080p Android TV display. No physical TV hardware or emulators required.
- **Systematic 4-Tier Matrix**: Category-Partition + Boundary Value Analysis + Pairwise Combinatorial Interactions + Real-World Remote Sessions.

---

## Feature Inventory & Test Matrix

| # | Feature | Source (Requirement) | Tier 1 (Isolated) | Tier 2 (Boundary) | Tier 3 (Pairwise) | Tier 4 (Workload) |
|---|---------|----------------------|:-----------------:|:-----------------:|:-----------------:|:-----------------:|
| F1 | App Discovery (TV + Sideloaded) | ORIGINAL_REQUEST R4 & AC | 5 | 5 | ✓ | ✓ |
| F2 | TV App Launching & Intent Handling | ORIGINAL_REQUEST R1 & AC | 5 | 5 | ✓ | ✓ |
| F3 | Sideloaded Fallback Banner Generation | ORIGINAL_REQUEST R4 | 5 | 5 | ✓ | ✓ |
| F4 | Favorites Management & Persistence | ORIGINAL_REQUEST R1, R4 | 5 | 5 | ✓ | ✓ |
| F5 | Hide Apps & Unhide Settings | ORIGINAL_REQUEST R4 | 5 | 5 | ✓ | ✓ |
| F6 | Dynamic Grid Columns (6, 4, 3) | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ | ✓ |
| F7 | OLED High-Contrast Mode (#000000 / #FFD600) | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ | ✓ |
| F8 | Icon Shape Customization (Squircle / Circle / Rect) | ORIGINAL_REQUEST R4 | 5 | 5 | ✓ | ✓ |
| F9 | D-Pad Navigation & Focus Retention | ORIGINAL_REQUEST R1 & AC | 5 | 5 | ✓ | ✓ |
| F10| Performance (< 30 MB RAM, < 300 ms Cold Start) | ORIGINAL_REQUEST R2 & AC | 2 | 2 | ✓ | ✓ |
| F11| Bing Daily Wallpaper & Dark Scrim | ORIGINAL_REQUEST R5 | 5 | 5 | ✓ | ✓ |
| F12| Modular Status Widgets (RAM, Net, Weather, News) | ORIGINAL_REQUEST R5 | 5 | 5 | ✓ | ✓ |
| **Total** | | | **57** | **57** | **6 Suites** | **4 Sessions** |

---

## Test Architecture

### Test Runner & Invocation
- **Runner**: Robolectric with `BaseTvRobolectricTest` establishing 1920x1080 TV layout bounds.
- **Command**:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- **Pass/Fail Semantics**: All test suites must execute with exit code `0` and 0 failures.
- **Report Location**: `app/build/reports/tests/testDebugUnitTest/index.html`.

### Test Architecture Directory Layout
```
app/src/test/java/com/minimal/tvlauncher/
├── harness/
│   ├── TvTestDriver.kt              # Remote control DSL & layout simulator
│   ├── FakeAppPackageGenerator.kt   # Creates Leanback & Mobile test apps
│   └── BaseTvRobolectricTest.kt    # Base class with 1080p TV qualifiers
├── tier1/
│   ├── AppLoadingFeatureTest.kt
│   ├── TvAppLaunchFeatureTest.kt
│   ├── SideloadedFallbackFeatureTest.kt
│   ├── FavoritesFeatureTest.kt
│   ├── HideAppFeatureTest.kt
│   ├── ColumnSwitchingFeatureTest.kt
│   ├── HighContrastFeatureTest.kt
│   └── IconShapeFeatureTest.kt
├── tier2/
│   ├── EmptyStateBoundaryTest.kt
│   ├── StressAppLoadBoundaryTest.kt
│   ├── PackageUninstallBoundaryTest.kt
│   ├── RapidInputFloodBoundaryTest.kt
│   ├── SpatialFocusBoundaryTest.kt
│   └── SettingsCorruptionBoundaryTest.kt
├── tier3/
│   └── CrossFeatureCombinationTest.kt
├── tier4/
│   └── RealWorldUserJourneyTest.kt
└── perf/
    ├── MemoryAllocationBenchmarkTest.kt
    └── ColdStartLatencyBenchmarkTest.kt
```

---

## Real-World Application Scenarios (Tier 4)

| # | Scenario | Features Exercised | Complexity |
|---|----------|--------------------|------------|
| 1 | Boot, Discover, Favorite, and App Launch Journey | F1, F2, F4, F9 | Medium |
| 2 | Accessibility & Layout Customization Session (OLED High Contrast + 4-Column + Circle) | F6, F7, F8, F9 | High |
| 3 | App Decluttering Session (Long-press hide, unhide via Settings) | F4, F5, F9 | Medium |
| 4 | Low Memory Reclaim & State Restoration Session | F1, F4, F6, F7, F10| High |

---

## Coverage Thresholds
- **Tier 1 (Feature Coverage)**: >=5 test cases per feature covering equivalence class representatives. Total >= 45 tests.
- **Tier 2 (Boundary & Corner Cases)**: >=5 test cases per boundary condition (0 apps, 150+ apps, uninstalls during focus, rapid D-pad flood, grid edges, corrupted storage). Total >= 30 tests.
- **Tier 3 (Cross-Feature Combinations)**: Pairwise and multi-way feature interactions (Theme + Columns + Shape, Hiding Favorites, etc.). Total >= 5 multi-step suites.
- **Tier 4 (Real-World Workloads)**: >=4 complete end-to-end user remote control sessions.
- **Performance Benchmarks**: Memory heap expansion < 5 MB over 100 navigation cycles; bitmap cache capped at <= 8 MB; cold start < 300 ms.
- **Total Minimum**: >= 100 test cases with 100% pass rate.
