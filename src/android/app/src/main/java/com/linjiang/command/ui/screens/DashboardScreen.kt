package com.linjiang.command.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linjiang.command.data.model.*
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 态势页 — HUD 指挥中心科幻风格 v4.0
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    connectedInstances: Map<String, Boolean> = emptyMap(),
    instanceNames: Map<String, String> = emptyMap(),
    instanceEmojis: Map<String, String> = emptyMap(),
    onInstanceClick: (String) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val healthMap by viewModel.instanceHealthMap.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val timelineEvents by viewModel.timelineEvents.collectAsState()
    val subAgentTasks by viewModel.subAgentTasks.collectAsState()
    val onlineCount by viewModel.onlineInstanceCount.collectAsState()

    val unreadAlerts = alerts.filter { !it.isRead }
    val activeTasks = subAgentTasks.filter { it.status == TaskStatus.RUNNING }
    val latestAlert = unreadAlerts.maxByOrNull { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
    ) {
        // ========== 状态栏（HUD 装饰） ==========
        StatusBar(
            onlineCount = onlineCount,
            totalCount = connectedInstances.size
        )

        // ========== 中心健康度仪表 ==========
        CentralHealthGauge(
            onlineCount = onlineCount,
            totalCount = connectedInstances.size,
            activeTaskCount = activeTasks.size,
            alertCount = unreadAlerts.size,
            hasUrgentAlert = unreadAlerts.any { it.level == AlertLevel.CRITICAL },
            onInstanceClick = { onNavigateToSettings() },
            onTaskClick = { onNavigateToTasks() },
            onAlertClick = { /* TODO: 告警详情 */ }
        )

        // ========== 告警横幅 ==========
        if (latestAlert != null) {
            AlertBanner(alert = latestAlert)
        }

        // ========== 实例条 ==========
        SectionHeader(title = "实例", action = "管理 →", onAction = onNavigateToSettings)

        InstanceStrip(
            healthMap = healthMap,
            connectedInstances = connectedInstances,
            instanceNames = instanceNames,
            instanceEmojis = instanceEmojis,
            onInstanceClick = onInstanceClick
        )

        // ========== 活跃任务 ==========
        if (subAgentTasks.isNotEmpty()) {
            SectionHeader(
                title = "活跃任务",
                action = "全部 →",
                onAction = onNavigateToTasks
            )

            TaskStream(
                tasks = subAgentTasks.take(3),
                instanceNames = instanceNames
            )
        }

        // ========== 快捷指令 ==========
        SectionHeader(title = "快捷指令")

        QuickActions(
            onDispatch = onNavigateToTasks,
            onSearch = { /* TODO: 记忆搜索 */ },
            onSummary = onNavigateToChat,
            onHealthCheck = { /* TODO: 全体自检 */ }
        )

        // ========== 今日时间线 ==========
        if (timelineEvents.isNotEmpty()) {
            SectionHeader(
                title = "今日动态",
                action = "全部 →",
                onAction = {}
            )

            Timeline(events = timelineEvents.take(5))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==================== 状态栏（HUD 装饰线） ====================

@Composable
private fun StatusBar(onlineCount: Int, totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(
                color = when {
                    totalCount == 0 -> TextDim
                    onlineCount == totalCount -> StatusGreen
                    onlineCount > 0 -> StatusYellow
                    else -> StatusRed
                }
            )
            Spacer(modifier = Modifier.width(10.dp))
            // HUD 装饰标题：── 翎绛指挥中心 ──
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = WarmGlow.copy(alpha = 0.5f), fontSize = 12.sp)) { append("── ") }
                    append("翎")
                    withStyle(SpanStyle(color = Accent)) { append("绛") }
                    append("指挥中心")
                    withStyle(SpanStyle(color = WarmGlow.copy(alpha = 0.5f), fontSize = 12.sp)) { append(" ──") }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        val statusText = when {
            totalCount == 0 -> "未连接"
            onlineCount == totalCount -> "全部在线"
            onlineCount > 0 -> "$onlineCount/$totalCount 在线"
            else -> "全部离线"
        }
        val statusColor = when {
            totalCount == 0 -> TextDim
            onlineCount == totalCount -> StatusGreen
            onlineCount > 0 -> StatusYellow
            else -> StatusRed
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = statusColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.2f))
        ) {
            Text(
                "● $statusText",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut), repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut), repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

