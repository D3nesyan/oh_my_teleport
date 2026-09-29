# oh_my_teleport — 设计文档

日期：2026-09-29
目标平台：Minecraft Java Edition **26.3**（Fabric）
Mod id：`oh_my_teleport`

## 1. 目标

单人游戏与 Fabric 服务器均可用的一套传送命令：死亡回返、玩家间请求式传送、家、个人传送点。纯服务端逻辑，不改客户端。

## 2. 范围外（明确不做）

- 跨服 / 跨维度代理传送（BungeeCord 类）
- GUI、快捷键、HUD
- 经济系统、冷却购买、传送门票
- 世界间 / 服务器间同步
- 随机传送 `/rtp`
- 任何需要 mixin 的逻辑（当前设计不需要 mixin）

## 3. 版本与构建

| 项 | 值 | 来源 |
|---|---|---|
| Minecraft | `26.3` | Mojang version manifest（`latest.release`） |
| Fabric Loader | `0.19.5` | maven.fabricmc.net |
| Fabric API | `0.161.0+26.3` | maven.fabricmc.net（`0.161.2+26.4` 是给 26.4 快照的） |
| Loom | `1.18-SNAPSHOT`，插件 id `net.fabricmc.fabric-loom` | fabric-example-mod `26.3` 模板 |
| Gradle | `9.7.1`（wrapper） | 同上 |
| Java | `25` | 26.1 起最低 Java 25 |

**26.1 起 Minecraft 不再混淆**，Yarn 已停止支持（最后一版 `1.21.11+build.6`）。因此：

- `gradle.properties` 里**没有** `yarn_mappings`
- `build.gradle` 里**没有** `mappings` 依赖行
- 代码使用 **Mojang 官方名**，不是 Yarn 名

Loom 新插件不再 remap：用 `implementation`（不是 `modImplementation`），产物任务用 `jar`（不是 `remapJar`）。

`fabric.mod.json` 的 `environment` 必须是 `"*"`：文档明确 `"server"` 只在专用服务器加载，单人不加载。

本机只装了 JDK 17/21/24，缺 25。构建时用 toolchain 声明 25；daemon 若被 Loom 拒绝，改用独立下载的 JDK 25 作为 `JAVA_HOME`。

## 4. 命令面

| 命令 | 说明 |
|---|---|
| `/sethome [name]` | 在当前位置设家，`name` 省略时是 `home` |
| `/home [name]` | 回家，省略时是 `home` |
| `/delhome <name>` | 删家 |
| `/homes` | 列出自己的家 |
| `/warp set <name>` | 在当前位置建个人传送点 |
| `/warp <name>` | 去自己的传送点 |
| `/warp del <name>` | 删传送点 |
| `/warp list`（直接打 `/warp` 同效） | 列自己的传送点 |
| `/tpa <player>` | 请求传送到该玩家处 |
| `/tpaccept [player]` | 接受请求；无参数时接受唯一待处理请求 |
| `/tpadeny [player]` | 拒绝请求；无参数时拒绝唯一待处理请求 |
| `/back` | 回死亡点（用一次失效）；无死亡点时回上一个传送前位置 |

全部命令玩家级，不需要 OP，不使用任何权限 API。
`/warp` 的 `set` / `del` / `list` 是字面量子命令，因此**名为 `set`、`del`、`list` 的 warp 不可用**（已文档化）。

## 5. 架构

分两层，边界是「纯逻辑」与「Minecraft API」。

### 纯逻辑层（无 MC 依赖，JUnit 可测）

- `TeleportPoint` — record `(String dimensionId, double x, y, z, float yaw, float pitch)`。
  维度存**字符串 id**而非 `ResourceKey<Level>`，正是为了这一层能脱离 MC 运行。
- `PointStore` — `LinkedHashMap<String, TeleportPoint>`，负责 put/get/remove/names/contains，
  并做名字合法性校验（去空白、长度上限、禁止空）。
- `PlayerData` — 一个 UUID 的聚合：`homes: PointStore`、`warps: PointStore`、
  `lastTeleportOrigin: TeleportPoint?`。
- `PlayerDataStore` — `Map<UUID, PlayerData>`，负责 get-or-create 与遍历。
- `TpaRegistry` — 请求表。`Map<UUID requester, TpaRequest>`，`TpaRequest(requester, target, createdAtMillis)`。
  时钟由构造器注入 `LongSupplier`，便于测试。
- `BackResolver` — 纯函数：给定 `deathPoint: TeleportPoint?` 与 `lastOrigin: TeleportPoint?`，
  返回该去哪或「无可返回位置」。
- `NameRules` — warp / home 名字合法性规则，被 `PointStore` 与命令参数解析共用。

### Minecraft 层（薄适配）

- `OhMyTeleport implements ModInitializer` — 只做注册接线。
- `TeleportPoints` — MC 与 core 之间的类型转换（玩家当前位置、原版死亡点、维度字符串 ↔ `ServerLevel`），
  并且是**整个 mod 唯一**调用 `ServerPlayer.teleport(TeleportTransition)` 的地方。
- `TeleportService` — 传送编排：解析维度、记录 `lastTeleportOrigin`、调用 `TeleportPoints.move`，
  失败时返回一个 `Failure(key, fallback)` 给命令层。
- `TpaService` — 持有那个进程内唯一的 `TpaRegistry`。放在这里而不是命令类里，是为了让命令类保持纯派发，
  也让测试能直接塞请求进去。
