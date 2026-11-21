// server_scripts/play_anim_demo.js
// KubeJS Player Animator 演示脚本
//
// 前置条件:
//   - kubejs_playeranim mod 已安装
//   - playeranimator mod 已安装
//   - 动画文件位于 assets/kubejs_playeranim/player_animation/wave.json
//
// 动画ID格式: "namespace:path"
//   - "wave"           → 自动补全为 "kubejs_playeranim:wave"
//   - "kubejs_playeranim:wave" → 完整写法
//   - "mymod:wave"     → 引用 mymod 的动画

// ============================================
// 示例1: 注册命令 /playanim <animationId>
// ============================================
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
                const player = ctx.getSource().getPlayerOrException()
                PlayerAnim.stop(player)
                return 1
            })
    )
})

// ============================================
// 示例2: 监听 PlayerAnimEvents.play 事件
// ============================================
PlayerAnimEvents.play(event => {
    console.log(`[PlayerAnim] Playing "${event.animation}" on ${event.player.username} (fade=${event.fadeTicks}, ease=${event.ease})`)

    // 示例: 可以在此修改参数
    // event.fadeTicks = 10
    // event.ease = "EASEINOUTQUAD"

    // 示例: 可以取消特定动画
    // if (event.animation === "blocked") {
    //     event.cancel()
    // }
})

// ============================================
// 示例3: 右键空气时播放挥手动画
// ============================================
PlayerEvents.interact(event => {
    if (event.hand !== 'main_hand') return
    if (event.item.isEmpty()) {
        // 第一人称也显示: 用第三人称模型渲染手臂/物品
        const fp = PlayerAnim.firstPerson()
        fp.mode = "THIRD_PERSON_MODEL"
        fp.rightArm = true      // 挥手只动右手
        fp.leftArm = false
        PlayerAnim.play(event.player, "wave", 10, "EASEINOUTQUAD", fp)
    }
})

// ============================================
// 示例4: 命令触发第一人称可见动画
//   /playanim_fp wave
// ============================================
ServerEvents.commandRegistry(event => {
    const { commands: Commands, arguments: Arguments } = event
    event.register(
        Commands.literal("playanim_fp")
            .requires(src => src.hasPermission(2))
            .then(Commands.argument("animation", Arguments.STRING.create(event))
                .executes(ctx => {
                    const player = ctx.getSource().getPlayerOrException()
                    const animId = Arguments.STRING.getResult(ctx, "animation")
                    const fp = PlayerAnim.firstPerson()
                    fp.mode = "THIRD_PERSON_MODEL"
                    PlayerAnim.play(player, animId, 10, "EASEINOUTQUAD", fp)
                    return 1
                })
            )
    )
})
