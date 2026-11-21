package com.kubejs.playeranim.net;

import com.kubejs.playeranim.locomotion.LocomotionSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 同步移动动画替换配置
 */
public class SyncLocomotionMessage {

    private final String json;

    public SyncLocomotionMessage(String json) {
        this.json = json;
    }

    public static void encode(SyncLocomotionMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.json, 1_048_576);
    }

    public static SyncLocomotionMessage decode(FriendlyByteBuf buf) {
        return new SyncLocomotionMessage(buf.readUtf(1_048_576));
    }

    public static void handle(SyncLocomotionMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            LocomotionSync.applyJson(msg.json);
        });
        ctx.get().setPacketHandled(true);
    }
}
