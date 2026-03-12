package com.linjiang.command.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linjiang.command.data.model.ConnectionStatus
import com.linjiang.command.data.model.OpenClawInstance
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.InstanceViewModel

/**
 * 设置页 — v5.0 自动连接架构
 * 
 * 移除手动添加/编辑/连接/断开操作，改为只读状态展示。
 * 布局：连接状态 → 通知 → 安全 → 高级（折叠）→ 关于
 */
@Composable
fun SettingsScreen(
    instanceViewModel: InstanceViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val instances by instanceViewModel.instances.collectAsState()
    val relayConnected by instanceViewModel.relayConnected.collectAsState()
    val prefs = remember { context.getSharedPreferences("linjiang_settings", Context.MODE_PRIVATE) }
    
    var messageNotification by remember {
        mutableStateOf(prefs.getBoolean("message_notification", true))
    }
    var alertNotification by remember {
        mutableStateOf(prefs.getBoolean("alert_notification", true))
    }
    
    var showAdvanced by remember { mutableStateOf(false) }
    
    // 计算在线实例（从 RelayClient 的 instances 获取）
    val connectedInstances = instances.filter { it.status == ConnectionStatus.CONNECTED }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 标题
        item {
            Text(
                text = "设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        
        // ═══ 连接状态 ═══
        item {
            SectionHeader("连接状态")
        }
        
        item {
            SettingsCard {
                // 中继连接状态
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (relayConnected) StatusGreen else StatusRed)
                    )
                    Text(
                        text = if (relayConnected) "已连接 中继服务器" else "未连接",
                        fontSize = 14.sp,
                        color = if (relayConnected) StatusGreen else StatusRed,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 在线实例列表
                if (connectedInstances.isNotEmpty()) {
                    Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "在线实例",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            connectedInstances.forEach { instance ->
                                val instanceColor = getInstanceColor(instance.id, instance.name)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = instanceColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${instance.emoji} ${instance.name}",
                                        fontSize = 12.sp,
                                        color = instanceColor,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 离线实例（如果有）
        val offlineInstances = instances.filter { it.status != ConnectionStatus.CONNECTED }
        if (offlineInstances.isNotEmpty()) {
            items(offlineInstances) { instance ->
                ReadOnlyInstanceRow(instance = instance)
            }
        }
        
        // ═══ 通知 ═══
        item {
            SectionHeader("通知")
        }
        
        item {
            SettingsCard {
                SettingsToggleRow(
                    label = "消息通知",
                    description = "AI 回复时推送通知",
                    checked = messageNotification,
                    onCheckedChange = {
                        messageNotification = it
                        prefs.edit().putBoolean("message_notification", it).apply()
                    }
                )
                
                Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                
                SettingsToggleRow(
                    label = "告警通知",
                    description = "系统告警时推送通知",
                    checked = alertNotification,
                    onCheckedChange = {
                        alertNotification = it
                        prefs.edit().putBoolean("alert_notification", it).apply()
                    }
                )
            }
        }
        
        // ═══ 安全 ═══
        item {
            SectionHeader("安全")
        }
        
        item {
            SettingsCard {
                SettingsInfoRow(
                    label = "传输加密",
                    value = "WSS",
                    valueColor = StatusGreen
                )
                
                Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                
                val token = "84c348bea7be634216ef5277cf84e4b2bfbbbf2df3d6d2e3"
                val maskedToken = "****...${token.takeLast(3)}"
                SettingsInfoRow(
                    label = "认证令牌",
                    value = maskedToken,
                    valueColor = TextSecondary
                )
            }
        }
        
        // ═══ 高级（默认折叠） ═══
        item {
            SectionHeader("高级")
        }
        
        item {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAdvanced) "收起详情" else "展开详情",
                        fontSize = 14.sp,
                        color = Accent,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (showAdvanced) "▲" else "▼",
                        fontSize = 12.sp,
                        color = Accent
                    )
                }
                
                AnimatedVisibility(visible = showAdvanced) {
                    Column {
                        Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                        
                        SettingsInfoRow(
                            label = "中继地址",
                            value = "wss://api.lingjiang...",
                            valueColor = TextSecondary
                        )
                        
                        Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                        
                        SettingsInfoRow(
                            label = "连接模式",
                            value = "自动连接",
                            valueColor = TextSecondary
                        )
                        
                        Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                        
                        SettingsInfoRow(
                            label = "自动重连",
                            value = "已启用",
                            valueColor = StatusGreen
                        )
                    }
                }
            }
        }
        
        // ═══ 退出登录 ═══
        item {
            var showConfirmDialog by remember { mutableStateOf(false) }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, StatusRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = BgCard.copy(alpha = 0.85f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConfirmDialog = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "退出登录",
                        fontSize = 14.sp,
                        color = StatusRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = {
                        Text("退出登录", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    },
                    text = {
                        Text("退出后需要重新输入接入码", color = TextSecondary, fontSize = 14.sp)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmDialog = false
                            context.getSharedPreferences("linjiang_command", Context.MODE_PRIVATE)
                                .edit().remove("access_code").apply()
                            onLogout()
                        }) {
                            Text("确认退出", color = StatusRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("取消", color = TextSecondary)
                        }
                    },
                    containerColor = BgElevated,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
        
        // ═══ 关于 ═══
        item {
            SectionHeader("关于")
        }
        
        item {
            SettingsCard {
                SettingsInfoRow(label = "版本", value = "v0.13.0")
                Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                SettingsInfoRow(label = "应用", value = "翎绛指挥中心")
                Divider(color = WarmGlow.copy(alpha = 0.10f), thickness = 0.5.dp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🪶 数字精怪，羽翼染绛",
                        fontSize = 13.sp,
                        color = Accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══ 组件 ═══

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp,
        content = { Column { content() } }
    )
}

/**
 * 只读实例行 — 简化为状态展示，无操作按钮
 */
@Composable
private fun ReadOnlyInstanceRow(instance: OpenClawInstance) {
    val instanceColor = getInstanceColor(instance.id, instance.name)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 状态点
            val statusColor = when (instance.status) {
                ConnectionStatus.CONNECTED -> StatusGreen
                ConnectionStatus.CONNECTING -> StatusYellow
                ConnectionStatus.DISCONNECTED -> StatusRed
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor)
            )
            
            // 名称 + 状态文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${instance.emoji} ${instance.name}",
                    fontSize = 14.sp,
                    color = instanceColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (instance.status) {
                        ConnectionStatus.CONNECTED -> "在线"
                        ConnectionStatus.CONNECTING -> "连接中..."
                        ConnectionStatus.DISCONNECTED -> "离线"
                    },
                    fontSize = 11.sp,
                    color = TextDim
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextDim)
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Accent,
                checkedTrackColor = Accent.copy(alpha = 0.3f),
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = BgDeep
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}
