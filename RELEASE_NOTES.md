# v0.7.0.0

The first release of the dos-ise fork, building on v0.6.0.9-alpha. It focuses on much smarter computer opponents, new gameplay features (peacetime, siege weapons, waypoints), a large set of Android improvements and many bug fixes.

## Assets

- **JSettlers.zip** – desktop game for Windows/Linux (includes the Map Editor and tools)
- **JSettlers-0.7.0.0-windows.zip** – portable Windows build (bundled JRE, unzip and double-click `JSettlers.exe`, no installation needed)
- **JSettlers-0.7.0.0-linux.zip** – portable Linux build (bundled JRE, unzip and run `JSettlers/bin/JSettlers`, works on any distro)
- **JSettlers-1.7.0-macos.zip** – portable macOS build (bundled JRE, unzip and open `JSettlers.app`; macOS requires a version that does not start with 0, so 0.7.0.0 is reported as 1.7.0)
- **JSettlers-Android.apk** – Android app
- **DedicatedJSettlersServer.jar** – dedicated multiplayer server

## Computer opponents (AI)

The computer players have been reworked into much more capable opponents:

- **Smarter, more aggressive and less predictable attacks** – the AI picks weak spots (undefended towers), varies its targets from game to game, and presses the advantage when it is clearly winning.
- **Combat-outcome estimates** – the AI weighs the enemy's troop quality (upgrades included) before committing to an attack.
- **Proportionate, responsive defence** – defenders are distributed to the nearest breach in proportion to the threat, so a small raid no longer pulls the whole army away from the front.
- **Naval invasion AI** – computer players build dockyards, produce ferries and attack across water, including amphibious flanking around a defended land front, fuller ferry loads and multi-ferry invasion waves.
- **Cross-water colonization** – the AI now colonizes islands: it scans for targets, sends a colonization expedition, builds and holds a beachhead, grows the colony with a living house, fortifies it with towers and a barrack, and ships the harvest home. It can also colonize empty/farmland islands when no ore target exists.
- **Harassment and raiding** – with spare soldiers, the AI peels off small raiding parties against lightly defended buildings; raids break off and retreat when clearly losing.
- **Per-opponent adaptation and retaliation** – the AI adjusts how hard it presses based on how the war is going.
- **Seeded per-game personalities** – every computer player gets a random play style (turtle, balanced, aggressor, raider, ...) at game start.
- **Difficulty affects behaviour, not only economy** – harder levels use the advanced tactics; easy levels play a plainer game and now send out scouts to "explore" (first fog-of-war step).
- **Configurable attack grace period** – no unfair opening rush; configurable via `ai-attack-grace-seconds`.
- **Deterministic-AI mode** – opt-in reproducible AI for testing.
- **Balance fixes** – EGYPTIAN very-hard AI no longer loses to hard; ASIAN hard AI can beat easy AI again; fixed AI attack oscillation; harbors and dockyards can be built on the sandy coast.

## Gameplay

- **Peacetime** – optional 5–30 minute peace phase when creating a game (or *without*); no combat damage, no tower attacks, economy and scouting unaffected. The AI respects it.
- **Siege weapons** – new siege workshops and siege weapons with graphics, sounds and UI (Android, Swing and map editor), an ammunition production toggle, and the siege engineer is counted in the settler statistics.
- **Waypoints** for player-controlled movables.
- **Powder maker** building for the Asian civilisation.
- **Decorative objects** of original maps are now shown.
- **Savegame description** prompt when saving.
- **Start resources** are now applied in multiplayer games.
- **Soldier obedience** – soldiers obey a human player's order immediately, even mid-chase; soldiers no longer chase raiders across the map (pursuit leash).
- **Thief fix** – the thief now actually takes the good before the steal animation.
- **Selection kept** after converting settlers; **invert-mouse-drag** option; under-attack sound throttled.
- **Rendering fixes** – invisible ferry passengers, stray building-preview shadow, control panel oversizing on maximized windows, settlers drawn in front of/behind buildings.
- **Crash fix** – the game no longer crashes at start when the log directory is not writable.
- **Production statistics** and a "produce goods" win condition.

## Multiplayer

- Players, their **ping and connection state** are now shown in multiplayer games.
- **Spell bug fixes**, mana statistic, per-civilisation statistic layout, and the game asks before quitting.
- Fixed StatisticsRecorder scheduling beyond the RescheduleTimer horizon.

## Android

- **Ubisoft Connect login** and **game data download** to `/storage/emulated/0/JSettlers`.
- **targetSdk 33 (Android 13)**, compileSdk 35, build tools 36.0.0; all-files access on Android 11+.
- **Toggleable minimap**.
- **Convert bearers into specialists** in the settlers menu.
- **Material and goods priority** settings.
- **End-game statistics with history charts**.
- **Audio settings** – music on/off, music and sound volume (ported from the BotGGias fork).
- Various fixes (MediaMetadataRetriever crash, base64 padding, FOREGROUND_SERVICE permission).

## Build & CI

- Build with **Gradle 8 on JDK 21**, still producing Java 11 bytecode.
- Updated deprecated GitHub Actions.
- **Automated release workflow** – pushing a `v*` tag now builds and publishes the release with all six assets.
- **Native packages** – the desktop game is packaged with `jpackage` as portable app-images for Windows, Linux and macOS, each bundling its own JRE and shipped as a per-platform zip (no installer, no security-warning setup).
- **Manual CI trigger** – the workflow can be run manually (Actions → CI → Run workflow) to build and inspect the native packages without creating a release.
- Fixes ported from the BotGGias fork (fog-of-war save sync, movable draw order, geologist resource sound).
