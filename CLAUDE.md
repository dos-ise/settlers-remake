# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

JSettlers is a Java remake of *The Settlers 3* (Gradle multi-module, desktop + Android). Most AI/gameplay logic lives in `jsettlers.logic`. This file focuses on the non-obvious architecture and the workflow traps that waste time if you don't know them.

## Overview

JSettlers is a Java remake of the 1998 strategy game *The Settlers 3*. It targets PC (Windows/Linux) and Android (macOS is broken). It requires Java 11+ to build/run, and the original game's `GFX`/`SND`/`MAP` asset folders at runtime (the repo ships only code, not those copyrighted assets). On first launch the game prompts for the folder containing those assets.

## Build & Run

Gradle multi-module build. Use the wrapper (`./gradlew` / `gradlew.bat`). The Gradle daemon is disabled (`gradle.properties`), so builds start a fresh JVM each time.

> **Before building locally, read `docs/LOCAL_BUILD.md`.** It documents machine-specific workarounds that are *not* in this file: the corporate proxy setup for the Gradle distribution and dependencies, the null-safe `findGitRevision` fix and the `GIT_DIR` override needed to build in linked git worktrees, and the exact Android SDK paths. Builds fail with confusing JGit errors (`Cannot get property 'name' on null`, `Missing commit <sha>`) if these are skipped.

- **JDK 17** (the code targets Java 11 source-compat, but the Gradle toolchain builds with JDK 17). Always pass `--no-daemon` for test/CI-style runs — background daemons from earlier runs otherwise starve later ones and look like regressions.
- Compile logic only (fast feedback): `./gradlew :jsettlers.logic:compileJava :jsettlers.logic:compileTestJava --no-daemon`
- Run a single test: `./gradlew :jsettlers.logic:test --tests 'jsettlers.integration.ai.ColonizationIT' --no-daemon --info`
- Desktop entry point: `jsettlers.main.swing/.../SwingManagedJSettlers.java`. Packaging/running the game is covered in the upstream wiki (linked from README.md); it needs original Settlers 3 `GFX`/`SND` data files to run.
- Clear stuck Gradle daemons with `./gradlew --stop`. **Never `taskkill //IM java.exe`** — a user's running game is a `java` process and will be killed with it.

```bash
./gradlew run                         # run the Swing desktop game (root task delegates to :jsettlers.main.swing:run)
./gradlew runMapCreator               # run the map editor
./gradlew runTools                    # run :jsettlers.tools
./gradlew :jsettlers.main.swing:shadowJar   # build the runnable fat JAR -> jsettlers.main.swing/build/libs/JSettlers.jar
./gradlew release                     # assemble release zips (game + dedicated server + android) into release/
```

Run a built JAR directly: `java -jar jsettlers.main.swing/build/libs/JSettlers.jar`. The repo includes `Jogar-JSettlers.bat`, a local launcher (not committed upstream) that forces `--backend=glfw` because the default WGL backend crashes on this machine; the GOG asset path is already cached under `%APPDATA%\.jsettlers`.

Android is auto-enabled only when an Android SDK is found (`ANDROID_HOME` env var or `local.properties` with `sdk.dir=`); otherwise `settings.gradle` prints a warning and skips the `go.graphics.android` / `jsettlers.main.android` modules.

### Runtime configuration

Behavior flags can be set in an `options.prp` file next to the JAR, or as `--double-dash` command-line args. Examples: `--all-ai`, `--fixed-ai-type=ROMAN_VERY_HARD`, `--disable-ai`, `--locale=en_en`, `--backend=glfw`.

## Testing

```bash
./gradlew test                              # all tests across modules
./gradlew :jsettlers.logic:unitTest         # logic unit tests only (excludes jsettlers/integration/**)
./gradlew :jsettlers.logic:autoReplayIT     # replay determinism tests
./gradlew :jsettlers.logic:replayValidationIT
./gradlew :jsettlers.logic:aiDifficultiesIT # AI strength regression
./gradlew :jsettlers.tests:test
```

Run a single test class/method with the standard Gradle filter, e.g.
`./gradlew :jsettlers.logic:test --tests '*MovableTest'` or `--tests 'jsettlers.foo.BarTest.someMethod'`.

JUnit 4 is the test framework (injected globally for all `java`/`application` modules in the root `build.gradle`). Integration tests live under `jsettlers.logic/src/test/java/jsettlers/integration/` (AI + replay) and are split out by name so they can run independently from fast unit tests.

### Testing the AI

