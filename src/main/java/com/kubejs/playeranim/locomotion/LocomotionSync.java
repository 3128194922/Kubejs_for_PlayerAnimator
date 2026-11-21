package com.kubejs.playeranim.locomotion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kubejs.playeranim.KubeJSPlayerAnimMod;
import com.kubejs.playeranim.net.SyncLocomotionMessage;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.PacketDistributor;

/**
 * 注册入口: 服务端注册后广播给所有客户端, 客户端注册则只影响自己
 */
public final class LocomotionSync {

    private LocomotionSync() {
    }

    public static void publish(LocomotionProfile profile) {
        LocomotionRegistry.register(profile);
        if (FMLEnvironment.dist != Dist.CLIENT) {
            broadcastAll();
        }
    }

    /**
     * 把当前全部配置推送给所有在线客户端
     */
    public static void broadcastAll() {
        // 只有专职服务端才需要广播; 单人游戏的客户端与服务端共用同一份静态注册表
        if (KubeJSPlayerAnimMod.CHANNEL == null || FMLEnvironment.dist == Dist.CLIENT) return;
        KubeJSPlayerAnimMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncLocomotionMessage(exportJson()));
    }

    /**
     * 单个新玩家登录时推送
     */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player) {
        if (KubeJSPlayerAnimMod.CHANNEL == null || FMLEnvironment.dist == Dist.CLIENT) return;
        KubeJSPlayerAnimMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncLocomotionMessage(exportJson()));
    }

    public static String exportJson() {
        JsonArray array = new JsonArray();
        for (LocomotionProfile profile : LocomotionRegistry.all()) {
            array.add(profile.toJson());
        }
        JsonObject root = new JsonObject();
        root.add("profiles", array);
        return root.toString();
    }

    public static void applyJson(String json) {
        try {
            LocomotionRegistry.clear();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("profiles")) return;
            for (var element : root.getAsJsonArray("profiles")) {
                LocomotionRegistry.register(LocomotionProfile.fromJson(element.getAsJsonObject()));
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to apply locomotion profiles from server", e);
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("KubeJSPlayerAnim");
}
