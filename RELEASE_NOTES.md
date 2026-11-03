# v0.7.0.2

A maintenance release that fixes the in-app GOG download of the Settlers 3 game data on Android and brings the app up to date with Android 14+ requirements.

## Assets

- **JSettlers.zip** – desktop game for Windows/Linux (includes the Map Editor and tools)
- **JSettlers-0.7.0.2-windows.zip** – portable Windows build (bundled JRE, unzip and double-click `JSettlers.exe`, no installation needed)
- **JSettlers-0.7.0.2-linux.zip** – portable Linux build (bundled JRE, unzip and run `JSettlers/bin/JSettlers`, works on any distro)
- **JSettlers-1.7.0-macos.zip** – portable macOS build (bundled JRE, unzip and open `JSettlers.app`; macOS requires a version that does not start with 0, so 0.7.0.2 is reported as 1.7.0)
- **JSettlers-Android.apk** – Android app
- **DedicatedJSettlersServer.jar** – dedicated multiplayer server

## GOG download (Android)

The in-app GOG download of the Settlers 3 game data is fixed:

- The downloader previously picked the `win11_fix` **patch** instead of the real installer; it now skips patches and downloads the full installer (`.exe` + `setup-N.bin` data files).
- The game data is stored in **Inno Setup** format, which the app could not extract. A pure-Java Inno Setup extractor (with external `.bin` slice support) now extracts the `GFX`, `SND` and `Map` folders directly on the device.

## Android 14+ compatibility

- `targetSdk` raised to 35 (silences the Play Protect warning).
- Added the `FOREGROUND_SERVICE_SPECIAL_USE` permission required for the in-game notification service on Android 14+.
- Edge-to-edge rendering enabled and a `registerReceiver` crash on Android 14+ fixed.

## Fixes

- The game no longer crashes when the system restarts the notification service after the game quit (`NullPointerException` on a null `GameMenu`).
- The local folder picker now has a **"Select this folder"** button and validates the selection, showing a clear message instead of crashing on an invalid folder.
- The game data setup no longer crashes when returning from the GOG login screen.

# v0.7.0.1

A follow-up to v0.7.0.0 that unifies the Android and desktop menus on shared OpenGL panels, giving both platforms the same look & feel and removing the duplicated native Android menu UIs.

## Assets

- **JSettlers.zip** – desktop game for Windows/Linux (includes the Map Editor and tools)
- **JSettlers-0.7.0.1-windows.zip** – portable Windows build (bundled JRE, unzip and double-click `JSettlers.exe`, no installation needed)
- **JSettlers-0.7.0.1-linux.zip** – portable Linux build (bundled JRE, unzip and run `JSettlers/bin/JSettlers`, works on any distro)
- **JSettlers-1.7.0-macos.zip** – portable macOS build (bundled JRE, unzip and open `JSettlers.app`; macOS requires a version that does not start with 0, so 0.7.0.1 is reported as 1.7.0)
- **JSettlers-Android.apk** – Android app
- **DedicatedJSettlersServer.jar** – dedicated multiplayer server

## Unified menus (Android + desktop)

The main menu, settings, map selection, join-game, multiplayer picker and endgame statistics are now rendered by the **same shared OpenGL panels** on Android and desktop:

- **One codebase for all menus** – the Swing menu code was moved into `jsettlers.graphics` as backend-agnostic GL panels; the duplicated native Android fragments, view models and layouts were removed.
- **Single GLFW window on desktop** – the menu and the game no longer switch windows.
- **Consistent look & feel** – both platforms use the same menu graphics, gold-yellow text and layout.

## New features

- **Native save dialog** and a **delete-savegame button** in the save list.
- **Language selection** in the settings menu.
- **Fullscreen toggle** for the desktop window.
- **Map preview images** in the map selection menu.
- **Touch scrolling** and **text input** for the shared menus on Android.
- The in-game control panel and endgame statistics are shared between platforms too.

## Fixes

- Crash fixes when quitting the game, when the game menu dialog is recreated, and a `NullPointerException` in the settings toggle buttons.
- The render thread now stops cleanly before the game clock is cleared during shutdown.
- CI: the known-flaky AI difficulty battles and the replay-determinism test are excluded again until fixed.
