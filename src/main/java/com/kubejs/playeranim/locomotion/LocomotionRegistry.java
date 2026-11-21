package com.kubejs.playeranim.locomotion;

import java.util.ArrayList;
import java.util.List;

/**
 * 已注册的移动动画替换配置
 * <p>
 * 服务端脚本注册, 通过 S2C 同步给客户端; 客户端也允许直接从 client_scripts 注册。
 */
public final class LocomotionRegistry {

    private static final List<LocomotionProfile> PROFILES = new ArrayList<>();

    private LocomotionRegistry() {
    }

    public static synchronized void register(LocomotionProfile profile) {
        if (profile == null || profile.isEmpty() || !profile.hasConditions()) return;
        PROFILES.removeIf(existing -> existing.id().equals(profile.id()));
        PROFILES.add(profile);
    }

    public static synchronized void clear() {
        PROFILES.clear();
    }

    public static synchronized List<LocomotionProfile> all() {
        return List.copyOf(PROFILES);
    }
}
