package com.kubejs.playeranim.locomotion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一套"持有时替换原版待机/移动动作"的配置
 * <p>
 * 纯数据对象, 不引用任何客户端类, 服务端脚本里也能安全创建。
 */
public class LocomotionProfile {

    public static final String DEFAULT_NAMESPACE = "kubejs_playeranim";

    private final String id;
    private int priority = 1000;
    private int fadeTicks = 4;
    private boolean always;
    private boolean loop = true;

    private String firstPersonMode;
    private Boolean showRightArm;
    private Boolean showLeftArm;
    private Boolean showRightItem;
    private Boolean showLeftItem;

    private final List<String> mainHand = new ArrayList<>();
    private final List<String> offHand = new ArrayList<>();
    private final List<String> eitherHand = new ArrayList<>();
    private final Map<LocomotionState, String> animations = new EnumMap<>(LocomotionState.class);

    public LocomotionProfile(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public LocomotionProfile priority(int priority) {
        this.priority = priority;
        return this;
    }

    public int fadeTicks() {
        return fadeTicks;
    }

    public LocomotionProfile fadeTicks(int fadeTicks) {
        this.fadeTicks = Math.max(0, fadeTicks);
        return this;
    }

    public boolean always() {
        return always;
    }

    @Info("是否对所有玩家生效(不判断手持物品)")
    public LocomotionProfile always(boolean always) {
        this.always = always;
        return this;
    }

    public boolean loop() {
        return loop;
    }

    @Info("动画是否循环播放, 默认 true; 待机/移动动画必须循环, 否则播一次就停")
    public LocomotionProfile loop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public String firstPersonMode() {
        return firstPersonMode;
    }

    public LocomotionProfile firstPersonMode(String mode) {
        this.firstPersonMode = mode;
        return this;
    }

    public Boolean showRightArm() {
        return showRightArm;
    }

    public LocomotionProfile showRightArm(Boolean value) {
        this.showRightArm = value;
        return this;
    }

    public Boolean showLeftArm() {
        return showLeftArm;
    }

    public LocomotionProfile showLeftArm(Boolean value) {
        this.showLeftArm = value;
        return this;
    }

    public Boolean showRightItem() {
        return showRightItem;
    }

    public LocomotionProfile showRightItem(Boolean value) {
        this.showRightItem = value;
        return this;
    }

    public Boolean showLeftItem() {
        return showLeftItem;
    }

    public LocomotionProfile showLeftItem(Boolean value) {
        this.showLeftItem = value;
        return this;
    }

    public LocomotionProfile mainHand(String... ids) {
        addAll(mainHand, ids);
        return this;
    }

    @Info("主手物品ID, 支持 namespace:item / namespace:* / #namespace:tag")
    public LocomotionProfile offHand(String... ids) {
        addAll(offHand, ids);
        return this;
    }

    @Info("副手物品ID, 支持 namespace:item / namespace:* / #namespace:tag")
    public LocomotionProfile eitherHand(String... ids) {
        addAll(eitherHand, ids);
        return this;
    }

    private static void addAll(List<String> target, String[] ids) {
        if (ids == null) return;
        for (String id : ids) {
            if (id != null && !id.isBlank()) target.add(id.trim());
        }
    }

    public LocomotionProfile animState(LocomotionState state, String animationId) {
        if (state != null && animationId != null && !animationId.isBlank()) {
            animations.put(state, animationId.trim());
        }
        return this;
    }

    @Info("状态 -> 动画ID。状态可用: idle / walk / walk_front / walk_back / walk_left / walk_right / run / sneak / jump / fall / swim")
    public LocomotionProfile anim(String state, String animationId) {
        return animState(LocomotionState.byKey(state), animationId);
    }

    @Info("待机动画")
    public LocomotionProfile idle(String animationId) {
        return animState(LocomotionState.IDLE, animationId);
    }

    @Info("通用走路动画(四方向未单独指定时使用)")
    public LocomotionProfile walk(String animationId) {
        return animState(LocomotionState.WALK, animationId);
    }

    @Info("向前走")
    public LocomotionProfile walkForward(String animationId) {
        return animState(LocomotionState.WALK_FORWARD, animationId);
    }

    @Info("向后走")
    public LocomotionProfile walkBackward(String animationId) {
        return animState(LocomotionState.WALK_BACKWARD, animationId);
    }

    @Info("向左平移")
    public LocomotionProfile walkLeft(String animationId) {
        return animState(LocomotionState.WALK_LEFT, animationId);
    }

    @Info("向右平移")
    public LocomotionProfile walkRight(String animationId) {
        return animState(LocomotionState.WALK_RIGHT, animationId);
    }

    @Info("疾跑")
    public LocomotionProfile run(String animationId) {
        return animState(LocomotionState.RUN, animationId);
    }

    @Info("潜行")
    public LocomotionProfile sneak(String animationId) {
        return animState(LocomotionState.SNEAK, animationId);
    }

    @Info("起跳")
    public LocomotionProfile jump(String animationId) {
        return animState(LocomotionState.JUMP, animationId);
    }

    @Info("下落")
    public LocomotionProfile fall(String animationId) {
        return animState(LocomotionState.FALL, animationId);
    }

    @Info("提交注册, 服务端会自动同步给所有客户端")
    public LocomotionProfile register() {
        LocomotionSync.publish(this);
        return this;
    }

    public boolean hasConditions() {
        return always || !mainHand.isEmpty() || !offHand.isEmpty() || !eitherHand.isEmpty();
    }

    public boolean isEmpty() {
        return animations.isEmpty();
    }

    /**
     * 当前玩家是否符合这套配置的触发条件
     */
    public boolean matches(Player player) {
        if (player == null) return false;
        if (always) return true;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!mainHand.isEmpty() && matchesAny(main, mainHand)) return true;
        if (!offHand.isEmpty() && matchesAny(off, offHand)) return true;
        if (!eitherHand.isEmpty() && (matchesAny(main, eitherHand) || matchesAny(off, eitherHand))) return true;
        return false;
    }