// ==================== 中心健康度仪表 ====================

@Composable
private fun CentralHealthGauge(
    onlineCount: Int,
    totalCount: Int,
    activeTaskCount: Int,
    alertCount: Int,
    hasUrgentAlert: Boolean,
    onInstanceClick: () -> Unit,
    onTaskClick: () -> Unit,
    onAlertClick: () -> Unit
) {
    val healthPercent = if (totalCount > 0) (onlineCount.toFloat() / totalCount * 100).toInt() else 0
    val sweepAngle = if (totalCount > 0) (onlineCount.toFloat() / totalCount * 270f) else 0f
    val gaugeColor = when {
        totalCount == 0 -> TextDim
        onlineCount == totalCount -> StatusGreen
        onlineCount > 0 -> StatusYellow
        else -> StatusRed
    }

    // 扫描线动画
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "scanRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 中心圆环仪表
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8f
                val padding = strokeWidth / 2 + 12f
                val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
                val topLeft = Offset(padding, padding)

                // 轨道（底色）
                drawArc(
                    color = GaugeTrack,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 进度弧
                if (sweepAngle > 0f) {
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 外层 HUD 装饰环（极细青蓝线）
                val outerPadding = 4f
                val outerSize = Size(size.width - outerPadding * 2, size.height - outerPadding * 2)
                drawArc(
                    color = WarmGlow.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(outerPadding, outerPadding),
                    size = outerSize,
                    style = Stroke(width = 1f)
                )

                // 扫描线效果（微弱的青蓝色短弧）
                drawArc(
                    color = WarmGlow.copy(alpha = 0.08f),
                    startAngle = scanAngle,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(outerPadding, outerPadding),
                    size = outerSize,
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }

            // 中心数字
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${healthPercent}%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor,
                    letterSpacing = (-1).sp
                )
                Text(
                    "综合健康度",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 底部 3 个小指标
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniMetricChip(
                value = onlineCount.toString(),
                label = "实例在线",
                color = if (onlineCount > 0) StatusGreen else StatusRed,
                onClick = onInstanceClick
            )
            MiniMetricChip(
                value = activeTaskCount.toString(),
                label = "任务执行",
                color = if (activeTaskCount > 0) StatusBlue else TextDim,
                onClick = onTaskClick
            )
            MiniMetricChip(
                value = alertCount.toString(),
                label = "待处理",
                color = when {
                    hasUrgentAlert -> StatusRed
                    alertCount > 0 -> StatusYellow
                    else -> StatusGreen
                },
                onClick = onAlertClick
            )
        }
    }
}

