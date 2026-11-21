package com.kubejs.playeranim.client;

import com.kubejs.playeranim.locomotion.LocomotionProfile;
import com.kubejs.playeranim.locomotion.LocomotionState;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * 根据玩家的真实移动状态挑选 {@link LocomotionState}
 * <p>
 * 判定思路与 MechanicianCombat 的 TkGunClient 一致: 用位置差采样而不是动画相位,
 * 因为动画相位由原版模型自己推进, 无法反映"站定却还在踏步"的情况。
 */
public final class LocomotionResolver {

    /** 每 tick 水平位移超过 0.02 格才算移动 */
    private static final double MOVING_EPS2 = 0.02 * 0.02;
    /** 停下后再当作"在动"的宽限 tick 数, 避免临界速度抖动 */
    private static final int MOVING_HOLD_TICKS = 4;
    /** 疾跑兜底阈值: 疾跑约 0.28, 普通走约 0.216 */
    private static final double RUN_SPEED_MIN = 0.25;

    private final AbstractClientPlayer player;

    private double lastX;
    private double lastZ;
    private boolean primed;

    private double dirX;
    private double dirZ;
    private float dirYaw;
    private boolean sprinting;
    private long movingUntil;
    private long ticks;

    public LocomotionResolver(AbstractClientPlayer player) {
        this.player = player;
    }

    /**
     * 每客户端 tick 调用一次
     */
    public void tick() {
        ticks++;
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        lastX = player.getX();
        lastZ = player.getZ();
        if (!primed) {
            primed = true;
            return;
        }
        if (dx * dx + dz * dz > MOVING_EPS2) {
            movingUntil = ticks + MOVING_HOLD_TICKS;
            dirX = dx;
            dirZ = dz;
            dirYaw = player.getYRot();
            sprinting = player.isSprinting();
        }
    }

    public boolean isMoving() {
        return primed && ticks < movingUntil;
    }

    /**
     * 当前应播放的移动状态
     */
    public LocomotionState resolve() {
        if (player.isSwimming() || player.isVisuallySwimming()) return LocomotionState.SWIM;
        if (!player.onGround()) {
            return player.getDeltaMovement().y > 0.01D ? LocomotionState.JUMP : LocomotionState.FALL;
        }
        if (!isMoving()) {
            return player.isShiftKeyDown() || player.isCrouching() ? LocomotionState.SNEAK : LocomotionState.IDLE;
        }
        if (player.isShiftKeyDown() || player.isCrouching()) return LocomotionState.SNEAK;

        double yaw = Math.toRadians(dirYaw);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double forward = dirX * fx + dirZ * fz;
        double right = dirX * rx + dirZ * rz;
        double speed = Math.sqrt(dirX * dirX + dirZ * dirZ);

        // 只有真正向前、且速度到疾跑量级时才用 run; 侧移/后退一律走 walk_*
        if ((sprinting || speed > RUN_SPEED_MIN) && forward > 0.0D && Math.abs(forward) >= Math.abs(right)) {
            return LocomotionState.RUN;
        }
        if (Math.abs(forward) >= Math.abs(right)) {
            return forward >= 0.0D ? LocomotionState.WALK_FORWARD : LocomotionState.WALK_BACKWARD;
        }
        return right >= 0.0D ? LocomotionState.WALK_RIGHT : LocomotionState.WALK_LEFT;
    }

    /**
     * 在已注册配置里找出优先级最高、且能提供该状态动画的那一套
     */
    public static LocomotionProfile select(java.util.List<LocomotionProfile> profiles,
                                           AbstractClientPlayer player, LocomotionState state) {
        LocomotionProfile best = null;
        for (LocomotionProfile profile : profiles) {
            if (!profile.hasAnimation(state)) continue;
            if (!profile.matches(player)) continue;
            if (best == null || profile.priority() > best.priority()) {
                best = profile;
            }
        }
        return best;
    }
}
