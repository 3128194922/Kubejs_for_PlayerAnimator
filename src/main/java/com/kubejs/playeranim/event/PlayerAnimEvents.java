package com.kubejs.playeranim.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface PlayerAnimEvents {
    EventGroup GROUP = EventGroup.of("PlayerAnimEvents");

    // server_scripts 可监听: PlayerAnimEvents.play(event => { ... })
    // 每次调用 PlayerAnim.play() 时触发, 允许脚本拦截/修改播放参数
    EventHandler PLAY = GROUP.server("play", () -> PlayAnimEventJS.class);
}
