package com.kubejs.playeranim.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.jetbrains.annotations.NotNull;

/**
 * 把一次性动画包装成循环动画
 * <p>
 * playerAnimator 的 KeyframeAnimationPlayer 播到 stopTick 就会 stop(), 而待机/走路这类
 * 动画必须一直循环。这里在内层播放器停下时立刻从头重建, 并始终对外报告 active,
 * 保证 AnimationStack 会继续 tick 这一层。
 */
public class LoopingAnimationPlayer implements IAnimation {

    private final KeyframeAnimation animation;
    private final FirstPersonMode firstPersonMode;
    private final FirstPersonConfiguration firstPersonConfiguration;

    private KeyframeAnimationPlayer inner;
    private boolean enabled = true;
    private boolean loop;

    public LoopingAnimationPlayer(KeyframeAnimation animation, FirstPersonMode mode,
                                  FirstPersonConfiguration configuration, boolean loop) {
        this.animation = animation;
        this.firstPersonMode = mode;
        this.firstPersonConfiguration = configuration;
        this.loop = loop;
        this.inner = create();
    }

    private KeyframeAnimationPlayer create() {
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(animation);
        player.setFirstPersonMode(firstPersonMode);
        player.setFirstPersonConfiguration(firstPersonConfiguration);
        return player;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @Override
    public boolean isActive() {
        // 非循环动画播完会把自己关掉, 此时必须对外 inactive, 否则动画层会一直占位
        return enabled && (loop || inner.isActive());
    }

    @Override
    public void tick() {
        if (!enabled) return;
        if (!inner.isActive()) {
            if (loop) {
                inner = create();
            } else {
                // 非循环动画播完就停, 让调用方切回原版
                enabled = false;
            }
            return;
        }
        inner.tick();
    }

    @Override
    public @NotNull Vec3f get3DTransform(@NotNull String modelName, @NotNull TransformType type,
                                         float tickDelta, @NotNull Vec3f value0) {
        return inner.get3DTransform(modelName, type, tickDelta, value0);
    }

    @Override
    public void setupAnim(float tickDelta) {
        inner.setupAnim(tickDelta);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        return firstPersonMode;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return firstPersonConfiguration;
    }
}
