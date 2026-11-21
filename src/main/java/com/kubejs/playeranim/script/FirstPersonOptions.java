package com.kubejs.playeranim.script;

import dev.latvian.mods.kubejs.typings.Info;

/**
 * 第一人称显示选项
 * <p>
 * 字段为 null 表示"未指定", 此时由动画 JSON 的 firstPerson 字段或配置文件决定。
 * <p>
 * 脚本用法:
 * <pre>
 * const fp = PlayerAnim.firstPerson()
 * fp.mode = "THIRD_PERSON_MODEL"
 * fp.rightArm = true
 * fp.leftArm = false
 * PlayerAnim.play(player, "wave", 10, "EASEINOUTQUAD", fp)
 * </pre>
 */
public class FirstPersonOptions {

    private String mode;
    private Boolean rightArm;
    private Boolean leftArm;
    private Boolean rightItem;
    private Boolean leftItem;

    @Info("第一人称模式: AUTO / NONE / VANILLA / THIRD_PERSON_MODEL / DISABLED, 留空表示自动")
    public String getMode() {
        return mode;
    }

    public FirstPersonOptions setMode(String mode) {
        this.mode = mode;
        return this;
    }

    @Info("是否显示右臂 (THIRD_PERSON_MODEL 模式下才有效), 留空表示按动画关键帧自动判断")
    public Boolean getRightArm() {
        return rightArm;
    }

    public FirstPersonOptions setRightArm(Boolean rightArm) {
        this.rightArm = rightArm;
        return this;
    }

    @Info("是否显示左臂 (THIRD_PERSON_MODEL 模式下才有效), 留空表示按动画关键帧自动判断")
    public Boolean getLeftArm() {
        return leftArm;
    }

    public FirstPersonOptions setLeftArm(Boolean leftArm) {
        this.leftArm = leftArm;
        return this;
    }

    @Info("是否显示右手物品, 留空表示显示")
    public Boolean getRightItem() {
        return rightItem;
    }

    public FirstPersonOptions setRightItem(Boolean rightItem) {
        this.rightItem = rightItem;
        return this;
    }

    @Info("是否显示左手物品, 留空表示显示")
    public Boolean getLeftItem() {
        return leftItem;
    }

    public FirstPersonOptions setLeftItem(Boolean leftItem) {
        this.leftItem = leftItem;
        return this;
    }
}
