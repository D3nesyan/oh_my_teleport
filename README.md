# Oh My Teleport

English | [中文](README.zh_cn.md)

Teleport commands for **Minecraft 26.3** on **Fabric**. Works in singleplayer and on Fabric servers.

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## Features

- `/back` — return to your death point, then to wherever you were before your last teleport
- `/tpa` + `/tpaccept` — request-based teleporting between players, with 60-second expiry
- `/home` + `/sethome` — named homes, stored per player
- `/warp` — personal named teleport points
- No OP required; every player has their own homes, warps and `/back` history
- Works across modded dimensions (Aether, Twilight Forest, …) with no extra config

## Commands

| Command | Description |
| --- | --- |
| `/sethome [name]` | Set a home here. Defaults to `home`. |
| `/home [name]` | Teleport to a home. |
| `/delhome <name>` | Delete a home. |
| `/homes` | List your homes. |
| `/warp set <name>` | Set a warp point here. |
| `/warp <name>` | Teleport to a warp point. |
| `/warp del <name>` | Delete a warp point. |
| `/warp list` (or `/warp`) | List your warp points. |
| `/tpa <player>` | Ask to teleport to another player. |
| `/tpaccept [player]` | Accept a request. |
| `/tpadeny [player]` | Deny a request. |
| `/back` | Return to your death point; afterwards, to your pre-teleport position. |

Points and names are per player — you cannot see or use anyone else's.

## Behaviour notes

- Names are case-insensitive (stored lowercase), up to 32 characters with no spaces.
- `set`, `del` and `list` are `/warp` subcommands, so they cannot be used as warp names.
- Each player has at most one outgoing `/tpa` request; sending another replaces it and notifies the old target. Requests to one target from different players coexist. Requests expire after 60 seconds.
- With multiple pending requests, `/tpaccept` with no argument lists the senders and asks for a name.
- A death point is consumed by the first `/back`; the next `/back` returns to the pre-teleport origin. The death location uses vanilla data, so it survives a relog.
- Every teleport reports the destination coordinates, and names the dimension only when it changes.
- Only this mod's teleports update the pre-teleport origin; vanilla `/tp` and other mods are not recorded.

## Modded dimensions

Homes and warps store the dimension id as a string and resolve it the same way vanilla dimensions are resolved, so custom dimensions work as-is. An unloaded or missing dimension fails cleanly with "target dimension is not loaded".

Vanilla ships no display names for modded dimensions, so they are shown by full id by default (e.g. `aether:the_aether`). Modpack authors can name any dimension with a resource pack — no code change needed:

```json
{
  "oh_my_teleport.msg.dimension.twilightforest.twilight_forest": "Twilight Forest",
  "oh_my_teleport.msg.dimension.aether.the_aether": "The Aether"
}
```

The key is `oh_my_teleport.msg.dimension.` + the dimension id with `:` replaced by `.`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5 or newer.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Drop the jar from [Releases](../../releases) into your `mods/` folder.

In singleplayer, cheats must be enabled to use commands. The mod reports `environment: "*"` on purpose so it also loads in singleplayer; a `server`-only environment would not.

## Building

Requires **JDK 25**, both for the toolchain and for the Gradle daemon itself.

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

The jar is written to `build/libs/oh_my_teleport-fabric-mc26.3-v1.0.0.jar`. Do not install the `-sources.jar`.

Tests:

```bash
./gradlew test         # unit tests
./gradlew runGameTest  # headless in-world tests
```

## License

[MIT](LICENSE)
