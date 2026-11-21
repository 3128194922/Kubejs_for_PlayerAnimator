package com.kubejs.playeranim.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;

/**
 * 解析完成的第一人称设置
 */
public record FirstPersonSettings(FirstPersonMode mode, FirstPersonConfiguration configuration) {
}
