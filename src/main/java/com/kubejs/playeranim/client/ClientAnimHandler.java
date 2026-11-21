package com.kubejs.playeranim.client;

import com.kubejs.playeranim.KubeJSPlayerAnimConfig;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端动画处理器
 * <p>
 * 职责:
 * 1. 在 clientInit 阶段为每个 AbstractClientPlayer 注册 ModifierLayer 动画层
 * 2. 收到 S2C 数据包后, 查找目标玩家的动画层并播放/停止动画
 * <p>
 * 动画层 ID: kubejs_playeranim:main
 * 动画资源路径: assets/{namespace}/player_animation/{name}.json
 */
@OnlyIn(Dist.CLIENT)
public class ClientAnimHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("KubeJSPlayerAnim");

    public static final ResourceLocation LAYER_ID = new ResourceLocation("kubejs_playeranim", "main");
    public static final ResourceLocation LOCOMOTION_LAYER_ID = new ResourceLocation("kubejs_playeranim", "locomotion");
    public static final String DEFAULT_NAMESPACE = "kubejs_playeranim";

    /** 每个玩家的移动替换层, 供客户端 tick 驱动 */
    private static final Map<UUID, LocomotionLayer> LOCOMOTION_LAYERS = new ConcurrentHashMap<>();

    /**
     * 注册动画层, 由 KubeJSPlayerAnimPlugin.clientInit() 调用
     * 对每个 AbstractClientPlayer 创建一个 ModifierLayer 并存入 playerAssociatedData
     */
    public static void registerAnimationLayer() {
        // 优先级可配置, 默认 2000 (config/kubejs_playeranim-common.toml)
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, KubeJSPlayerAnimConfig.ANIMATION_PRIORITY.get(), player -> {
            // 对所有客户端玩家都创建动画层 (本地玩家 + 其他玩家)
            return new FirstPersonModifierLayer();
        });

        // 移动替换层: 优先级略低于一次性动画, 这样 play() 播动作时仍然能盖住待机/走路
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LOCOMOTION_LAYER_ID,
                KubeJSPlayerAnimConfig.LOCOMOTION_PRIORITY.get(), player -> {
                    LocomotionLayer layer = new LocomotionLayer(player);
                    LOCOMOTION_LAYERS.put(player.getUUID(), layer);
                    return layer;
                });
    }

    /**
     * 客户端 tick: 推进每个玩家的移动状态采样并刷新替换动画
     */
    public static void tickLocomotion() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            LOCOMOTION_LAYERS.clear();
            return;
        }
        LOCOMOTION_LAYERS.entrySet().removeIf(entry -> mc.level.getPlayerByUUID(entry.getKey()) == null);
        for (LocomotionLayer layer : LOCOMOTION_LAYERS.values()) {
            layer.update();
        }
    }

    /**
     * 处理 S2C 数据包, 播放或停止动画
     * 由 PlayAnimMessage.handle 在客户端主线程调用
     *
     * @param playerUUID 目标玩家 UUID
     * @param animationId 动画ID ("namespace:path" 或 "path"), stop=true 时忽略
     * @param fadeTicks  淡入/淡出帧数
     * @param easeName   缓动函数名
     * @param stop       true=停止当前动画, false=播放新动画
     */
    public static void handlePlayAnim(UUID playerUUID, String animationId, int fadeTicks, String easeName, boolean stop) {
        handlePlayAnim(playerUUID, animationId, fadeTicks, easeName, stop, null, null, null, null, null);
    }

    /**
     * 处理 S2C 数据包, 播放或停止动画, 并应用第一人称参数
     *
     * @param firstPersonMode      第一人称模式名, null 表示由动画 JSON / 配置决定
     * @param firstPersonRightArm  是否显示右臂, null 表示自动推断
     * @param firstPersonLeftArm   是否显示左臂, null 表示自动推断
     * @param firstPersonRightItem 是否显示右手物品, null 表示显示
     * @param firstPersonLeftItem  是否显示左手物品, null 表示显示
     */
    public static void handlePlayAnim(UUID playerUUID, String animationId, int fadeTicks, String easeName, boolean stop,
                                      String firstPersonMode, Boolean firstPersonRightArm, Boolean firstPersonLeftArm,
                                      Boolean firstPersonRightItem, Boolean firstPersonLeftItem) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var entity = mc.level.getPlayerByUUID(playerUUID);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        // 获取该玩家的动画层
        var rawData = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
        if (!(rawData instanceof FirstPersonModifierLayer animLayer)) return;

        Ease ease = parseEase(easeName);

        if (stop) {
            // 第一人称: 淡出期间沿用旧动画的模式, 否则相机会立刻跳回原版手部
            animLayer.holdFirstPerson(animLayer.getFirstPersonMode(0f),
                    animLayer.getFirstPersonConfiguration(0f), fadeTicks);
            // 停止: fade 到 null (无动画状态)
            animLayer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fadeTicks, ease),
                    null
            );
            return;
        }

        // 播放: 查找动画资源
        ResourceLocation animRl = parseAnimationId(animationId);
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animRl);
        if (anim == null) {
            LOGGER.error("Animation not found: {}", animRl);
            return;
        }

        KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(anim);
        FirstPersonSupport.apply(animPlayer, anim, firstPersonMode, firstPersonRightArm, firstPersonLeftArm,
                firstPersonRightItem, firstPersonLeftItem);

        animLayer.clearFirstPersonHold();
        animLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(fadeTicks, ease),
                animPlayer
        );
    }

    /**
     * 将字符串动画ID解析为 ResourceLocation
     * "wave" → kubejs_playeranim:wave
     * "mymod:wave" → mymod:wave
     */
    private static ResourceLocation parseAnimationId(String animationId) {
        int idx = animationId.indexOf(':');
        if (idx < 0) {
            return new ResourceLocation(DEFAULT_NAMESPACE, animationId);
        }
        return new ResourceLocation(animationId);
    }

    /**
     * 将缓动函数名解析为 Ease 枚举, 不匹配时返回 LINEAR
     */
    private static Ease parseEase(String easeName) {
        if (easeName == null || easeName.isBlank()) return Ease.LINEAR;
        try {
            return Ease.valueOf(easeName.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Ease.LINEAR;
        }
    }
}
