package com.kubejs.playeranim.client;

import com.kubejs.playeranim.locomotion.LocomotionProfile;
import com.kubejs.playeranim.locomotion.LocomotionRegistry;
import com.kubejs.playeranim.locomotion.LocomotionState;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 持有物品时替换原版待机 / 移动动作的动画层
 * <p>
 * 原理: playerAnimator 的动画层按优先级叠加, 高优先级的层后写入同一个 ModelPart,
 * 所以这里持续输出一段循环动画就能覆盖原版的待机摆动 / 走路摆腿。
 * <p>
 * 本层由客户端 tick 主动驱动({@link #update()}), 不依赖 AnimationStack 的 active 判定 ——
 * 否则"没有配置"时层不会被 tick, 永远无法重新激活。没有配置时不写任何骨骼。
 */
public class LocomotionLayer extends ModifierLayer<IAnimation> {

    private static final Logger LOGGER = LoggerFactory.getLogger("KubeJSPlayerAnim");
    private static final int CLEAR_FADE = 4;

    private final AbstractClientPlayer player;
    private final LocomotionResolver resolver;

    private String currentProfileId;
    private LocomotionState currentState;
    private ResourceLocation currentAnimation;
    private LoopingAnimationPlayer looping;
    private boolean active;

    public LocomotionLayer(AbstractClientPlayer player) {
        this.player = player;
        this.resolver = new LocomotionResolver(player);
    }

    /**
     * 每客户端 tick 调用: 采样位置差 + 刷新动画
     */
    public void update() {
        resolver.tick();

        LocomotionState state = resolver.resolve();
        LocomotionProfile profile = LocomotionResolver.select(LocomotionRegistry.all(), player, state);
        if (profile == null) {
            deactivate();
            return;
        }

        ResourceLocation animationId = profile.animationResource(state);
        if (animationId == null) {
            deactivate();
            return;
        }

        boolean unchanged = profile.id().equals(currentProfileId)
                && state == currentState
                && animationId.equals(currentAnimation);
        if (unchanged) {
            // 动画推进交给 AnimationStack 的 tick, 这里只负责状态判定与切换
            return;
        }

        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null) {
            LOGGER.warn("Locomotion animation not found: {} (profile {}, state {})",
                    animationId, profile.id(), state);
            deactivate();
            return;
        }

        // null 表示"未指定", 由 FirstPersonSupport 按动画 JSON / 配置回退
        FirstPersonSettings settings = FirstPersonSupport.resolve(animation, profile.firstPersonMode(),
                profile.showRightArm(), profile.showLeftArm(),
                profile.showRightItem(), profile.showLeftItem());

        LoopingAnimationPlayer next = new LoopingAnimationPlayer(animation, settings.mode(),
                settings.configuration(), profile.loop());
        int fade = profile.fadeTicks();
        if (fade > 0 && active && looping != null) {
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fade, Ease.INOUTQUAD), next);
        } else {
            setAnimation(next);
        }
        looping = next;
        active = true;

        currentProfileId = profile.id();
        currentState = state;
        currentAnimation = animationId;
    }

    private void deactivate() {
        if (!active) return;
        active = false;
        looping = null;
        currentProfileId = null;
        currentState = null;
        currentAnimation = null;
        replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(CLEAR_FADE, Ease.INOUTQUAD), null);
    }

    /**
     * 只在真正有动画时才参与叠加 (淡出期间 modifier 仍 active, 会自然播完)
     * <p>
     * 注意: 本层的 update() 由客户端 tick 主动调用, 不依赖这里返回 true,
     * 所以没有配置时也能在之后重新激活。
     */
    @Override
    public boolean isActive() {
        return super.isActive();
    }

    public String debugInfo() {
        return currentProfileId + " / " + currentState + " / " + currentAnimation;
    }
}
