package com.kubejs.playeranim.client;

import com.kubejs.playeranim.KubeJSPlayerAnimConfig;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
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

import java.util.UUID;

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
    public static final String DEFAULT_NAMESPACE = "kubejs_playeranim";

    /**
     * 注册动画层, 由 KubeJSPlayerAnimPlugin.clientInit() 调用
     * 对每个 AbstractClientPlayer 创建一个 ModifierLayer 并存入 playerAssociatedData
     */
    public static void registerAnimationLayer() {
        // 优先级可配置, 默认 2000 (config/kubejs_playeranim-common.toml)
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, KubeJSPlayerAnimConfig.ANIMATION_PRIORITY.get(), player -> {
            // 对所有客户端玩家都创建动画层 (本地玩家 + 其他玩家)
            return new ModifierLayer<IAnimation>();
        });
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
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var entity = mc.level.getPlayerByUUID(playerUUID);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        // 获取该玩家的动画层
        var rawData = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
        if (!(rawData instanceof ModifierLayer<?> layer)) return;

        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> animLayer = (ModifierLayer<IAnimation>) layer;

        Ease ease = parseEase(easeName);

        if (stop) {
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

        animLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(fadeTicks, ease),
                new KeyframeAnimationPlayer(anim)
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
