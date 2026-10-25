# Project: Apple TV (tvOS) Style Android TV Launcher (Zen TV)

## Architecture
Ultra-lightweight, high-performance Android TV launcher engineered for Google TV 2021 4K hardware (Amlogic, 2 GB RAM limit, Mali-G31 GPU). Built strictly using native Android Views (`RecyclerView`, `CardView`, `TextClock`) with zero heavy runtimes (no Compose, no Leanback library, no Dagger/Hilt, no Room).

### Component Boundaries
- **Data & Model Layer**: `com.minimal.tvlauncher.model.AppModel` (lean representation of TV and sideloaded apps).
- **Persistence Layer**: `com.minimal.tvlauncher.manager.PreferencesManager` (zero-bloat `SharedPreferences` for favorites, hidden apps, grid size, high-contrast mode, icon shapes, and sound feedback).
- **Discovery Layer**: `com.minimal.tvlauncher.manager.AppDiscoveryManager` (PackageManager dual-query for `CATEGORY_LEANBACK_LAUNCHER` and `CATEGORY_LAUNCHER`, deduplication, procedural 16:9 fallback banner generator for sideloaded apps with <8 MB LRU cache, and `PackageChangeReceiver`).
- **UI & Animation Layer**:
  - `com.minimal.tvlauncher.adapter.AppGridAdapter` & `FavoritesAdapter` (dynamic column rendering, view recycling, squircle/circle/rounded corner clipping).
  - `com.minimal.tvlauncher.ui.FocusAnimationEngine` (1.08x hardware-accelerated scale, glowing borders, elevation depth, audio click feedback).
  - `com.minimal.tvlauncher.ui.MainActivity` (leanback launcher lifecycle: `singleTask`, `onNewIntent` reset, BACK key trap/scroll-to-top, seamless D-pad traversal between header, favorites, and grid).
  - `com.minimal.tvlauncher.ui.SettingsDialog` & `ContextMenuDialog` (long-press OK actions, OLED high-contrast toggle, column selection, icon shape picker).

---