    /**
     * 该状态对应的动画ID, 找不到时按链式回退
     */
    public String animationFor(LocomotionState state) {
        if (state == null) return null;
        String direct = animations.get(state);
        if (direct != null) return direct;
        for (LocomotionState fallback : fallbacks(state)) {
            String id = animations.get(fallback);
            if (id != null) return id;
        }
        return null;
    }

    public boolean hasAnimation(LocomotionState state) {
        return animationFor(state) != null;
    }

    public ResourceLocation animationResource(LocomotionState state) {
        return parseAnimationId(animationFor(state));
    }

    private static List<LocomotionState> fallbacks(LocomotionState state) {
        return switch (state) {
            case WALK_FORWARD, WALK_BACKWARD, WALK_LEFT, WALK_RIGHT -> List.of(LocomotionState.WALK);
            case RUN -> List.of(LocomotionState.WALK);
            case JUMP -> List.of(LocomotionState.FALL);
            case FALL -> List.of(LocomotionState.JUMP);
            // 没写潜行/游泳动画时退回待机, 而不是直接放弃接管
            case SNEAK, SWIM -> List.of(LocomotionState.IDLE);
            default -> List.of();
        };
    }

    private static ResourceLocation parseAnimationId(String animationId) {
        if (animationId == null || animationId.isBlank()) return null;
        if (animationId.indexOf(':') < 0) {
            return new ResourceLocation(DEFAULT_NAMESPACE, animationId);
        }
        return new ResourceLocation(animationId);
    }

    /**
     * 支持三种写法: 完整物品ID、命名空间通配 "namespace:*"、标签 "#namespace:tag"
     */
    private static boolean matchesAny(ItemStack stack, List<String> specs) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (String spec : specs) {
            if (spec.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(spec.substring(1));
                if (tagId != null && stack.is(ItemTags.create(tagId))) return true;
                continue;
            }
            if (spec.endsWith(":*")) {
                String namespace = spec.substring(0, spec.length() - 2);
                if (itemId.getNamespace().equals(namespace)) return true;
                continue;
            }
            ResourceLocation expected = ResourceLocation.tryParse(spec);
            if (expected != null && expected.equals(itemId)) return true;
        }
        return false;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("priority", priority);
        root.addProperty("fadeTicks", fadeTicks);
        root.addProperty("always", always);
        root.addProperty("loop", loop);
        if (firstPersonMode != null) root.addProperty("firstPersonMode", firstPersonMode);
        if (showRightArm != null) root.addProperty("showRightArm", showRightArm);
        if (showLeftArm != null) root.addProperty("showLeftArm", showLeftArm);
        if (showRightItem != null) root.addProperty("showRightItem", showRightItem);
        if (showLeftItem != null) root.addProperty("showLeftItem", showLeftItem);
        addArray(root, "mainHand", mainHand);
        addArray(root, "offHand", offHand);
        addArray(root, "eitherHand", eitherHand);
        JsonObject anims = new JsonObject();
        animations.forEach((state, animationId) -> anims.addProperty(state.key(), animationId));
        root.add("animations", anims);
        return root;
    }

    private static void addArray(JsonObject root, String key, List<String> values) {
        if (values.isEmpty()) return;
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        root.add(key, array);
    }

    public static LocomotionProfile fromJson(JsonObject root) {
        LocomotionProfile profile = new LocomotionProfile(root.get("id").getAsString());
        if (root.has("priority")) profile.priority = root.get("priority").getAsInt();
        if (root.has("fadeTicks")) profile.fadeTicks = root.get("fadeTicks").getAsInt();
        if (root.has("always")) profile.always = root.get("always").getAsBoolean();
        if (root.has("loop")) profile.loop = root.get("loop").getAsBoolean();
        if (root.has("firstPersonMode")) profile.firstPersonMode = root.get("firstPersonMode").getAsString();
        if (root.has("showRightArm")) profile.showRightArm = root.get("showRightArm").getAsBoolean();
        if (root.has("showLeftArm")) profile.showLeftArm = root.get("showLeftArm").getAsBoolean();
        if (root.has("showRightItem")) profile.showRightItem = root.get("showRightItem").getAsBoolean();
        if (root.has("showLeftItem")) profile.showLeftItem = root.get("showLeftItem").getAsBoolean();
        readArray(root, "mainHand", profile.mainHand);
        readArray(root, "offHand", profile.offHand);
        readArray(root, "eitherHand", profile.eitherHand);
        if (root.has("animations")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
                profile.animState(LocomotionState.byKey(entry.getKey()), entry.getValue().getAsString());
            }
        }
        return profile;
    }

    private static void readArray(JsonObject root, String key, List<String> target) {
        if (!root.has(key)) return;
        for (JsonElement element : root.getAsJsonArray(key)) {
            target.add(element.getAsString());
        }
    }
}
