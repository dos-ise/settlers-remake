# v0.7.0.1

A follow-up to v0.7.0.0 that unifies the Android and desktop menus on shared OpenGL panels, giving both platforms the same look & feel and removing the duplicated native Android menu UIs.

## Assets

- **JSettlers.zip** – desktop game for Windows/Linux (includes the Map Editor and tools)
- **JSettlers-0.7.0.1-windows.zip** – portable Windows build (bundled JRE, unzip and double-click `JSettlers.exe`, no installation needed)
- **JSettlers-0.7.0.1-linux.zip** – portable Linux build (bundled JRE, unzip and run `JSettlers/bin/JSettlers`, works on any distro)
- **JSettlers-1.7.1-macos.zip** – portable macOS build (bundled JRE, unzip and open `JSettlers.app`; macOS requires a version that does not start with 0, so 0.7.0.1 is reported as 1.7.1)
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
