package com.kubejs.playeranim.client;

import com.kubejs.playeranim.KubeJSPlayerAnimConfig;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 第一人称配置的统一入口
 * <p>
 * playerAnimator 的动画默认第一人称不可见, 必须显式设 FirstPersonMode。
 * 优先级: 调用方传入值 &gt; 动画 JSON 顶层字段 &gt; 配置文件。
 */
public final class FirstPersonSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("KubeJSPlayerAnim");

    private FirstPersonSupport() {
    }

    public static void apply(KeyframeAnimationPlayer animPlayer, KeyframeAnimation anim,
                             String modeOverride, Boolean rightArmOverride, Boolean leftArmOverride,
                             Boolean rightItemOverride, Boolean leftItemOverride) {
        FirstPersonSettings settings = resolve(anim, modeOverride, rightArmOverride, leftArmOverride,
                rightItemOverride, leftItemOverride);
        animPlayer.setFirstPersonMode(settings.mode());
        animPlayer.setFirstPersonConfiguration(settings.configuration());
    }

    /**
     * 解析出最终的第一人称模式与可见性, null 表示沿用动画 JSON / 配置
     */
    public static FirstPersonSettings resolve(KeyframeAnimation anim,
                                              String modeOverride, Boolean rightArmOverride,
                                              Boolean leftArmOverride, Boolean rightItemOverride,
                                              Boolean leftItemOverride) {
        String modeName = modeOverride;
        if (isBlank(modeName)) {
            modeName = readString(anim, "firstPersonMode", "firstPerson");
        }
        if (isBlank(modeName)) {
            modeName = KubeJSPlayerAnimConfig.DEFAULT_FIRST_PERSON_MODE.get();
        }
        FirstPersonMode mode = parseMode(modeName);

        boolean autoArm = KubeJSPlayerAnimConfig.AUTO_ARM_VISIBILITY.get();
        boolean rightAnimated = hasPart(anim, "rightArm");
        boolean leftAnimated = hasPart(anim, "leftArm");
        // 动画只动身体/躯干时保留两条手臂, 否则第一人称会完全看不到手
        boolean fallbackArm = autoArm && !rightAnimated && !leftAnimated;
        boolean rightArm = resolve(rightArmOverride, anim, "showRightArm",
                autoArm && (rightAnimated || fallbackArm));
        boolean leftArm = resolve(leftArmOverride, anim, "showLeftArm",
                autoArm && (leftAnimated || fallbackArm));
        // 物品默认跟随手臂, 避免"看不见手却浮着物品"
        boolean rightItem = resolve(rightItemOverride, anim, "showRightItem", rightArm);
        boolean leftItem = resolve(leftItemOverride, anim, "showLeftItem", leftArm);

        FirstPersonConfiguration configuration = new FirstPersonConfiguration()
                .setShowRightArm(rightArm)
                .setShowLeftArm(leftArm)
                .setShowRightItem(rightItem)
                .setShowLeftItem(leftItem);
        return new FirstPersonSettings(mode, configuration);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean resolve(Boolean override, KeyframeAnimation anim, String key, boolean fallback) {
        if (override != null) return override;
        Object value = anim.extraData.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return fallback;
    }

    private static String readString(KeyframeAnimation anim, String... keys) {
        for (String key : keys) {
            Object value = anim.extraData.get(key);
            if (value != null) return value.toString();
        }
        return null;
    }

    private static boolean hasPart(KeyframeAnimation anim, String partName) {
        var part = anim.getPart(partName);
        return part != null && part.isEnabled();
    }

    /**
     * AUTO / 空值回退为 THIRD_PERSON_MODEL, 非法值回退为 NONE
     */
    public static FirstPersonMode parseMode(String modeName) {
        if (isBlank(modeName)) return FirstPersonMode.THIRD_PERSON_MODEL;
        String normalized = modeName.trim().toUpperCase();
        if (normalized.equals("AUTO")) return FirstPersonMode.THIRD_PERSON_MODEL;
        try {
            return FirstPersonMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown first person mode '{}', falling back to NONE", modeName);
            return FirstPersonMode.NONE;
        }
    }
}
