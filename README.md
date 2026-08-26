# KubeJS Player Animator

KubeJS 与 [playerAnimator](https://github.com/KosmX/minecraftPlayerAnimator) 之间的桥接模组。
允许服务端 KubeJS 脚本通过 S2C 网络同步触发客户端玩家自定义动画,实现多人可见的动画播放。本mod由AI编写。

## 核心特性

- **服务端脚本驱动**:在 `server_scripts` 中调用 `PlayerAnim.play()` / `PlayerAnim.stop()` 即可播放动画
- **多人同步**:通过 `TRACKING_ENTITY_AND_SELF` 广播,所有能看到目标玩家的客户端都会播放动画
- **事件拦截**:提供 `PlayerAnimEvents.play` 事件,允许脚本修改动画参数或取消播放
- **淡入淡出**:支持自定义淡入帧数与缓动函数(LINEAR、EASEINOUTQUAD 等)
- **任意玩家**:动画层对所有 `AbstractClientPlayer`(本地玩家 + 其他玩家)生效

## 依赖

| 依赖 | 版本范围 |
|------|----------|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| KubeJS | 2001.1.2+ |
| playerAnimator | 1.0.0+ |
| Rhino | (KubeJS 传递依赖) |
| Architectury | (KubeJS 传递依赖) |

## 架构

```
server_scripts                    服务端客户端
     │                               │
     │ PlayerAnim.play(player, ...)  │
     ▼                               │
PlayerAnimBinding                   │
     │                               │
     │ 触发 PlayerAnimEvents.play    │
     ▼ (脚本可修改/取消)              │
PlayAnimMessage (S2C) ──────────────▶ ClientAnimHandler
                                     │
                                     │ 查找 ModifierLayer
                                     │ PlayerAnimationRegistry 查找动画
                                     ▼
                                  播放 KeyframeAnimationPlayer
                                     + AbstractFadeModifier
```

### 关键类

| 类 | 职责 |
|----|------|
| [KubeJSPlayerAnimMod](src/main/java/com/kubejs/playeranim/KubeJSPlayerAnimMod.java) | 主类,注册 SimpleChannel 与数据包 |
| [KubeJSPlayerAnimPlugin](src/main/java/com/kubejs/playeranim/KubeJSPlayerAnimPlugin.java) | KubeJS 插件,注册事件、绑定对象、客户端动画层 |
| [PlayerAnimBinding](src/main/java/com/kubejs/playeranim/script/PlayerAnimBinding.java) | 暴露 `PlayerAnim` 全局对象到脚本 |
| [PlayAnimMessage](src/main/java/com/kubejs/playeranim/net/PlayAnimMessage.java) | S2C 数据包(UUID / 动画ID / 淡入 / 缓动 / stop) |
| [ClientAnimHandler](src/main/java/com/kubejs/playeranim/client/ClientAnimHandler.java) | 客户端动画层注册与播放处理 |
| [PlayerAnimEvents](src/main/java/com/kubejs/playeranim/event/PlayerAnimEvents.java) | 事件组定义 |
| [PlayAnimEventJS](src/main/java/com/kubejs/playeranim/event/PlayAnimEventJS.java) | 事件对象,字段可修改 |

## KubeJS 脚本 API

### `PlayerAnim.play(player, animationId [, fadeTicks [, ease]])`

为指定玩家播放动画。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家(必须是 `ServerPlayer`) |
| `animationId` | `string` | 动画ID,格式 `namespace:path`。无命名空间时默认 `kubejs_playeranim` |
| `fadeTicks` | `int` | 可选,淡入帧数,默认 `20` |
| `ease` | `string` | 可选,缓动函数名,默认 `LINEAR` |

### `PlayerAnim.stop(player [, fadeTicks [, ease]])`

为指定玩家停止当前动画(带淡出)。

### `PlayerAnimEvents.play(event => { ... })`

每次调用 `PlayerAnim.play()` 时触发的服务端事件,可读取/修改参数或取消播放。

| 事件属性 | 可读 | 可写 | 说明 |
|----------|------|------|------|
| `event.player` | 是 | 否 | 目标玩家 |
| `event.animation` | 是 | 是 | 动画ID |
| `event.fadeTicks` | 是 | 是 | 淡入帧数 |
| `event.ease` | 是 | 是 | 缓动函数名 |

调用 `event.cancel()` 可取消本次播放。

## 使用示例

### 1. 命令触发

```js
// server_scripts/play_anim.js
ServerEvents.commandRegistry(event => {
    const { commands: Commands, arguments: Arguments } = event
    event.register(
        Commands.literal("playanim")
            .requires(src => src.hasPermission(2))
            .then(Commands.argument("animation", Arguments.STRING.create(event))
                .executes(ctx => {
                    const player = ctx.getSource().getPlayerOrException()
                    const animId = Arguments.STRING.getResult(ctx, "animation")
                    PlayerAnim.play(player, animId)
                    return 1
                })
            )
    )
    event.register(
        Commands.literal("stopanim")
            .requires(src => src.hasPermission(2))
            .executes(ctx => {
                PlayerAnim.stop(ctx.getSource().getPlayerOrException())
                return 1
            })
    )
})
```

游戏内执行:`/playanim wave` 或 `/stopanim`

### 2. 事件监听

```js
PlayerAnimEvents.play(event => {
    console.log(`[PlayerAnim] Playing "${event.animation}" on ${event.player.username}`)
    // 修改淡入帧数
    event.fadeTicks = 10
    event.ease = "EASEINOUTQUAD"
    // 取消特定动画
    // if (event.animation === "blocked") event.cancel()
})
```

### 3. 右键空气挥手

```js
PlayerEvents.interact(event => {
    if (event.hand !== 'main_hand') return
    if (event.item.isEmpty()) {
        PlayerAnim.play(event.player, "wave", 10, "EASEINOUTQUAD")
    }
})
```

## 动画资源

动画 JSON 放置于资源包的 `assets/<namespace>/player_animation/<name>.json`,其中 `name` 字段即为动画ID 的 path 部分。

示例动画见 [assets/kubejs_playeranim/player_animation/wave.json](src/main/resources/assets/kubejs_playeranim/player_animation/wave.json)。

动画文件格式遵循 playerAnimator 规范,关键字段:

```json
{
    "name": "wave",
    "emote": {
        "beginTick": 0,
        "endTick": 30,
        "stopTick": 40,
        "degrees": false,
        "moves": [
            { "tick": 0, "rightArm": { "pitch": 0 } },
            { "tick": 10, "rightArm": { "pitch": -2.5, "roll": 0.4 } }
        ]
    }
}
```

## 缓动函数

`ease` 参数接受以下枚举名(大小写不敏感):

`LINEAR`、`EASEINQUAD`、`EASEOUTQUAD`、`EASEINOUTQUAD`、`EASEINCUBIC`、`EASEOUTCUBIC`、`EASEINOUTCUBIC`、`EASEINELASTIC`、`EASEOUTELASTIC`、`EASEINOUTELASTIC`、`EASEINBOUNCE`、`EASEOUTBOUNCE`、`EASEINOUTBOUNCE` 等。

不匹配时回退为 `LINEAR`。

## 构建

```bash
./gradlew build
```

产物位于 `build/libs/kubejs_playeranim-<version>.jar`。

## 技术说明

- **网络通道注册**:`SimpleChannel` 必须在 mod 加载阶段(mod 构造函数)注册,数据包在 `FMLCommonSetupEvent` 中通过 `enqueueWork` 注册,避免 "Registration of impl channels is locked" 错误。
- **动画层注册**:通过 `PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory` 为每个 `AbstractClientPlayer` 创建 `ModifierLayer`,优先级 42。
- **客户端隔离**:`PlayAnimMessage.handle` 通过 `FMLEnvironment.dist` 检查确保 playerAnimator 相关代码仅在客户端执行。
- **事件取消**:使用 `EventResult.interruptFalse()` 判断脚本是否调用 `event.cancel()`,而非 `isCanceled()`(KubeJS 1.20.1 的 `EventJS` 无此方法)。

## 许可证

MIT
