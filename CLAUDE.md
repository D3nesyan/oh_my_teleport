# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`oh_my_teleport` — a Fabric mod for **Minecraft 26.3** adding `/back`, `/tpa` + `/tpaccept`, `/home` + `/sethome`, and personal `/warp` points. Works in singleplayer and on Fabric servers.

## Build and test

Every Gradle invocation needs **JDK 25**, because Loom refuses to configure on an older Gradle daemon ("Dependency requires at least JVM runtime version 25"). This machine has JDK 25 only at `~/.jdks/jdk-25.0.4.1+1`; `JAVA_HOME` defaults to JDK 24, which fails.

```bash
JAVA_HOME="C:/Users/Ratoriku/.jdks/jdk-25.0.4.1+1" ./gradlew build        # compile + unit tests + jar
JAVA_HOME="C:/Users/Ratoriku/.jdks/jdk-25.0.4.1+1" ./gradlew test         # unit tests only
JAVA_HOME="C:/Users/Ratoriku/.jdks/jdk-25.0.4.1+1" ./gradlew test --tests '*TpaRegistryTest*'   # single test class
JAVA_HOME="C:/Users/Ratoriku/.jdks/jdk-25.0.4.1+1" ./gradlew runGameTest  # headless in-world tests
JAVA_HOME="C:/Users/Ratoriku/.jdks/jdk-25.0.4.1+1" ./gradlew runServer    # dev server for manual checks
```

`runServer` reads console commands from stdin and exits on stdin EOF, so hold it open (`tail -f /dev/null | ./gradlew runServer`). `run/` holds the dev world (`run/eula.txt`, `run/server.properties` with RCON pre-enabled) and is gitignored; `tools/RconProbe.java` is a minimal RCON client for smoke tests.

Unit tests report nothing on success — read `build/test-results/test/*.xml` to confirm counts. Gametest results land in `build/junit.xml`.

## Non-obvious platform facts (26.3)

Minecraft 26.1 was the first unobfuscated release. **Yarn no longer exists** (last version `1.21.11+build.6`), there is no `yarn_mappings` and no `mappings` dependency, and all code uses **Mojang names**: `Component` (not `Text`), `net.minecraft.resources.Identifier` (not `ResourceLocation`), `ServerPlayer`/`ServerLevel`/`Level`. The Loom plugin id is `net.fabricmc.fabric-loom` and it does not remap (`implementation`, not `modImplementation`; plain `jar`, not `remapJar`).

Online tutorials and older mod source are wrong about this. **To settle any API question, run `javap` against the real jar** rather than trusting docs (docs.fabricmc.net lags at 26.2):

```bash
~/.jdks/jdk-25.0.4.1+1/bin/javap -cp ~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar net.minecraft.server.level.ServerPlayer
```

Renames that have already bitten here: `ResourceKey.location()` → `identifier()`; `Entity.getServer()` is gone (use `player.level().getServer()`); `GameProfile.getName()` → `name()` (authlib 10 made it a record); `sendFeedback`/`sendError` → `sendSuccess(Supplier<Component>, boolean)`/`sendFailure(Component)`; `PersistentState` → `SavedData`; `suggestMatching` → `SharedSuggestionProvider.suggest`.

## Architecture

Two layers, and the boundary is enforced by the build: the `core` package must not import `net.minecraft.*`, which is what makes it unit-testable without launching the game.

- **`core/`** — pure logic. `TeleportPoint` stores its dimension as a **plain id string** (not `ResourceKey<Level>`) precisely to keep this layer MC-free. `PointStore` (named points, case-insensitive via `NameRules`), `PlayerData` (homes + warps + `lastTeleportOrigin`), `PlayerDataStore`, `TpaRegistry` (clock injected via `LongSupplier`), `BackResolver`, `DimensionNames` (readable dimension names; pure so it can be tested).
- **`data/`** — persistence. `PlayerDataCodec` is written with DataFixerUpper primitives only, so it round-trips through `JsonOps` in a plain unit test. `PlayerDataSavedData` is the `SavedData` attached to the overworld; 26.x is codec-only with no NBT read/write pair, and **every mutation must go through `PlayerDataSavedData.mutate`** or nothing reaches disk.
- **`server/`** — MC glue. `TeleportPoints` is the **only place that calls `player.teleport(...)`**; `TeleportService` records `lastTeleportOrigin` before moving (this is what makes `/back` able to undo a teleport), sends the arrival notice ("arrived in DIMENSION at X, Y, Z"), and returns a `Failure(key, fallback)` instead of throwing; `TpaService` owns the one process-wide `TpaRegistry`.
- **`command/`** — Brigadier dispatch only, no teleport logic and no shared text.
- **`text/`** — `Messages`, the single outlet for player-facing strings, using `translatableWithFallback` so a client without the mod still sees English rather than a raw key. It sits outside `command/` because `server/` needs it too and must not depend on `command/`.
- **`gametest/`** — in-world tests, inert unless launched with `-Dfabric-api.gametest`.

Behaviours that are deliberate and easy to break by "fixing" them:

- The arrival notice is sent by `TeleportService` rather than by each command, so no command can forget it. Consequence: the notice is printed *before* the command's own message. The dimension is named only when the teleport crossed one (`teleported` vs `teleported_in_dimension`), because vanilla ships no dimension name keys and a modded dimension would otherwise print its raw id on every teleport inside it.
- Teleporting is dimension-agnostic on purpose: points store a dimension **id string** resolved through `ResourceKey.create(Registries.DIMENSION, …)` + `server.getLevel(…)`, the same path for `aether:the_aether` as for `minecraft:the_nether`. Never add a check for vanilla dimensions. A dimension that is not loaded must keep failing with `unknown_dimension` rather than throwing.
- `/back` reads vanilla's own death location and **clears it** (`setLastDeathLocation(Optional.empty())`) once used, so the second `/back` falls through to the pre-teleport origin. No mixin, no stored death point.
- `TpaRegistry` keeps **one pending request per requester** (a new `/tpa` replaces their own, returning the replaced request so the old target can be notified) while requests from different requesters to one target coexist; expiry is lazy on read, so there is no ticking task.
- Pending TPA requests are intentionally not persisted; a restart dropping them is correct.

## Testing

Unit tests cover `core` and the codec. Gametests cover what unit tests cannot: command dispatch, `SavedData` write-through, and the real teleport call — they drive actual commands through `Commands.performPrefixedCommand` against `GameTestHelper.makeMockServerPlayerInLevel()` players and assert real coordinates.

Mock players all share the display name `test-mock-player`, so anything requiring player-name resolution (e.g. `/tpa <player>`, `/tpaccept <player>`) cannot be driven through commands in a gametest — seed `TpaService.registry()` directly instead.

Two limits worth knowing: `Commands.performPrefixedCommand` **swallows exceptions** (a throw inside a command becomes a failed command, and position assertions still pass), so anything that must fail loudly should call the service directly — see `theTeleportServiceMovesThePlayerAndAnnouncesIt`. And mock players do not record chat, so message text and localisation can only be checked by eye in a real client; server log lines come from console-source feedback, not from player chat.

Spec and design rationale: [docs/superpowers/specs/2026-09-29-oh-my-teleport-design.md](docs/superpowers/specs/2026-09-29-oh-my-teleport-design.md).
