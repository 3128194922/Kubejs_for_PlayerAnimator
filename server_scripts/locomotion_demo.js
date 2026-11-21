// server_scripts/locomotion_demo.js
// 持有物品时替换原版待机 / 移动动作
//
// 动画资源放在资源包: assets/<namespace>/player_animation/<name>.json
// 这些动画会被循环播放, 用来覆盖原版的待机摆动和走路摆腿。

// ============================================
// 示例1: 手持某把枪时, 整套待机/移动动画
// ============================================
PlayerAnim.locomotion("demo_gun")
    // 主手或副手持有时生效; 支持 namespace:item / namespace:* / #namespace:tag
    .eitherHand("tacz:modern_kinetic_gun", "mechaniciancombat:*")
    .priority(1000)
    .fadeTicks(4)
    .firstPersonMode("THIRD_PERSON_MODEL")
    .showRightArm(true)
    .showLeftArm(false)
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

// ============================================
// 示例2: 四方向没做完时, 只写 idle + walk 兜底
//   walk 会作为 walk_front/back/left/right 的回退
// ============================================
PlayerAnim.locomotion("demo_melee")
    .mainHand("minecraft:iron_sword", "minecraft:diamond_sword")
    .idle("kubejs_playeranim:sword_idle")
    .walk("kubejs_playeranim:sword_walk")
    .register()

// ============================================
// 示例3: 运行时按玩家切换 (例如某个技能期间)
//   同一 id 重复 register 会覆盖旧配置
// ============================================
ServerEvents.tick(event => {
    // 这里只做演示: 每 10 秒在两种姿态之间切换
    if (event.server.tickCount % 200 !== 0) return
    const profile = PlayerAnim.locomotion("demo_gun")
        .eitherHand("tacz:modern_kinetic_gun", "mechaniciancombat:*")
        .idle("kubejs_playeranim:gun_idle")
        .walkForward("kubejs_playeranim:gun_walk_front")
        .register()
})
