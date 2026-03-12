package com.linjiang.command.ui.theme

import androidx.compose.ui.graphics.Color

// 绛红色主题（设计方案 v5.0 — 绛红暖光统一风格）
val Crimson80 = Color(0xFFDC143C)
val CrimsonGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Crimson40 = Color(0xFF8B0000)
val CrimsonGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 品牌色 — 正绛红
val Accent = Color(0xFFE05A4F)
val AccentGlow = Color(0x4DE05A4F)          // 30%（原 20%，提升存在感）

// ══════════════════════════════════════════
// 背景体系 — 暖黑色调，偏棕紫的极深色
// ══════════════════════════════════════════
val BgDeep = Color(0xFF100A0E)              // 最深层：暖黑（偏紫棕）
val BgCard = Color(0xFF251A28)              // 卡片层：暖紫棕，与 BgDeep 拉开距离
val BgCardHover = Color(0xFF302338)         // 交互态：更亮的暖紫
val BgSurface = Color(0xFF1A1220)           // 表面层：介于 Deep 和 Card 之间
val BgNavBar = Color(0xFF130E16)            // 导航栏：略高于 Deep
val BgElevated = Color(0xFF302540)          // 浮起元素（dialog、弹窗）：明显浮起感
val BgGlass = Color(0x33FFFFFF)             // 毛玻璃效果叠加层（white 20% alpha）

// ══════════════════════════════════════════
// 暖光发光系统 — 暖金色，替代原青蓝 HUD
// ══════════════════════════════════════════
val WarmGlow = Color(0xFFD4A06A)            // 暖金发光色（主）
val WarmGlowDim = Color(0x40D4A06A)         // 暖金淡色（25% alpha，背景/底色）
val WarmGlowBorder = Color(0x30D4A06A)      // 暖金边框（19% alpha）
val WarmGlowGlow = Color(0x20D4A06A)        // 暖金微光（12% alpha，卡片底色叠加）
val GaugeTrack = Color(0xFF130E16)          // 仪表盘轨道色（和 BgNavBar 统一）

// ══════════════════════════════════════════
// 文字层次 — 暖灰色系，和背景协调
// ══════════════════════════════════════════
val TextPrimary = Color(0xFFF0EDE8)         // 暖白
val TextSecondary = Color(0xFF9A8F8A)       // 暖灰
val TextDim = Color(0xFF5A524D)             // 深暖灰

// ══════════════════════════════════════════
// 语义色（功能色，保持不变）
// ══════════════════════════════════════════
val StatusGreen = Color(0xFF34D399)
val StatusGreenDim = Color(0x3334D399)
val StatusYellow = Color(0xFFFBBF24)
val StatusYellowDim = Color(0x33FBBF24)
val StatusRed = Color(0xFFF87171)
val StatusRedDim = Color(0x33F87171)
val StatusBlue = Color(0xFF60A5FA)
val StatusBlueDim = Color(0x3360A5FA)

// 实例专属颜色
val InstanceBlue = Color(0xFF4FC3F7)        // 天蓝色（翎云专属）
val InstanceBlueDim = Color(0x334FC3F7)     // 天蓝色淡色

/**
 * 获取实例专属颜色
 * 翎绛 → 绛红（Accent），翎云 → 天蓝（InstanceBlue），其他 → 暖金（WarmGlow）
 */
fun getInstanceColor(instanceId: String, instanceName: String): Color {
    return when {
        instanceId.contains("linjiang", ignoreCase = true) ||
        instanceId.contains("main", ignoreCase = true) ||
        instanceName.contains("翎绛") -> Accent
        instanceId.contains("lingyun", ignoreCase = true) ||
        instanceName.contains("翎云") -> InstanceBlue
        else -> WarmGlow
    }
}

// 渐变辅助色
val StatusPurple = Color(0xFF8B5CF6)

// 兼容旧代码
val StatusConnected = StatusGreen
val StatusConnecting = StatusYellow
val StatusDisconnected = StatusRed

// ══════════════════════════════════════════
// 毛玻璃 & 边框 — 暖金色微发光边框
// ══════════════════════════════════════════
val GlassBorder = Color(0x30D4A06A)         // 暖金边框 19% alpha
val GlassBorderLight = Color(0x22D4A06A)    // 淡暖金边框 13% alpha
val GlassBorderFocus = Color(0x45D4A06A)    // 焦点态暖金边框 27% alpha
val GlassBorderHover = Color(0x55D4A06A)    // 悬浮态暖金边框 33% alpha

// Accent 变体（透明度提升）— 用于强调元素
val AccentDim = Color(0x59E05A4F)           // 35%（原 20%）— 用户消息气泡底色
val AccentBorder = Color(0x26E05A4F)        // 15%（原 10%）— 保留兼容
val AccentBorderActive = Color(0x66E05A4F)  // 40%（原 30%）— 选中/活跃态

// ══════════════════════════════════════════
// 消息气泡颜色 — 绛红暖光风格，提高不透明度
// ══════════════════════════════════════════
val UserMessageBg = Color(0x59E05A4F)       // accent 35%（原 20%）
val AssistantMessageBg = Color(0xCC1A1220)  // BgSurface 80%（原 70%）
val SystemMessageBg = Color(0xCC130E16)     // BgNavBar 80%（原 70%）