- **`AiDifficultiesIT`** — pits difficulty levels against each other on land maps; the balance regression guard. Long and cliff-edge sensitive: run **one battle per fresh JVM** with `--no-daemon`; re-run apparent flakes before trusting them. Any AI change must keep this green.
- **`ColonizationIT`** — end-to-end island-colonization test. **Local-only:** it loads a GOG Settlers 3 island map by absolute path and `assumeTrue`-skips (never fails) when absent, so it does not run in CI. A full run is ~20–70 min of simulation. Its per-step `[ColonizationIT...]` stdout is the primary debugging signal and is captured into the JUnit XML (`build/test-results/test/TEST-*.xml`), not the console.
- **Determinism:** `CommonConstants.DETERMINISTIC_AI` (default `false`). `AiExecutor`/`AiStatistics` normally run players and stat-updaters concurrently, so GUI-task enqueue order — and thus emergent AI outcomes — varies run-to-run. `ColonizationIT` sets `DETERMINISTIC_AI = true` so long emergent pipelines reproduce. When adding AI state that feeds a decision, keep it deterministic: use membership-only sets (never iterate a `HashSet` to pick), give tie-breaks a stable order (by value then x/y), and never touch `MatchConstants.aiRandom()` / `Math.random()` / `Date` in AI decision paths.

## Architecture

### Module dependency layering

```
jsettlers.common      core data types, enums, geometry, interfaces (no deps on other modules)
  ^
jsettlers.network     lockstep networking, the game clock, dedicated server (DedicatedServerApp)
  ^
jsettlers.logic       game simulation: grid, movables, buildings, AI, pathfinding, map loading
  ^
jsettlers.graphics    rendering of the game world + original Settlers-3 UI controls
  ^
jsettlers.main.swing  desktop entry point (SwingManagedJSettlers), menus, replay playback
```

Rendering is split: `go.graphics` is a backend-agnostic 2D drawing abstraction; `go.graphics.swing` / `go.graphics.android` are the concrete LWJGL/OpenGL and Android backends. `jsettlers.graphics` draws *on top of* `go.graphics`, never directly against a GL backend.

Other modules: `jsettlers.mapcreator` (map editor), `jsettlers.tools` (asset/dev tools), `jsettlers.testutils` (shared test helpers, e.g. `TestUtils.setupTempResourceManager()`), `jsettlers.tests` (a meta-module that only aggregates and runs tests from other modules — it has empty `main`/`test` srcDirs pointing at `helpers`/`tests`).

### Module map (big picture)

- `jsettlers.common` — shared value types: `EBuildingType`, `EMaterialType`, `EMovableType`, `EResourceType`, positions, `CommonConstants` (global flags/toggles).
- `jsettlers.logic` — the simulation: map grid, partitions, buildings, movables (unit behaviour trees), and **all AI** (`jsettlers.ai.*`). This is where nearly all gameplay work happens.
- `jsettlers.graphics` / `go.graphics(.swing)` — rendering; `jsettlers.main.swing` — desktop app + replay tools; `jsettlers.network` — multiplayer; `jsettlers.testutils` — test harness (`TestUtils.setupTempResourceManager()`, `MainGridDataAccessor` test seams).

### Determinism & the lockstep model — read this before touching simulation code

The entire game simulation must be **deterministic**: the same initial state + the same ordered stream of player actions must always produce the identical result. This is what enables multiplayer (only actions are sent over the network, not state) and replays (the `*_replay.log` files referenced in bug reports). The `AutoReplayIT` / `ReplayValidationIT` tests guard this by replaying recorded games and asserting the outcome matches.

Consequences when editing `jsettlers.logic`:
- **Never call `java.util.Random` or `Math.random()` directly.** Use `MatchConstants.random()` (game logic) or `MatchConstants.aiRandom()` (AI). Both are seeded `ExtendedRandom` instances created in `MatchConstants.init(clock, randomSeed)` and are serialized with the save game.
- All time/scheduling goes through `MatchConstants.clock()` (an `IGameClock` from `jsettlers.network`). Do not use wall-clock time in simulation logic.
- Avoid iteration order that depends on `HashMap`/`HashSet` hash codes or object identity for anything affecting the simulation.

`MainGrid` (`jsettlers.logic/.../map/grid/MainGrid.java`) is the central simulation world; movables (settlers/soldiers), buildings, partitions, fog-of-war and pathfinding (`jsettlers.logic/.../algorithms/path/astar`) all operate against it. Periodic behavior is driven by the game clock via `RescheduleTimer`, not threads.

### buildSrc — custom Gradle tasks

`buildSrc/` contains two in-tree Gradle plugins/tasks used during the build:
- `jsettlers.shaders.compile` — compiles GLSL shaders.
- `jsettlers.textures.generation` — generates packed textures from source images + a texture index.

## The two partition systems (the #1 source of subtle bugs)

Every tile belongs to two *different* partitions; confusing them causes silent failures.

- **WALK / blocked partition** — `landscapeGrid.getBlockedPartitionAt(x,y)` and `LandscapeGrid.isReachable(x1,y1,x2,y2,ship)`. Movement/landmass/sea connectivity. Negative ids are navigable sea.
- **TERRITORY partition** — `partitionsGrid.getPartitionIdAt(x,y)`. Ownership, **material stock**, and **bearer delivery**. Each `PlayerStatistic.partitionIdToBuildOn` is the single home partition the AI builds on.

