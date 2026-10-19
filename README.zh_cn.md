# Oh My Teleport

[English](README.md) | 中文

**Minecraft 26.3**（Fabric）的传送命令 mod，单人与 Fabric 服务器都能用。

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## 功能

- `/back` —— 回死亡地点；用掉之后回上次传送前的位置
- `/tpa` + `/tpaccept` —— 玩家间请求式传送，60 秒过期
- `/home` + `/sethome` —— 按玩家分开存的命名家
- `/warp` —— 个人命名传送点
- 所有命令都是玩家级，**不需要 OP**；家、传送点、`/back` 记录每人一份
- 无需额外配置即可跨模组维度（天堂、暮色森林等）传送

## 命令

| 命令 | 说明 |
| --- | --- |
| `/sethome [名字]` | 在当前位置设家，省略名字时是 `home` |
| `/home [名字]` | 回家 |
| `/delhome <名字>` | 删家 |
| `/homes` | 列出自己的家 |
| `/warp set <名字>` | 在当前位置建传送点 |
| `/warp <名字>` | 去自己的传送点 |
| `/warp del <名字>` | 删传送点 |
| `/warp list`（或直接 `/warp`） | 列出自己的传送点 |
| `/tpa <玩家>` | 请求传送到该玩家处 |
| `/tpaccept [玩家]` | 接受请求 |
| `/tpadeny [玩家]` | 拒绝请求 |
| `/back` | 回死亡地点；用掉之后回上次传送前的位置 |

家和传送点按玩家分开，别人的既看不到也用不了。

## 行为说明

- 名字大小写不敏感（存为小写），最多 32 个字符，不能含空格。
- `set`、`del`、`list` 是 `/warp` 的子命令，不能拿来当传送点名字。
- 每个请求者同一时刻只有一份待处理 `/tpa`；再发一次会覆盖旧的（旧目标会收到提示）。不同的人发给同一目标的请求并存。请求 60 秒过期。
- 目标同时有多份待处理请求时，`/tpaccept` 不带参数会列出发送者并要求写名字。
- `/back` 的死亡地点用一次就失效，第二次 `/back` 回上次传送前的位置。死亡地点用原版数据，死后掉线重登依然有效。
- 每次传送都会报坐标，只有**跨维度**时才显示维度名。
- 只有本 mod 的传送会更新「上次传送前的位置」；原版 `/tp` 或别的 mod 不会被记录。

## 模组维度

家与传送点存的是维度 id 字符串，解析方式与原版维度完全相同，所以自定义维度开箱即用。维度不存在或未加载时报「目标维度未加载」，不传送、不崩。

原版没有模组维度的显示名，因此默认显示完整 id（如 `aether:the_aether`）。整合包作者可以用资源包给任意维度起名，不用改代码：

```json
{
  "oh_my_teleport.msg.dimension.twilightforest.twilight_forest": "暮色森林",
  "oh_my_teleport.msg.dimension.aether.the_aether": "天堂"
}
```

键名规则：`oh_my_teleport.msg.dimension.` + 维度 id（冒号换成点）。

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/) 0.20.0 或更新版本。
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 [Releases](../../releases) 里的 jar 丢进 `mods/`。

单人要开作弊才能用命令。`fabric.mod.json` 的 `environment` 是 `"*"`——故意如此，因为 `"server"` 的 mod 单人游戏不会加载。

## 构建

需要 **JDK 25**，Gradle daemon 自身也必须跑在 25 上。

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

产物在 `build/libs/oh_my_teleport-fabric-mc26.3-v1.0.0.jar`。`-sources.jar` 只在调试时用，不要丢进 `mods/`。

测试：

```bash
./gradlew test         # 单元测试
./gradlew runGameTest  # 无头游戏内测试
```

## 许可证

[MIT](LICENSE)
