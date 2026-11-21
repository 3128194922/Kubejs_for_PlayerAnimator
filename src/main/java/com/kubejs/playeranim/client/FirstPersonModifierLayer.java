package com.kubejs.playeranim.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import org.jetbrains.annotations.NotNull;

/**
 * 带"第一人称保持"的动画层
 * <p>
 * playerAnimator 的淡出是把 animation 置为 null, 此时第一人称模式会立刻变回 NONE,
 * 相机瞬间切回原版视角, 淡出在第一人称完全看不到。这里在淡出期间继续沿用上一次的
 * 第一人称模式与可见性配置, 让退出同样是平滑的。
 */
public class FirstPersonModifierLayer extends ModifierLayer<IAnimation> {

    private FirstPersonMode holdMode;
    private FirstPersonConfiguration holdConfig;
    private int holdTicks;

    public void holdFirstPerson(FirstPersonMode mode, FirstPersonConfiguration config, int ticks) {
        if (mode == null || mode == FirstPersonMode.NONE || ticks <= 0) {
            return;
        }
        this.holdMode = mode;
        this.holdConfig = config;
        this.holdTicks = ticks;
    }

    public void clearFirstPersonHold() {
        this.holdTicks = 0;
        this.holdMode = null;
        this.holdConfig = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (holdTicks > 0 && --holdTicks == 0) {
            holdMode = null;
            holdConfig = null;
        }
    }

    /**
     * 只在淡出进行中保持: 淡出结束后 isActive() 变 false, 自动交还原版第一人称,
     * 避免动画层停 tick 后一直卡在第三人称模型上。
     */
    private boolean isHolding() {
        return holdMode != null && getAnimation() == null && super.isActive();
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        if (isHolding()) {
            return holdMode;
        }
        return super.getFirstPersonMode(tickDelta);
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        if (isHolding() && holdConfig != null) {
            return holdConfig;
        }
        return super.getFirstPersonConfiguration(tickDelta);
    }
}