@Composable
private fun MiniMetricChip(
    value: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = BgCard.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ==================== 告警横幅 ====================

@Composable
private fun AlertBanner(alert: Alert) {
    val bgColor = when (alert.level) {
        AlertLevel.CRITICAL -> StatusRedDim
        AlertLevel.WARNING -> StatusYellowDim
        AlertLevel.INFO -> StatusBlueDim
    }
    val textColor = when (alert.level) {
        AlertLevel.CRITICAL -> StatusRed
        AlertLevel.WARNING -> StatusYellow
        AlertLevel.INFO -> StatusBlue
    }
    val icon = when (alert.level) {
        AlertLevel.CRITICAL -> "❌"
        AlertLevel.WARNING -> "⚠️"
        AlertLevel.INFO -> "ℹ️"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(0.5.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = bgColor.copy(alpha = 0.5f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                alert.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatTimeAgo(alert.timestamp),
                fontSize = 11.sp,
                color = TextDim
            )
        }
    }
}

// ==================== 实例条 — HUD 毛玻璃卡片 + 小圆环 ====================

@Composable
private fun InstanceStrip(
    healthMap: Map<String, InstanceHealth>,
    connectedInstances: Map<String, Boolean>,
    instanceNames: Map<String, String>,
    instanceEmojis: Map<String, String>,
    onInstanceClick: (String) -> Unit
) {
    val allIds = (healthMap.keys + connectedInstances.keys).distinct()

    if (allIds.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无实例连接", fontSize = 13.sp, color = TextDim)
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        allIds.forEach { id ->
            val health = healthMap[id]
            val isConnected = connectedInstances[id] ?: false
            val name = instanceNames[id] ?: id
            val emoji = instanceEmojis[id] ?: "🤖"

            InstanceChip(
                name = name,
                emoji = emoji,
                isConnected = isConnected,
                health = health,
                onClick = { onInstanceClick(id) }
            )
        }
    }
}

@Composable
private fun InstanceChip(
    name: String,
    emoji: String,
    isConnected: Boolean,
    health: InstanceHealth?,
    onClick: () -> Unit
) {
    // 实例专属颜色
    val instanceColor = getInstanceColor(name, name)
    
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .border(0.5.dp, instanceColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        // 在线实例有微妙的专属色底色调
        val bgTint = if (isConnected) instanceColor.copy(alpha = 0.02f) else Color.Transparent
        Column(
            modifier = Modifier
                .background(bgTint)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = instanceColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) StatusGreen else TextDim)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (health != null) {
                // 小圆环 gauge 代替文字+横条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniGauge(
                        label = "CPU",
                        percent = health.cpuPercent,
                        color = if (health.cpuPercent > 80) StatusRed else StatusGreen
                    )
                    MiniGauge(
                        label = "MEM",
                        percent = (health.memoryUsedGb / 16f * 100f).coerceIn(0f, 100f),
                        color = StatusBlue
                    )
                    MiniGauge(
                        label = "DISK",
                        percent = health.diskPercent,
                        color = when {
                            health.diskPercent > 85 -> StatusRed
                            health.diskPercent > 60 -> StatusYellow
                            else -> StatusGreen
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniGauge(label = "CPU", percent = 0f, color = TextDim)
                    MiniGauge(label = "MEM", percent = 0f, color = TextDim)
                    MiniGauge(label = "DISK", percent = 0f, color = TextDim)
                }
            }
        }
    }
}

@Composable
private fun MiniGauge(label: String, percent: Float, color: Color) {
    val sweepAngle = (percent / 100f * 270f).coerceIn(0f, 270f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3f
                val pad = stroke / 2 + 4f
                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                val topLeft = Offset(pad, pad)

                // 轨道
                drawArc(
                    color = GaugeTrack,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // 进度
                if (sweepAngle > 0f) {
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                "${percent.toInt()}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(label, fontSize = 8.sp, color = TextDim, letterSpacing = 0.5.sp)
    }
}

// ==================== 活跃任务 ====================

@Composable
private fun TaskStream(
    tasks: List<SubAgentTask>,
    instanceNames: Map<String, String>
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tasks.forEach { task ->
            TaskCard(task = task, instanceName = instanceNames[task.instanceId] ?: task.instanceId)
        }
    }
}

@Composable
private fun TaskCard(task: SubAgentTask, instanceName: String) {
    val isFinished = task.status == TaskStatus.COMPLETED || task.status == TaskStatus.FAILED

    // 左侧状态色条 + HUD 毛玻璃卡片
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFinished) 0.6f else 1f)
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        Row {
            // 左侧 4dp 状态色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        when (task.status) {
                            TaskStatus.RUNNING -> StatusBlue
                            TaskStatus.COMPLETED -> StatusGreen
                            TaskStatus.FAILED -> StatusRed
                            TaskStatus.PENDING -> TextDim
                        }
                    )
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    val iconBg = when (task.status) {
                        TaskStatus.RUNNING -> Brush.linearGradient(listOf(StatusBlue, StatusPurple))
                        TaskStatus.COMPLETED -> Brush.linearGradient(listOf(StatusGreenDim, StatusGreenDim))
                        TaskStatus.FAILED -> Brush.linearGradient(listOf(StatusRedDim, StatusRedDim))
                        TaskStatus.PENDING -> Brush.linearGradient(listOf(BgSurface, BgSurface))
                    }
                    val taskEmoji = when (task.status) {
                        TaskStatus.RUNNING -> "⚡"
                        TaskStatus.COMPLETED -> "✅"
                        TaskStatus.FAILED -> "❌"
                        TaskStatus.PENDING -> "⏳"
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(taskEmoji, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                task.label.ifEmpty { "未命名任务" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGlow
                            ) {
                                Text(
                                    instanceName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        val durationText = formatDuration(task.duration)
                        val statusText = when (task.status) {
                            TaskStatus.RUNNING -> "运行 $durationText"
                            TaskStatus.COMPLETED -> "完成 · 耗时 $durationText"
                            TaskStatus.FAILED -> "失败 · 耗时 $durationText"
                            TaskStatus.PENDING -> "等待中"
                        }

                        Text(
                            "${task.id} · $statusText",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Progress bar — accent 渐变
                if (task.status == TaskStatus.RUNNING) {
                    Spacer(modifier = Modifier.height(10.dp))

                    if (task.progress != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BgDeep)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(task.progress / 100f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Accent.copy(alpha = 0.6f), Accent)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("执行中...", fontSize = 11.sp, color = TextDim)
                            Text("${task.progress}%", fontSize = 11.sp, color = TextDim)
                        }
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Accent,
                            trackColor = BgDeep
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("进行中...", fontSize = 11.sp, color = TextDim)
                    }
                }
            }
        }
    }
}