## Feature Inventory
Every feature identified during the Survey phase is mapped to an implementation milestone below:

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Leanback Home Registration | Intent filters `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT`, `CATEGORY_LEANBACK_LAUNCHER` | M1 | Survey |
| 2 | Gradle & Toolchain Foundation | Gradle wrapper scripts, JDK 17, missing mipmap icons, test dependencies | M1 | Survey |
| 3 | Core Data Model (`AppModel`) | Clean immutable representation of apps (package, activity, title, banner, icon, flags) | M1 | Survey |
| 4 | Persistence Engine (`PreferencesManager`) | SharedPreferences storage for favorites, hidden apps, column count, contrast, shape, audio | M1 | Survey |
| 5 | Leanback App Query | Query `CATEGORY_LEANBACK_LAUNCHER` activities via `PackageManager` | M2 | Survey |
| 6 | Sideloaded App Query | Query `CATEGORY_LAUNCHER` activities for non-TV apps | M2 | Survey |
| 7 | Package Deduplication & Merge | Merge TV & mobile apps, prioritize leanback activity, sort alphabetically, exclude self | M2 | Survey |
| 8 | TV Banner Extraction | Extract 16:9 widescreen banners from leanback activities | M2 | Survey |
| 9 | Fallback Banner Generator | Procedural 16:9 banner generation for sideloaded apps (hash gradient + centered icon + title) | M2 | Survey |
| 10 | Bounded Bitmap LRU Cache | In-memory cache capped at <= 8 MB for icons and procedural banners | M2 | Survey |
| 11 | Dynamic Package Receiver | BroadcastReceiver monitoring app install, update, and uninstall in real-time | M2 | Survey |
| 12 | Main App Grid Adapter | `RecyclerView` adapter with dynamic columns (6, 4, 3) and efficient view recycling | M3 | Survey |
| 13 | Favorites Row Adapter | Dedicated pinned favorites row with persistent ordering and star badges | M3 | Survey |
| 14 | 1.08x Focus Scale Animation | Smooth 1.08x hardware-accelerated scale with elevation depth (2dp -> 10dp) in 180ms | M3 | Survey |
| 15 | Glowing Border Highlight | Luminous white border (normal) and vivid yellow `#FFD600` (high-contrast) | M3 | Survey |
| 16 | Dynamic Icon Shapes | Shape switching: Squircle (Apple TV 24dp), Circle (height/2), Soft Rounded Rect (8dp) | M3 | Survey |
| 17 | D-pad Audio Feedback | Auditory click feedback on navigation and selection via `AudioManager` / `SoundPool` | M3 | Survey |
| 18 | Strict Focus Retention | Edge boundary clamping, no focus drops, seamless navigation between sections | M3 | Survey |
| 19 | TV Status Header Bar | Live `TextClock` (time & date) + D-pad focusable settings button | M4 | Survey |
| 20 | Root Activity Exit Trap | Intercept BACK key on home screen (scrolls to top / dismisses modals; never exits root) | M4 | Survey |
| 21 | SingleTask HOME Reset | Intercept HOME button via `onNewIntent` to dismiss dialogs and reset scroll to position 0 | M4 | Survey |
| 22 | Configuration Change Immunity | `configChanges` handling to absorb HDMI CEC and resolution flips without activity restart | M4 | Survey |
| 23 | Long-Press Context Menu | Long-press D-pad Center / OK to toggle favorites, hide apps, or open app details | M4 | Survey |
| 24 | App Details Navigation | Launch system app settings (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) | M4 | Survey |
| 25 | OLED High-Contrast Mode | Pure black `#000000` background + `#FFD600` yellow borders + pure white text | M4 | Survey |
| 26 | Dynamic Grid Column Switching | Standard (6 cols), Large (4 cols), Extra-Large (3 cols) dynamic reconfiguration | M4 | Survey |
| 27 | Hidden Apps Management | Ability to hide apps from main grid and unhide them via settings | M4 | Survey |
| 28 | Ultra-Low RAM Budget (< 30 MB) | Memory profiling & optimization ensuring <30 MB peak RAM during rapid browsing | M5 | Survey |
| 29 | Cold Start Optimization (< 300 ms) | Cold start performance benchmark verifying <300 ms initial frame render | M5 | Survey |
| 30 | Opaque-Box E2E Test Suite (Tiers 1-4) | Pass 100% of headless Robolectric TV test cases | M5 | Survey |
| 31 | Adversarial Coverage Hardening (Tier 5) | White-box adversarial testing and code gap elimination | M5 | Survey |
| 32 | Bing Daily Wallpaper | Fetch Bing 'Image of the Day' with dark contrast scrim overlay, switchable with gradient | M4 | R5 Update |
| 33 | Status Widget: Live RAM | Free/Used RAM indicator in status header with ON/OFF toggle in settings | M4 | R5 Update |
| 34 | Status Widget: Network Status | Wi-Fi / Ethernet connectivity badge in header with ON/OFF toggle in settings | M4 | R5 Update |
| 35 | Status Widget: Weather | Weather widget in status header with ON/OFF toggle in settings | M4 | R5 Update |
| 36 | Status Widget: News Ticker | Google News RSS headlines ticker in top bar with ON/OFF toggle in settings | M4 | R5 Update |
| 37 | Apple TV Frosted Dock Shelf | Frosted glass dock at bottom with squircle app icons matching concept mockup | M4 | Mockup |
| 38 | Top-Right Status Bar Capsules | Translucent glass capsules for RAM Free, Wi-Fi 6, Weather 21°C, Settings gear | M4 | Mockup |
| 39 | Center News Ticker Pill | Centered floating pill with marquee Google News RSS headlines | M4 | Mockup |
| 40 | Alpine Lake Wallpaper Engine | 4K scenic alpine lake default wallpaper with Bing Daily HTTPS refresh & scrim | M4 | Mockup |
| 41 | Unit Test Suite 100% Pass Rate | All unit tests (core data, widgets, UI discovery) passing without error | M5 | Testing |
| 42 | Top Shelf TvContract Engine | Dynamic Watch Next / Featured series banner above dock with ON/OFF toggle in settings | M4 | TvContract |
| 43 | Multi-Platform Abstraction | TV Platform abstraction layer supporting Google TV, Android TV and Amazon Fire TV | M4 | Architecture |
| 44 | Live Sports Widget | Real-time match scores and live results ticker with league selector | M4 | Sports |
| 45 | Display & Refresh Rate Engine | Dynamic handling of 4K, 1080p, 720p TV resolutions, 50/60/120Hz display modes and HDMI overscan safe zones | M4 | Hardware |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Foundation & Core Data Layer | Gradle wrapper, toolchain setup, manifest icon fixes, test dependencies in `app/build.gradle.kts`, `AppModel`, and `PreferencesManager` with unit tests | none | IN_PROGRESS |
| M2 | App Discovery & Sideloaded Fallback Engine | `AppDiscoveryManager` (TV + mobile query, deduplication, 16:9 procedural banner generator, LRU bitmap cache, `PackageChangeReceiver`) with unit tests | M1 | PLANNED |
| M3 | Apple TV UI, Adapters & Focus Engine | `AppGridAdapter`, `FavoritesAdapter`, `FocusAnimationEngine` (1.08x scale, glow, OLED yellow, Squircle/Circle/Rect shapes, D-pad audio feedback, boundary clamping) with unit tests | M1, M2 | PLANNED |
| M4 | Activity Orchestration, Header, Widgets & Settings | `MainActivity` lifecycle (`singleTask`, BACK trap, `onNewIntent`), Status Header (`TextClock` + modular widgets: RAM, network, weather, news ticker), Bing Daily Wallpaper engine with dark contrast scrim, Context Menu, Settings Dialog (OLED contrast, 6/4/3 columns, icon shape, wallpaper & widget toggles, hidden apps) with unit tests | M1, M2, M3 | PLANNED |
| M5 | Final E2E Test Verification & Hardening | Phase 1: Pass 100% of E2E tests (Tiers 1-4) from `TEST_READY.md`. Phase 2: Tier 5 adversarial coverage hardening | M1, M2, M3, M4, TEST_READY | PLANNED |

