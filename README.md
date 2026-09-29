# oh_my_teleport

传送命令 mod，面向 **Minecraft 26.3**（Fabric），单人与 Fabric 服务器都能用。

## 命令

| 命令 | 说明 |
|---|---|
| `/sethome [名字]` | 在当前位置设家，省略名字时是 `home` |
| `/home [名字]` | 回家 |
| `/delhome <名字>` | 删家 |
| `/homes` | 列出自己的家 |
| `/warp set <名字>` | 在当前位置建个人传送点 |
| `/warp <名字>` | 去自己的传送点 |
| `/warp del <名字>` | 删传送点 |
| `/warp list`（或直接 `/warp`） | 列出自己的传送点 |
| `/tpa <玩家>` | 请求传送到该玩家处 |
| `/tpaccept [玩家]` | 接受请求 |
| `/tpadeny [玩家]` | 拒绝请求 |
| `/back` | 回死亡地点；用掉之后回上次传送前的位置 |

全部命令都是玩家级，**不需要 OP**。家、传送点、`/back` 的记录都是每人一份，互不可见。

## 行为说明

- **家与传送点是按玩家分开存的。** 别人的家你既看不到也用不了。
- **名字大小写不敏感**，会被转成小写，最多 32 个字符，不能含空格。
- 因为 `set`、`del`、`list` 是 `/warp` 的子命令，**不能拿这三个词当传送点的名字**。
- **`/tpa` 同时来多份请求时**：每个请求者同一时刻只有一份待处理请求，他再打一次 `/tpa` 会覆盖自己旧的那份（旧目标会收到一条提示）；不同的人发给同一个目标的请求会并存。请求 **60 秒**过期。目标那边如果同时有多份待处理请求，`/tpaccept` 不带参数会列出发送者并要求写名字。
- **`/back` 的死亡地点用一次就失效**，第二次 `/back` 回「上次传送前的位置」。死亡地点用的是原版数据，所以死后掉线重登，`/back` 依然有效。
- **每次传送都会报坐标**，格式仿原版 `/tp`：同维度是 `已传送至坐标 12.50, 64.00, -30.50`，**跨维度**才加上维度名变成 `已传送至维度 主世界 的坐标 12.50, 64.00, -30.50`（与原版老版本 `/tp` 的写法一致）。这条提示由 `TeleportService` 统一发出，所以四条命令都有；命令自己的文案（比如「欢迎回家」）会紧跟在它后面。
- **只有本 mod 自己的传送**（`/home`、`/warp`、`/tpa`、`/back`）会更新「上次传送前的位置」。原版 `/tp` 或别的 mod 传送不会被记录。

## 多维度模组（天堂、暮色森林等）

**传送本身没问题。** 家、传送点里存的是维度的 id 字符串（`twilightforest:twilight_forest` 这种），解析时走
`server.getLevel(ResourceKey.create(Registries.DIMENSION, id))` —— 和原版维度**是同一段代码**，没有「只认原版维度」的判断。跨维度传送有游戏内测试实测过一次（把玩家真传进下界）。

**维度不存在时不会炸。** 模组被卸载、维度没加载、存档里存着一个已经不存在的地点 —— 这些情况报「目标维度未加载」，不传送，不崩。有测试钉住这条。

**维度名字是另一回事。** 原版 lang 文件里 `dimension.*` 的翻译键**一个都没有**（不是没读到，是真没有），所以不存在可以自动对接的命名约定；唯一普遍存在的标识就是 id。于是：

- 原版三个维度显示为「主世界 / 下界 / 末地」
- **其它维度的默认显示是完整 id**（如 `aether:the_aether`）—— 不猜、不截断，免得两个维度看起来一样
- **整合包作者可以用资源包给任何维度起名**，不用改代码、不用等更新：

```json
{
  "oh_my_teleport.msg.dimension.twilightforest.twilight_forest": "暮色森林",
  "oh_my_teleport.msg.dimension.aether.the_aether": "天堂"
}
```

键名规则是 `oh_my_teleport.msg.dimension.` + 维度 id 把冒号换成点。因为这个键由 mod 自己的命名空间占着，不会和别的模组撞车。