// ==================== 快捷指令 — HUD 风格 ====================

@Composable
private fun QuickActions(
    onDispatch: () -> Unit,
    onSearch: () -> Unit,
    onSummary: () -> Unit,
    onHealthCheck: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickButton("🚀", "派发任务", Modifier.weight(1f), onDispatch)
        QuickButton("🔍", "搜记忆", Modifier.weight(1f), onSearch)
        QuickButton("📊", "每日总结", Modifier.weight(1f), onSummary)
        QuickButton("🛠️", "全体自检", Modifier.weight(1f), onHealthCheck)
    }
}

@Composable
private fun QuickButton(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 2.dp
    ) {
        Box {
            // HUD 微光叠加
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(WarmGlow.copy(alpha = 0.06f), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            }
        }
    }
}

// ==================== 时间线 ====================

@Composable
private fun Timeline(events: List<TimelineEvent>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        events.forEachIndexed { index, event ->
            TimelineItem(
                event = event,
                isLast = index == events.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineItem(event: TimelineEvent, isLast: Boolean) {
    val dotColor = when (event.eventType) {
        "task_completed" -> StatusGreen
        "task_failed" -> StatusRed
        "deploy" -> StatusBlue
        "alert" -> StatusYellow
        "security" -> StatusBlue
        else -> TextDim
    }
    val dotEmoji = when (event.eventType) {
        "task_completed" -> "✅"
        "task_failed" -> "❌"
        "deploy" -> "🚀"
        "alert" -> "⚠️"
        "security" -> "🔒"
        "idle" -> "💤"
        else -> "●"
    }

    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(0.5.dp, dotColor.copy(alpha = 0.3f), CircleShape)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(dotEmoji, fontSize = 11.sp)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(WarmGlow.copy(alpha = 0.08f))
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    if (event.instanceName.isNotEmpty()) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(event.instanceName)
                        }
                        append(" ")
                    }
                    append(event.title)
                },
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                formatTime(event.timestamp),
                fontSize = 11.sp,
                color = TextDim
            )
        }
    }
}

// ==================== 通用组件 ====================

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.8.sp
        )
        if (action != null && onAction != null) {
            Text(
                action,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Accent,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

// ==================== 工具函数 ====================

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        minutes < 1440 -> "${minutes / 60}小时前"
        else -> "${minutes / 1440}天前"
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m${seconds % 60}s"
        else -> "${seconds / 3600}h${(seconds % 3600) / 60}m"
    }
}
