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


## 2026-10-25T20:11:11Z

Workflow Directive Update:
Implement Requirement R6 (Multi-Agent Branching & CI Integrator Gatekeeper):
1. Subagents must create and isolate their implementation work on dedicated feature branches (e.g., `feature/...`).
2. An Integrator / Gatekeeper subagent handles reviewing the branch, validating `./gradlew testDebugUnitTest`, and safely merging into `main`.
3. Keep main clean and push validated commits to origin. AGENTS.md has been updated accordingly.


## 2026-10-25T20:13:48Z

Pipeline Update:
The skill `.skills/tv-testing/SKILL.md` is now created and pushed to main:
1. Prior to merging any feature branch into main, the Integrator subagent must execute both the Backend checks (persistence, leak-free, double app scan, network resiliency) and Frontend checks (D-pad focus integrity, no focus traps, 60fps budget, TalkBack a11y, contrast).
2. When a version is ready for release, apply semantic versioning:
   - Increment versionCode and versionName in app/build.gradle.kts
   - Tag the git commit (vX.Y.Z) and update CHANGELOG.md.


## 2026-10-25T20:16:00Z

Pipeline Update:
Two-Tier Qualification Protocol recorded:
1. Tier 1: All unit, defensive, and boundary tests execute strictly in the isolated VM environment.
2. Tier 2: A pre-production test on the physical Google TV (192.168.2.100:38667) is required prior to any major release (vX.0.0). Both AGENTS.md and .skills/tv-testing/SKILL.md have been updated and pushed to main.


## 2026-10-25T20:17:08Z

Pre-PR QA Protocol Update:
Rule added to AGENTS.md and .skills/tv-testing/SKILL.md:
Before submitting any PR or requesting a merge to main:
1. Feature agents must test their code locally on their branch.
2. Feature agents must invoke a dedicated QA subagent to challenge the feature (boundary checks, leak verification, stress test).
3. Only upon QA subagent approval is the PR submitted to the Integrator Gatekeeper, preventing wasted integration and rebase cycles.


## 2026-10-25T20:18:14Z

Final Deliverable Requirement:
When implementation and testing are complete, a dedicated Documentation subagent must produce a comprehensive `DOCUMENTATION.md` file at the repository root covering:
1. Technical Architecture (native Views, <30MB RAM constraints, 60fps focus animations).
2. Remote Control & Navigation User Guide (D-pad gestures, long-press context menus).
3. Bing Wallpaper & Modular Widgets Configuration.
4. Accessibility Features (OLED High Contrast, custom grid columns, TalkBack).
5. Wireless ADB Deployment & Google TV Setup Guide.
6. Google Jules Maintenance Workflows.
Updated in AGENTS.md and pushed to main.


## 2026-10-25T20:31:50Z

JDK 17 and Gradle 8.5 test runner is 100% operational. `./gradlew.bat testDebugUnitTest` runs with 0 errors and all 40 tests pass.
Continue your planned milestones according to the multi-agent protocol in AGENTS.md (feature branches -> QA subagent validation -> merge to main -> build APK). Proceed with M2 (App Discovery, Sideload Fallback, and Bing Wallpaper/Widget services) and M3 (UI Grid & D-Pad focus).
