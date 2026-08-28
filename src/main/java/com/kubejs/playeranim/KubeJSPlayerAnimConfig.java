package com.kubejs.playeranim;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * mod 配置
 * <p>
 * 配置文件位置: config/kubejs_playeranim-common.toml
 */
public class KubeJSPlayerAnimConfig {

    public static final ForgeConfigSpec SPEC;

    /** 动画层注册优先级, 数值越大优先级越高 */
    public static final ForgeConfigSpec.IntValue ANIMATION_PRIORITY;

    static {
        var builder = new ForgeConfigSpec.Builder();
        ANIMATION_PRIORITY = builder
                .comment("Animation layer registration priority (higher = higher priority)")
                .defineInRange("animationPriority", 2000, 0, Integer.MAX_VALUE);
        SPEC = builder.build();
    }
}
