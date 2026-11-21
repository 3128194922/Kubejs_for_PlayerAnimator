package com.kubejs.playeranim.script;

import com.kubejs.playeranim.locomotion.LocomotionProfile;
import com.kubejs.playeranim.locomotion.LocomotionRegistry;
import com.kubejs.playeranim.locomotion.LocomotionSync;
import dev.latvian.mods.kubejs.typings.Info;

/**
 * 暴露给 KubeJS 的移动动画替换入口
 * <p>
 * server_scripts 用法:
 * <pre>
 * const gun = PlayerAnim.locomotion("tkgun_bowgun")
 *   .mainHand("tacz:modern_kinetic_gun")
 *   .anim("idle", "tacz:rifle_idle")
 *   .anim("walk_front", "tacz:rifle_walk_front")
 *   .register()
 * </pre>
 */
public class LocomotionBinding {

    @Info("创建一套'持有时替换原版待机/移动动作'的配置, id 用于去重")
    public LocomotionProfile profile(String id) {
        return new LocomotionProfile(id);
    }

    @Info("清空所有已注册配置(会同步到客户端)")
    public void clear() {
        LocomotionRegistry.clear();
        LocomotionSync.broadcastAll();
    }
}