Consequences that are load-bearing across the AI:
- **Goods move by bearers only WITHIN one territory partition.** Across water they move ONLY by sea-trade: a `HarborBuilding` → `CargoShipMovable` drops materials on a reachable coast, then bearers within that partition. Ferries carry only `IAttackableHumanMovable` (pioneers/soldiers/geologists) — **never plain bearers and never goods**.
- **A building's worker = a jobless BEARER of that building's partition + a tool** (scythe→farmer, hammer→bricklayer, blade→digger, pick→miner; miller/baker/waterworker need none). No spare bearer or missing tool → the building stands finished-but-unworked, or half-built with materials on site.
- **Population grows only via living houses** (`SpawnBuilding` subclasses, e.g. `SmallLivinghouse`): they spawn bearers at their own door into their own territory partition and self-fund beds. This is the *only* way a partition with no home connection (an overseas colony) gains labour.
- **Boxing trap:** partition ids are `short`. `Set<Integer>.contains(getPartitionIdAt())` autoboxes to `Short` and silently never matches — always cast to `int` first. This bug has bitten here before.

## AI architecture

`AiExecutor` ticks each AI player's `WhatToDoAi`, which delegates to two halves:

- **Economy** — `EconomyMinister` (impl `BuildingListEconomyMinister`, `jsettlers.ai.economy`) produces an *ordered* `getBuildingsToBuild()` list each tick: building material (wood/stone) → mana → interleaved food + material + weapons/gold, all scaled by map capacity (`AiMapInformation`) and difficulty factors. `determineFoodBuildings()` encodes the farm→mill→baker→waterworks(+pig/slaughter) ratios. `getLandForPlayer` is the candidate tile set the construction position-finders (`jsettlers.ai.construction.*`) iterate.
- **Army** — `ArmyGeneral` (impl `ModularGeneral`) runs a list of `ArmyModule`s each tick, order-sensitive (later modules can override earlier ones by claiming soldiers into `soldiersWithOrders`): defense, soldier production/upgrade/heal, opponent adaptation, harassment, `SimpleAttack`, naval invasion, colonization, and `Regroup` (runs last — pulls unclaimed soldiers home, which is why in-progress attacks must register their soldiers).
- `AiStatistics` is the shared read-model (rebuilt each tick); most "does the player have/own X" queries and the colonization/suppliability scans live here.

Unit behaviour is behaviour-tree style in `jsettlers.logic.movable.*` (e.g. `SoldierMovable`, `PioneerMovable`, `GeologistMovable`). `EMoveToType.DEFAULT` = work-on-arrival (a geologist surveys, a pioneer claims); `FORCED` suppresses that (needed to board a ferry without surveying/claiming en route).

## Cross-water colonization (`ColonizationModule` + `ColonizationBuildModule`)

The most heavily-developed AI subsystem, and the one where the two-partition rules matter most. Flow: decide/claim a beachhead (`ColonizationModule`, gated to AI_HARD/VERY_HARD + a ready dockyard + a ferry sea route) → build it up (`ColonizationBuildModule`: tower to hold, farm, living house to grow labour, barrack, harbor to export). Established engine limits that shape the design:

- **Landlocked island ore is a dead end:** an overseas mine only produces if its ore's territory partition touches a cargo-ship-reachable coast. Most island ore doesn't, so the AI leads with a **farm** (self-producing on a coastal beachhead) and ships the crop home to be baked there, rather than mining.
- The colony's labour comes from converting ferried pioneers (capped) plus a beachhead **living house**; a barrack turns a local bearer + a *shipped* SWORD into a soldier (no overseas weapon chain needed).

## Code style

Formatting and import order are enforced via the Eclipse formatter config and Checkstyle config in `docs/eclipse-setup/` (`formatterConfiguration.xml`, `checkstyle.xml`, `checkstyle-header`). Indentation is **tabs**. Every source file must start with the MIT-style license header from `docs/eclipse-setup/checkstyle-header` (with the appropriate copyright year). See `docs/eclipse-setup/` screenshots for IDE setup (Eclipse and IntelliJ).

## Workflow gotchas

- **Do not commit `gradle.properties` trust-store lines.** Local dev may add trust-store entries to work around a corporate HTTPS proxy; `git checkout -- gradle.properties` before committing. Never import the proxy root CA into the JDK cacerts.
- **Linked git worktrees fail to build on a branch** with `Cannot get property 'name' on null` (a JGit-version task). Work around with `git checkout --detach HEAD` before building in a worktree. Agent worktrees also branch from a *stale cached base*, not current HEAD — `git reset --hard <SHA>` to the intended commit first.
- Behaviour-preserving AI changes (dead-code deletion, gated additions inert on land/single-partition maps) can be verified by compile + argument; anything that changes emergent behaviour needs the relevant IT run. State which you relied on.
- **`AutoReplayIT` is effectively disabled.** `AutoReplaySetting.getDefaultSettings()` returns an empty list — the `fullproduction` setting is commented out ("TODO currently broken"). The reference savegames (`jsettlers.testutils/.../replay/fullproduction/savegame-*.zmap`) are stale and untested. To re-enable: uncomment the setting, regenerate the savegames with `RegenerateAutoReplayITReferences`, and get the test green.