而且**只在跨维度时才显示维度名**，所以在主世界里的 `/home`、`/warp` 永远不会看到裸 id，只有真的跨维度（那时你恰好需要知道自己在哪）才会出现。

## 构建

需要 **JDK 25**（Minecraft 26.1 起的最低要求；Loom 也会拒绝低版本 JVM）。

```bash
./gradlew build
```

`build.gradle` 里声明了 Java 25 的 toolchain，配合 `settings.gradle` 中的 foojay resolver，Gradle 可以自行下载 JDK 25 工具链。但 **Gradle daemon 自身**也必须跑在 Java 25 上，否则 Loom 会在配置阶段直接失败：

```
Dependency requires at least JVM runtime version 25. This build uses a Java 24 JVM.
```

所以如果你机器上的 `JAVA_HOME` 不是 25，这样跑：

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

产物在 `build/libs/oh_my_teleport-fabric-mc26.3-v1.0.0.jar`（文件名由 `build.gradle` 里的
`artifactBaseName` 拼出：mod 名 + 加载器 + Minecraft 版本 + mod 版本）。同目录的 `-sources.jar`
是源码包，只在调试时用，不要丢进 `mods/`。

本仓库开发时用的 JDK 25 放在 `~/.jdks/jdk-25.0.4.1+1`（Temurin），命令形如：

```bash
JAVA_HOME=~/.jdks/jdk-25.0.4.1+1 ./gradlew build
```

`tools/RconProbe.java` 是个极小的 RCON 客户端，用来在无头服务器里冒烟测试命令是否注册：

```bash
java tools/RconProbe.java localhost 25585 <rcon密码> help back
```

（服务端控制台不是玩家，所以只能证明命令注册成功，不能证明传送行为；传送行为由 gametest 覆盖。）

## 运行

单人：把 jar 丢进 `.minecraft/mods/`，需要 Fabric Loader 0.19.5+ 与 Fabric API。存档要开作弊才能用命令。

服务器：jar 丢进 `mods/`，同样需要 Fabric API。`fabric.mod.json` 的 `environment` 是 `"*"` —— 故意如此，因为 `"server"` 的 mod **单人游戏不会加载**。

数据存在 `<存档或服务器>/world/data/` 下，由原版 `SavedData` 机制随存档保存。

## 测试

两层，都能无头跑。

### 单元测试

```bash
./gradlew test
```

覆盖纯逻辑层（`core` 包与序列化 codec）。这些类不 import 任何 `net.minecraft.*`，所以不需要启动游戏。

### 游戏内测试（gametest）

```bash
./gradlew runGameTest
```

起一个无头服务器，在里面用真实的 `ServerPlayer` 跑真实命令，断言玩家的实际坐标与存档内容。覆盖了单元测试碰不到的东西：命令派发、`SavedData` 落盘、真正的传送调用。

已有用例：`/sethome` → `/home` 真的把人移回去、命名家互不干扰、`/warp set`/`del`/`/warp` 生效、`/back` 回死亡点**并把死亡点清掉**、`/back` 撤销上次传送、无目标时 `/back` 不动人、`/tpaccept` 真的把请求者传到目标处、`/tpadeny` 不动人且清掉请求、**同时有两份待处理请求时不带参数的 `/tpaccept` 拒绝执行且谁都不传**。

其中一个是**直接调用 `TeleportService`** 而不是走命令的：命令派发会把异常吞掉，直接调用才能让传送或聊天发送里的异常真的把测试打挂。

**测不到的**：提示文字的实际渲染。mock 玩家不记录聊天，服务器日志里也看不到，所以坐标提示的内容与本地化只能进游戏肉眼确认。

结果写在 `build/junit.xml`。这些测试类只在启动参数带 `-Dfabric-api.gametest` 时才激活，正常游戏里是死代码。

### 仍然需要手动验证的

单人开作弊，逐条：

1. 退出重进游戏 → `/home`、`/warp list` 仍在（跨存档的持久化）
2. 用两个号连续 `/tpa` 同一目标 → 目标不带参数 `/tpaccept` 应提示多份请求
3. 中文/英文文案显示正常（`/help` 与各类错误提示）

## 许可

MIT，见 [LICENSE](LICENSE)。
