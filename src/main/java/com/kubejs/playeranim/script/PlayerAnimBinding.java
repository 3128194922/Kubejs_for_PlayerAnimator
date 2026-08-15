package com.kubejs.playeranim.script;

import com.kubejs.playeranim.KubeJSPlayerAnimMod;
import com.kubejs.playeranim.event.PlayAnimEventJS;
import com.kubejs.playeranim.event.PlayerAnimEvents;
import com.kubejs.playeranim.net.PlayAnimMessage;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * 暴露给 KubeJS 脚本的全局绑定对象 "PlayerAnim"
 * <p>
 * server_scripts 用法:
 * <pre>
 * // 播放动画 (动画ID格式: "namespace:path")
 * PlayerAnim.play(event.player, "kubejs_playeranim:wave")
 *
 * // 带参数播放
 * PlayerAnim.play(event.player, "kubejs_playeranim:wave", 20, "EASEINOUTQUAD")
 *
 * // 停止动画
 * PlayerAnim.stop(event.player)
 * </pre>
 */
public class PlayerAnimBinding {

    @Info("为指定玩家播放动画, 默认 20 tick 淡入, LINEAR 缓动")
    public void play(Player player, String animationId) {
        play(player, animationId, 20, "LINEAR");
    }

    @Info("为指定玩家播放动画, 自定义淡入帧数")
    public void play(Player player, String animationId, int fadeTicks) {
        play(player, animationId, fadeTicks, "LINEAR");
    }

    /**
     * @param player      目标玩家
     * @param animationId 动画ID, 格式 "namespace:path", 对应 assets/namespace/player_animation/ 下的动画 name 字段
     * @param fadeTicks   淡入帧数
     * @param ease        缓动函数名 (LINEAR, EASEINQUAD, EASEOUTQUAD, EASEINOUTQUAD 等)
     */
    @Info("为指定玩家播放动画, 自定义淡入帧数和缓动函数")
    public void play(Player player, String animationId, int fadeTicks, String ease) {
        if (player == null || !(player instanceof ServerPlayer serverPlayer)) return;

        // 触发 server_scripts 事件, 允许脚本修改参数或取消
        var event = new PlayAnimEventJS(player, animationId, fadeTicks, ease);
        EventResult result = PlayerAnimEvents.PLAY.post(event);
        if (result.interruptFalse()) return; // 脚本调用了 event.cancel()

        // 发送 S2C 数据包给所有追踪该玩家的客户端 (含自己)
        var msg = new PlayAnimMessage(
                serverPlayer.getUUID(),
                event.getAnimation(),
                event.getFadeTicks(),
                event.getEase(),
                false
        );
        KubeJSPlayerAnimMod.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                msg
        );
    }

    @Info("停止指定玩家的当前动画 (带淡出)")
    public void stop(Player player) {
        stop(player, 20, "LINEAR");
    }

    @Info("停止指定玩家的当前动画, 自定义淡出帧数和缓动")
    public void stop(Player player, int fadeTicks, String ease) {
        if (player == null || !(player instanceof ServerPlayer serverPlayer)) return;

        var msg = new PlayAnimMessage(
                serverPlayer.getUUID(),
                "",
                fadeTicks,
                ease,
                true
        );
        KubeJSPlayerAnimMod.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                msg
        );
    }
}
