package com.kubejs.playeranim;

import com.kubejs.playeranim.net.PlayAnimMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(KubeJSPlayerAnimMod.MOD_ID)
public class KubeJSPlayerAnimMod {
    public static final String MOD_ID = "kubejs_playeranim";

    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    private static int packetId = 0;

    public KubeJSPlayerAnimMod() {
        // 注册 mod event bus 上的事件监听
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // 必须在 mod 加载阶段注册 channel, 否则会报 "Registration of impl channels is locked"
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MOD_ID, "main"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        // 注册配置 (config/kubejs_playeranim-common.toml)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, KubeJSPlayerAnimConfig.SPEC);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 在 FMLCommonSetupEvent 中注册 packet, 确保在注册窗口关闭前完成
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(packetId++,
                    PlayAnimMessage.class,
                    PlayAnimMessage::encode,
                    PlayAnimMessage::decode,
                    PlayAnimMessage::handle);
        });
    }
}
