package com.kubejs.playeranim.event;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.entity.player.Player;

/**
 * server_scripts 事件, 在 PlayerAnim.play() 调用时触发
 * <p>
 * 用法:
 * <pre>
 * PlayerAnimEvents.play(event => {
 *     console.log("Playing " + event.animation + " on " + event.player.username)
 *     // 可修改: event.animation, event.fadeTicks, event.ease
 *     // event.cancel() 可取消播放
 * })
 * </pre>
 */
public class PlayAnimEventJS extends EventJS {
    private Player player;
    private String animation;
    private int fadeTicks;
    private String ease;

    public PlayAnimEventJS(Player player, String animation, int fadeTicks, String ease) {
        this.player = player;
        this.animation = animation;
        this.fadeTicks = fadeTicks;
        this.ease = ease;
    }

    @Info("要播放动画的玩家")
    public Player getPlayer() {
        return player;
    }

    @Info("动画ID, 格式: namespace:path (对应 assets/namespace/player_animation/ 下的动画名)")
    public String getAnimation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation;
    }

    @Info("淡入帧数")
    public int getFadeTicks() {
        return fadeTicks;
    }

    public void setFadeTicks(int fadeTicks) {
        this.fadeTicks = fadeTicks;
    }

    @Info("缓动函数名, 如 LINEAR, EASEINQUAD, EASEINOUTQUAD 等")
    public String getEase() {
        return ease;
    }

    public void setEase(String ease) {
        this.ease = ease;
    }
}