---

## Interface Contracts

### Data Model (`com.minimal.tvlauncher.model.AppModel`)
```kotlin
package com.minimal.tvlauncher.model

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val banner: Drawable? = null,
    val icon: Drawable? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isSystemApp: Boolean = false
)
```

### Preferences Contract (`com.minimal.tvlauncher.manager.PreferencesManager`)
```kotlin
package com.minimal.tvlauncher.manager

interface IPreferencesManager {
    fun getFavorites(): List<String>
    fun setFavorites(packages: List<String>)
    fun addFavorite(packageName: String)
    fun removeFavorite(packageName: String)
    fun isFavorite(packageName: String): Boolean

    fun getHiddenApps(): Set<String>
    fun setAppHidden(packageName: String, hidden: Boolean)
    fun isAppHidden(packageName: String): Boolean

    fun isHighContrast(): Boolean
    fun setHighContrast(enabled: Boolean)

    fun getGridColumns(): Int // 6, 4, or 3
    fun setGridColumns(columns: Int)

    fun getIconShape(): String // "squircle", "circle", "rounded_rect"
    fun setIconShape(shape: String)

    fun isAudioFeedbackEnabled(): Boolean
    fun setAudioFeedbackEnabled(enabled: Boolean)

    // Requirement R5: Wallpaper & Modular Status Widgets
    fun isBingWallpaperEnabled(): Boolean
    fun setBingWallpaperEnabled(enabled: Boolean)

    fun isRamWidgetEnabled(): Boolean
    fun setRamWidgetEnabled(enabled: Boolean)

    fun isNetworkWidgetEnabled(): Boolean
    fun setNetworkWidgetEnabled(enabled: Boolean)

    fun isWeatherWidgetEnabled(): Boolean
    fun setWeatherWidgetEnabled(enabled: Boolean)

    fun isNewsWidgetEnabled(): Boolean
    fun setNewsWidgetEnabled(enabled: Boolean)
}
```

### App Discovery Contract (`com.minimal.tvlauncher.manager.AppDiscoveryManager`)
```kotlin
package com.minimal.tvlauncher.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.minimal.tvlauncher.model.AppModel

interface IAppDiscoveryManager {
    fun queryInstalledApps(callback: (List<AppModel>) -> Unit)
    fun registerPackageReceiver(context: Context, onPackageChanged: () -> Unit)
    fun unregisterPackageReceiver(context: Context)
    fun generateFallbackBanner(context: Context, packageName: String, label: String, icon: Drawable?): Bitmap
    fun launchApp(context: Context, app: AppModel): Boolean
}
```

---

## Code Layout

```
c:\Users\jdgen\OneDrive\Documents\android tv apple tv\
├── app\
│   ├── build.gradle.kts
│   └── src\
│       ├── main\
│       │   ├── AndroidManifest.xml
│       │   ├── java\com\minimal\tvlauncher\
│       │   │   ├── adapter\
│       │   │   │   ├── AppGridAdapter.kt
│       │   │   │   └── FavoritesAdapter.kt
│       │   │   ├── manager\
│       │   │   │   ├── AppDiscoveryManager.kt
│       │   │   │   ├── IAppDiscoveryManager.kt
│       │   │   │   ├── PreferencesManager.kt
│       │   │   │   └── IPreferencesManager.kt
│       │   │   ├── model\
│       │   │   │   └── AppModel.kt
│       │   │   └── ui\
│       │   │       ├── MainActivity.kt
│       │   │       ├── FocusAnimationEngine.kt
│       │   │       ├── SettingsDialog.kt
│       │   │       └── ContextMenuDialog.kt
│       │   └── res\
│       │       ├── drawable\ ...
│       │       ├── layout\ ...
│       │       ├── values\ ...
│       │       └── mipmap\ ...
│       └── test\java\com\minimal\tvlauncher\
│           ├── harness\
│           │   ├── TvTestDriver.kt
│           │   ├── FakeAppPackageGenerator.kt
│           │   └── BaseTvRobolectricTest.kt
│           ├── tier1\
│           ├── tier2\
│           ├── tier3\
│           ├── tier4\
│           └── perf\
├── gradle\
│   └── wrapper\
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew.bat
└── gradlew
```
