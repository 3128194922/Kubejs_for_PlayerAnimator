package com.kubejs.playeranim.locomotion;

/**
 * 原版移动状态, 用来挑选对应的替换动画
 */
public enum LocomotionState {
    IDLE("idle"),
    WALK("walk"),
    WALK_FORWARD("walk_front"),
    WALK_BACKWARD("walk_back"),
    WALK_LEFT("walk_left"),
    WALK_RIGHT("walk_right"),
    RUN("run"),
    SNEAK("sneak"),
    JUMP("jump"),
    FALL("fall"),
    SWIM("swim");

    private final String key;

    LocomotionState(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /**
     * 脚本里用字符串指代状态, 大小写/下划线都兼容
     */
    public static LocomotionState byKey(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase().replace('-', '_');
        for (LocomotionState state : values()) {
            if (state.key.equals(normalized) || state.name().toLowerCase().equals(normalized)) {
                return state;
            }
        }
        return null;
    }
}
