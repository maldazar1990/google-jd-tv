# Original User Request

## 2026-10-25T19:25:45Z

An ultra-lightweight, high-performance Android TV launcher inspired by Apple TV (tvOS), specifically engineered for the Google TV 2021 4K hardware (Amlogic, 2 GB RAM limit), featuring extensive accessibility options and icon customization.

Working directory: `c:\Users\jdgen\OneDrive\Documents\android tv apple tv`
Integrity mode: demo

## Requirements

### R1. TV-Optimized Interface (Apple TV Style)
- Deliver a clean tvOS-inspired layout: top status header (clock/date and settings shortcut) followed by a responsive app grid and a favorites row.
- Ensure fluid D-pad remote navigation with smooth focus scaling (1.08x), soft depth shadow, and glowing border highlight without dropping frames below 60 fps.

### R2. Ultra-Low Memory & Zero-Bloat Footprint
- Peak memory usage strictly maintained under 30 MB RAM using native Android Views and RecyclerView (avoiding heavy runtime layers).
- Fast cold start under 300 ms, with zero background telemetry, ads, or background worker battery drain.

### R3. Accessibility Options
- High-contrast visual mode (pure OLED black background `#000000` with vivid yellow `#FFD600` focus borders).
- Dynamic grid layout options: Standard (6 columns), Large (4 columns), Extra-Large (3 columns).
- Configurable text scaling and full D-pad sound feedback toggles.

### R4. Icon Customization & Management
- Selectable icon corner styles: Squircle (Apple TV style), Circle, and Soft Rounded Rectangle.
- Automatic banner and icon handling: proper display of TV banners with fallback generation for sideloaded mobile apps.
- Context menu on long-press (D-pad center / OK) to toggle favorites, hide system/unwanted apps, or open app details.

## Acceptance Criteria

### Performance & Memory
- [ ] Launcher memory footprint remains strictly under 30 MB during continuous rapid D-pad browsing.
- [ ] App launches without stutter or GC pauses on low-memory Android TV devices.

### Functionality & Navigation
- [ ] Properly registers `HOME`, `DEFAULT`, and `LEANBACK_LAUNCHER` intents in AndroidManifest.
- [ ] D-pad focus is never lost when scrolling between the top bar, favorites, and app grid.
- [ ] All installed TV and sideloaded apps are correctly detected, displayed, and launchable.
- [ ] Settings changes (contrast, grid columns, icon shapes) persist across reboots via lightweight storage.


## 2026-10-25T20:01:11Z

User Feature Request Update:
Please incorporate Requirement R5 into the project architecture and settings:
1. Bing Daily Wallpaper: Option to fetch and display the Bing 'Image of the Day' with a dark scrim overlay for contrast, switchable with the default dark gradient.
2. Modular Status Widgets: Add optional widgets in the top bar / ticker, each with its own ON/OFF toggle in PreferencesManager/Settings:
   - Live RAM usage (Free/Used)
   - Network status (Wi-Fi/Ethernet)
   - Weather widget
   - Google News RSS headlines ticker
All widgets must be individually toggleable so the user can keep the screen 100% minimal or enable specific info. Ensure strict zero-memory leaks and background caching.
