package com.kubejs.playeranim.net;

import com.kubejs.playeranim.client.ClientAnimHandler;
import net.minecraft.network.FriendlyByteBuf;
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
    // 第一人称参数; null 表示未指定, 由动画 JSON 或配置决定
    private final String firstPersonMode;
    private final Boolean firstPersonRightArm;
    private final Boolean firstPersonLeftArm;
    private final Boolean firstPersonRightItem;
    private final Boolean firstPersonLeftItem;

    public PlayAnimMessage(UUID playerUUID, String animationId, int fadeTicks, String easeName, boolean stop) {
        this(playerUUID, animationId, fadeTicks, easeName, stop, null, null, null, null, null);
    }

    public PlayAnimMessage(UUID playerUUID, String animationId, int fadeTicks, String easeName, boolean stop,
                           String firstPersonMode, Boolean firstPersonRightArm, Boolean firstPersonLeftArm,
                           Boolean firstPersonRightItem, Boolean firstPersonLeftItem) {
        this.playerUUID = playerUUID;
        this.animationId = animationId;
        this.fadeTicks = fadeTicks;
        this.easeName = easeName;
        this.stop = stop;
        this.firstPersonMode = firstPersonMode;
        this.firstPersonRightArm = firstPersonRightArm;
        this.firstPersonLeftArm = firstPersonLeftArm;
        this.firstPersonRightItem = firstPersonRightItem;
        this.firstPersonLeftItem = firstPersonLeftItem;
    }

    public static void encode(PlayAnimMessage msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeUtf(msg.animationId, 256);
        buf.writeVarInt(msg.fadeTicks);
        buf.writeUtf(msg.easeName, 32);
        buf.writeBoolean(msg.stop);
        buf.writeBoolean(msg.firstPersonMode != null);
        if (msg.firstPersonMode != null) {
            buf.writeUtf(msg.firstPersonMode, 32);
        }
        writeOptionalBoolean(buf, msg.firstPersonRightArm);
        writeOptionalBoolean(buf, msg.firstPersonLeftArm);
        writeOptionalBoolean(buf, msg.firstPersonRightItem);
        writeOptionalBoolean(buf, msg.firstPersonLeftItem);
    }

    public static PlayAnimMessage decode(FriendlyByteBuf buf) {
        UUID playerUUID = buf.readUUID();
        String animationId = buf.readUtf(256);
        int fadeTicks = buf.readVarInt();
        String easeName = buf.readUtf(32);
        boolean stop = buf.readBoolean();
        String firstPersonMode = buf.readBoolean() ? buf.readUtf(32) : null;
        Boolean rightArm = readOptionalBoolean(buf);
        Boolean leftArm = readOptionalBoolean(buf);
        Boolean rightItem = readOptionalBoolean(buf);
        Boolean leftItem = readOptionalBoolean(buf);
        return new PlayAnimMessage(playerUUID, animationId, fadeTicks, easeName, stop,
                firstPersonMode, rightArm, leftArm, rightItem, leftItem);
    }

    private static void writeOptionalBoolean(FriendlyByteBuf buf, Boolean value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeBoolean(value);
        }
    }

    private static Boolean readOptionalBoolean(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readBoolean() : null;
    }

    public static void handle(PlayAnimMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            ClientAnimHandler.handlePlayAnim(msg.playerUUID, msg.animationId, msg.fadeTicks, msg.easeName, msg.stop,
                    msg.firstPersonMode, msg.firstPersonRightArm, msg.firstPersonLeftArm,
                    msg.firstPersonRightItem, msg.firstPersonLeftItem);
        });
        ctx.get().setPacketHandled(true);
    }
}
