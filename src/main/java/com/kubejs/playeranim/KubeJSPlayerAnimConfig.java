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

    /** 移动替换层的注册优先级, 必须低于 ANIMATION_PRIORITY 才能被 play() 动画覆盖 */
    public static final ForgeConfigSpec.IntValue LOCOMOTION_PRIORITY;

    /** 动画未声明第一人称模式时使用的默认模式 */
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_FIRST_PERSON_MODE;

    /** 动画未声明手臂可见性时, 是否按动画关键帧自动推断 */
    public static final ForgeConfigSpec.BooleanValue AUTO_ARM_VISIBILITY;

    static {
        var builder = new ForgeConfigSpec.Builder();
        ANIMATION_PRIORITY = builder
                .comment("Animation layer registration priority (higher = higher priority)")
                .defineInRange("animationPriority", 2000, 0, Integer.MAX_VALUE);

        LOCOMOTION_PRIORITY = builder
                .comment("Priority of the locomotion-replacement layer (vanilla idle/walk override).",
                        "Keep it lower than animationPriority so one-shot animations still win.")
                .defineInRange("locomotionPriority", 1900, 0, Integer.MAX_VALUE);

        builder.comment("First person settings",
                "playerAnimator only renders an animation in first person if the animation",
                "explicitly declares a FirstPersonMode. This value is used when neither the",
                "script options nor the animation JSON declare one.",
                "Valid values: AUTO, NONE, VANILLA, THIRD_PERSON_MODEL, DISABLED",
                "AUTO is treated as THIRD_PERSON_MODEL (render the player model arms/items).")
                .push("firstPerson");
        DEFAULT_FIRST_PERSON_MODE = builder
                .comment("Default first person mode, default THIRD_PERSON_MODEL")
                .define("defaultMode", "THIRD_PERSON_MODEL");
        AUTO_ARM_VISIBILITY = builder
                .comment("If true, arm visibility is derived from the animation keyframes",
                        "when the animation does not declare it. Disable to keep both arms hidden.")
                .define("autoArmVisibility", true);
        builder.pop();
        SPEC = builder.build();
    }
}