- `DimensionCodec`（或并入 `PlayerDataCodec`）— `ResourceKey<Level>` ↔ 字符串 id 互转。
- `PlayerDataSavedData` — 挂在主世界的 `SavedData`，持有 `PlayerDataStore`，提供 codec。
- `CommandRegistry` — 组装 Brigadier 树，把命令委托给纯逻辑层。
- 各自 `HomeCommand` / `WarpCommand` / `TpaCommand` / `BackCommand`。
- `Messages` — 统一的文案出口，用 `Component.translatableWithFallback`。

**不通用的规则：命令类只做参数解析与文案，不含传送逻辑；传送逻辑只存在于 `TeleportAdapter`；纯逻辑层不 import 任何 `net.minecraft.*`。**

## 6. 数据模型与持久化

`SavedData` + `SavedDataType`，文件落在 `<world>/data/oh_my_teleport.dat`，由主世界的
`getDataStorage().computeIfAbsent(TYPE)` 取得。26.x 的 `SavedData` 只有 codec 路径（没有 NBT 读写对），
所以 `PlayerDataStore` 需要一个手写 codec：用 DFU 原语（`Codec.STRING`、`Codec.DOUBLE`、
`Codec.FLOAT`、`Codec.unboundedMap`、`RecordCodecBuilder`）组合，不依赖 MC 的 codec 常量。

每次写入后必须 `setDirty()`，否则不落盘。

选择 `SavedData` 而非「Gson 手写 JSON」（参考 mod 的做法）的理由：随世界保存自动落盘，
不需要 `SERVER_STARTED` / `SERVER_STOPPING` 生命周期钩子，崩溃时不易丢数据。代价是 codec 啰嗦。

## 7. `/back` 语义

1. `/back` 时先看 `player.getLastDeathLocation()`。有值 → 传到那里，然后
   `setLastDeathLocation(Optional.empty())` 清掉，于是「用一次失效」，第二次 `/back` 落到下一条规则。
2. 没有死亡点 → 用 `lastTeleportOrigin`（上一次本 mod 传送前的位置）。
3. 两者都没有 → 报错，不传送。

死亡点用原版的，不自己存、不写 mixin，因此**死后掉线重登再 `/back` 依然有效**。

`lastTeleportOrigin` 只在本 mod 自己的传送前更新。原版 `/tp` 或其它 mod 的传送不会更新它 —— 已知限制，写进 README。

## 8. TPA 语义

- 每个请求者同一时刻最多一份**待处理**请求。请求者再次 `/tpa` 会覆盖自己上一份（旧请求静默作废）。
- 不同请求者发给同一目标的请求**并存**。
- 请求 60 秒过期，过期即失效（`TpaRegistry.prune(now)` 在查询与接受时调用，不额外开 tick 任务）。
- `/tpaccept` 不带参数：该目标恰有一份待处理请求时接受它；有多份时列出发送者并要求带名字。
- `/tpadeny` 同上。
- 请求者离线、目标离线、二者相同、已是同维度同位置等边界情况各自有明确文案。
- 接受后：**请求者**被传送到**目标**处（`/tpa` 的语义是「我要去你那」）。

`TpaRegistry` 是内存态，不持久化 —— 服务器重启丢掉待处理请求是期望行为。

## 9. 文案与本地化

服务端 mod 只装在服务器上时，客户端没有 lang 文件，`translatable` 会显示成 key。
因此所有文案走 `Component.translatableWithFallback(key, 英文兜底)`，并附带
`assets/oh_my_teleport/lang/en_us.json` 与 `zh_cn.json`。
（`translatableWithFallback` 在 26.3 的确切签名尚未逐个验证，编译报错则退回 `Component.literal` 兜底。）

## 10. 测试策略

两层，都能无头运行。

**单元测试（JUnit 5）**，跑 `./gradlew test`：`PointStore`、`NameRules`、`TpaRegistry`、`BackResolver`、
`PlayerData`、`PlayerDataCodec`（codec 用 `JsonOps` 往返，只需 DataFixerUpper，不需要 MC 运行时）。
这些类不 import `net.minecraft.*`，所以测试不启动游戏。48 个用例。

**游戏内测试（Fabric gametest）**，跑 `./gradlew runGameTest`：起无头服务器，用真实的
`GameTestHelper.makeMockServerPlayerInLevel()` 造玩家，用 `Commands.performPrefixedCommand` 打真实命令，
断言玩家实际坐标与存档内容。这一层覆盖单元测试碰不到的：命令派发、`SavedData` 落盘、真正的传送调用。
11 个用例，覆盖 `/sethome`+`/home`、命名家、`/warp`、`/back` 的两条分支与「无处可回」分支、
`/tpaccept` 与 `/tpadeny`，以及「多份待处理请求时不带参数的 `/tpaccept` 必须拒绝且不传送任何人」。

测试类放在 `com.ratoriku.ohmyteleport.gametest`，只在 `-Dfabric-api.gametest` 下激活。

**先写测试再写实现**（TDD）：每个纯逻辑类先落一个失败测试。

## 11. 已知限制

- `lastTeleportOrigin` 只跟踪本 mod 的传送。
- `set` / `del` / `list` 不能作为 warp 名。
- 待处理 TPA 请求不跨服务器重启。
- 单人存档与服务器的数据互不相通（各自存在各自世界的 `data/` 下）。

## 12. 构建环境的开放风险

本机缺 JDK 25。方案按顺序尝试：
1. Gradle toolchain 声明 25 + foojay resolver 自动下载（只解决编译）
2. daemon JVM 被 Loom 拒绝 → 用 Gradle 9 的 daemon JVM 条件自动下载
3. 仍不行 → 下载独立 JDK 25 到用户目录，以该 JDK 作为 `JAVA_HOME` 运行 Gradle

首次构建要从零拉取 Gradle 9.7.1、Loom、MC 26.3、Fabric API（本机无 `~/.gradle` 缓存）。
