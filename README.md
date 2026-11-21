# KubeJS Player Animator

KubeJS 与 [playerAnimator](https://github.com/KosmX/minecraftPlayerAnimator) 之间的桥接模组。
允许服务端 KubeJS 脚本通过 S2C 网络同步触发客户端玩家自定义动画,实现多人可见的动画播放。本mod由AI编写。

## 核心特性

- **服务端脚本驱动**:在 `server_scripts` 中调用 `PlayerAnim.play()` / `PlayerAnim.stop()` 即可播放动画
- **多人同步**:通过 `TRACKING_ENTITY_AND_SELF` 广播,所有能看到目标玩家的客户端都会播放动画
- **事件拦截**:提供 `PlayerAnimEvents.play` 事件,允许脚本修改动画参数或取消播放
- **淡入淡出**:支持自定义淡入帧数与缓动函数(LINEAR、EASEINOUTQUAD 等)
- **任意玩家**:动画层对所有 `AbstractClientPlayer`(本地玩家 + 其他玩家)生效
- **优先级可配置**:动画层注册优先级默认 `2000`,可通过配置文件调整

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
| [KubeJSPlayerAnimMod](src/main/java/com/kubejs/playeranim/KubeJSPlayerAnimMod.java) | 主类,注册 SimpleChannel、数据包与配置 |
| [KubeJSPlayerAnimConfig](src/main/java/com/kubejs/playeranim/KubeJSPlayerAnimConfig.java) | Forge 配置定义(animationPriority) |
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

### 第一人称:`PlayerAnim.firstPerson()`

playerAnimator **默认不会把动画渲染到第一人称**,`KeyframeAnimationPlayer` 的第一人称模式初始值是 `FirstPersonMode.NONE`,
第一人称渲染通道会直接跳过该层。所以只调用 `PlayerAnim.play()` 时,第三人称能看到动画,自己视角看不到——这不是 bug。

要用第一人称显示,必须显式指定 `FirstPersonMode.THIRD_PERSON_MODEL`:playerAnimator 会把相机临时当作第三人称,
渲染玩家模型(默认只保留手臂和手持物),同时屏蔽原版第一人称手部渲染。

```js
const fp = PlayerAnim.firstPerson()
fp.mode = "THIRD_PERSON_MODEL"   // 关键
fp.rightArm = true               // 显示右臂
fp.leftArm = false
PlayerAnim.play(player, "wave", 10, "EASEINOUTQUAD", fp)
```

`FirstPersonOptions` 字段:

| 字段 | 类型 | 说明 |
|------|------|------|
| `mode` | `string` | `THIRD_PERSON_MODEL`(推荐)、`VANILLA`、`NONE`、`DISABLED`、`AUTO` |
| `rightArm` / `leftArm` | `boolean` | 是否显示该侧手臂(`THIRD_PERSON_MODEL` 下才有效) |
| `rightItem` / `leftItem` | `boolean` | 是否显示该侧手持物,留空时跟随对应手臂 |

所有字段都可留空,留空时按“动画 JSON 顶层字段 → 配置文件”的顺序回退。

### 用动画 JSON 声明(无需改脚本)

动画 JSON 顶层支持四个附加字段(playerAnimator 会把它们放进 `extraData`):

```json
{
  "name": "wave",
  "firstPersonMode": "THIRD_PERSON_MODEL",
  "showRightArm": true,
  "showLeftArm": false,
  "emote": { "...": "..." }
}
```

这样任何调用 `PlayerAnim.play(player, "wave")` 的地方都会自动带第一人称。

### 持有物品时替换原版待机/移动动作

`PlayerAnim.locomotion(id)` 注册一套"持有时接管原版动作"的配置:
待机摆动、四方向走、疾跑、潜行、跳跃、下落都会换成你的动画。

```js
PlayerAnim.locomotion("gun")
    .eitherHand("tacz:modern_kinetic_gun")   // 主手或副手
    .priority(1000)                          // 数值越大越优先
    .fadeTicks(4)                            // 切换淡入帧数
    .firstPersonMode("THIRD_PERSON_MODEL")
    .showRightArm(true)
    .idle("kubejs_playeranim:gun_idle")
    .walkForward("kubejs_playeranim:gun_walk_front")
    .walkBackward("kubejs_playeranim:gun_walk_back")
    .walkLeft("kubejs_playeranim:gun_walk_left")
    .walkRight("kubejs_playeranim:gun_walk_right")
    .run("kubejs_playeranim:gun_run")
    .sneak("kubejs_playeranim:gun_idle")
    .jump("kubejs_playeranim:gun_jump")
    .fall("kubejs_playeranim:gun_fall")
    .register()
```

**物品匹配**支持三种写法:`namespace:item`、`namespace:*`(整个命名空间)、`#namespace:tag`(标签)。
也可用 `.mainHand(...)` / `.offHand(...)` / `.always(true)`。

**状态回退规则**(没写的状态自动借用):

| 状态 | 回退到 |
|------|--------|
| `walk_front` / `walk_back` / `walk_left` / `walk_right` | `walk` |
| `run` | `walk` |
| `jump` | `fall` |
| `fall` | `jump` |
| `sneak` / `swim` | `idle` |

`run` 只在"真正向前 + 速度达到疾跑量级"时触发, 侧移和后退一律走 `walk_*`,
与原版/MechanicianCombat 的判定一致。移动状态用位置差采样, 所以站着不动时
不会因为走路动画还在循环而"原地踏步"。

**动画必须循环**: 配置默认 `loop = true`, 会自动把一次性动画包成循环;
若某段是一次性动作, 用 `.loop(false)`。

### 与 `play()` 的关系

替换层的注册优先级默认 `locomotionPriority = 1900`, 低于 `animationPriority = 2000`。
所以 `PlayerAnim.play()` 播的攻击/翻滚等一次性动画仍然盖在上面, 播完自动回到待机替换动画。

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

## 配置

配置文件位于 `config/kubejs_playeranim-common.toml`,首次启动后自动生成:

```toml
[general]
	#Animation layer registration priority (higher = higher priority)
	#Range: 0 ~ 2147483647
	animationPriority = 2000
	[general.firstPerson]
		#Default first person mode, default THIRD_PERSON_MODEL
		defaultMode = "THIRD_PERSON_MODEL"
		#If true, arm visibility is derived from the animation keyframes
		autoArmVisibility = true
	#Priority of the locomotion-replacement layer (vanilla idle/walk override).
	#Keep it lower than animationPriority so one-shot animations still win.
	#Range: 0 ~ 2147483647
	locomotionPriority = 1900
```

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `animationPriority` | `2000` | 动画层注册优先级,数值越大优先级越高。需要与其他动画模组(如 emotecraft)竞争动画层顺序时调整此项 |
| `firstPerson.defaultMode` | `THIRD_PERSON_MODEL` | 动画和脚本都没指定第一人称模式时的默认值;设为 `NONE` 可恢复“第一人称不显示”的旧行为 |
| `firstPerson.autoArmVisibility` | `true` | 未指定手臂可见性时,按动画是否包含 `rightArm`/`leftArm` 关键帧自动决定 |
| `locomotionPriority` | `1900` | 待机/移动替换层的优先级,必须低于 `animationPriority` |

注意:`defaultMode` 是全局开关。想让“部分动画第一人称可见、部分不可见”,请保持默认值,改在动画 JSON 里写
`"firstPersonMode": "NONE"` 或用脚本传 `fp.mode = "NONE"`。

修改配置后需重启游戏生效(动画层在客户端初始化阶段注册一次,不支持热重载)。

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
- **动画层注册**:通过 `PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory` 为每个 `AbstractClientPlayer` 创建 `ModifierLayer`,优先级由配置项 `animationPriority` 决定(默认 `2000`)。
- **客户端隔离**:`PlayAnimMessage.handle` 通过 `FMLEnvironment.dist` 检查确保 playerAnimator 相关代码仅在客户端执行。
- **事件取消**:使用 `EventResult.interruptFalse()` 判断脚本是否调用 `event.cancel()`,而非 `isCanceled()`(KubeJS 1.20.1 的 `EventJS` 无此方法)。

## 许可证

MIT
