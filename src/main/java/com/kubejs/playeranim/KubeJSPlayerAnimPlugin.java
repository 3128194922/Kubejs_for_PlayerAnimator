package com.kubejs.playeranim;

import com.kubejs.playeranim.client.ClientAnimHandler;
import com.kubejs.playeranim.event.PlayerAnimEvents;
import com.kubejs.playeranim.script.PlayerAnimBinding;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class KubeJSPlayerAnimPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        PlayerAnimEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        // 暴露 PlayerAnim 全局对象到 server_scripts / client_scripts
        event.add("PlayerAnim", new PlayerAnimBinding());
    }

    @Override
    public void clientInit() {
        // 仅客户端: 注册 playerAnimator 动画层
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientAnimHandler.registerAnimationLayer();
        }
    }
}
