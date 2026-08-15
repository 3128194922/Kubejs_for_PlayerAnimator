package com.kubejs.playeranim.net;

import com.kubejs.playeranim.KubeJSPlayerAnimMod;
import com.kubejs.playeranim.client.ClientAnimHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C 数据包: 服务端 → 客户端, 通知客户端为指定玩家播放动画
 * <p>
 * 客户端 handle 中的 playerAnimator 相关类仅在客户端加载,
 * 因为 handle 只在客户端执行 (S2C), 服务端仅调用 encode
 */
public class PlayAnimMessage {
    private final UUID playerUUID;
    private final String animationId;
    private final int fadeTicks;
    private final String easeName;
    private final boolean stop; // true = 停止当前动画, false = 播放新动画

    public PlayAnimMessage(UUID playerUUID, String animationId, int fadeTicks, String easeName, boolean stop) {
        this.playerUUID = playerUUID;
        this.animationId = animationId;
        this.fadeTicks = fadeTicks;
        this.easeName = easeName;
        this.stop = stop;
    }

    public static void encode(PlayAnimMessage msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeUtf(msg.animationId, 256);
        buf.writeVarInt(msg.fadeTicks);
        buf.writeUtf(msg.easeName, 32);
        buf.writeBoolean(msg.stop);
    }

    public static PlayAnimMessage decode(FriendlyByteBuf buf) {
        return new PlayAnimMessage(
                buf.readUUID(),
                buf.readUtf(256),
                buf.readVarInt(),
                buf.readUtf(32),
                buf.readBoolean()
        );
    }

    public static void handle(PlayAnimMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            ClientAnimHandler.handlePlayAnim(msg.playerUUID, msg.animationId, msg.fadeTicks, msg.easeName, msg.stop);
        });
        ctx.get().setPacketHandled(true);
    }
}
