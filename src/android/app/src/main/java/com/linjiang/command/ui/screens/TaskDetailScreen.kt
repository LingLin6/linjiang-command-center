package com.linjiang.command.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linjiang.command.data.model.SubAgentTask
import com.linjiang.command.data.model.TaskStatus
import com.linjiang.command.ui.theme.*

/**
 * 任务详情页 — HUD 指挥中心科幻风格 v4.0
 */
@Composable
fun TaskDetailScreen(
    task: SubAgentTask,
    instanceName: String,
    logs: List<String> = emptyList(),
    onBack: () -> Unit = {},
    onKill: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 20.sp, color = TextPrimary)
            }
            Text(
                task.label.ifEmpty { "任务详情" },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // 状态卡片 — 毛玻璃风格
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = BgCard.copy(alpha = 0.85f),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 状态行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusEmoji = when (task.status) {
                        TaskStatus.RUNNING -> "⚡"
                        TaskStatus.COMPLETED -> "✅"
                        TaskStatus.FAILED -> "❌"
                        TaskStatus.PENDING -> "⏳"
                    }
                    val statusText = when (task.status) {
                        TaskStatus.RUNNING -> "执行中"
                        TaskStatus.COMPLETED -> "已完成"
                        TaskStatus.FAILED -> "失败"
                        TaskStatus.PENDING -> "等待中"
                    }
                    val statusColor = when (task.status) {
                        TaskStatus.RUNNING -> StatusBlue
                        TaskStatus.COMPLETED -> StatusGreen
                        TaskStatus.FAILED -> StatusRed
                        TaskStatus.PENDING -> TextDim
                    }

                    Text(statusEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentGlow
                    ) {
                        Text(
                            instanceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailMetric("ID", task.id)
                    DetailMetric("运行时间", formatDetailDuration(task.duration))
                    if (task.progress != null) {
                        DetailMetric("进度", "${task.progress}%")
                    }
                }

                // 进度条 — accent 渐变，圆角 4dp
                if (task.status == TaskStatus.RUNNING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (task.progress != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
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
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Accent,
                            trackColor = BgDeep
                        )
                    }
                }
            }
        }

        // 实时日志（HUD 风格标题）
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左上角 HUD 装饰角标
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Transparent)
            ) {
                // 水平线
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(1.dp)
                        .align(Alignment.TopStart)
                        .background(WarmGlow.copy(alpha = 0.4f))
                )
                // 垂直线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(8.dp)
                        .align(Alignment.TopStart)
                        .background(WarmGlow.copy(alpha = 0.4f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "实时日志",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = BgDeep.copy(alpha = 0.8f)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("等待日志...", fontSize = 13.sp, color = TextDim)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { line ->
                        Text(
                            "> $line",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                line.contains("ERROR", true) || line.contains("FAIL", true) -> StatusRed
                                line.contains("PASS", true) || line.contains("SUCCESS", true) || line.contains("✅") -> StatusGreen
                                line.contains("WARN", true) || line.contains("⚠") -> StatusYellow
                                else -> TextSecondary
                            },
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 底部操作栏
        if (task.status == TaskStatus.RUNNING || task.status == TaskStatus.PENDING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusRed.copy(alpha = 0.1f),
                    onClick = onKill
                ) {
                    Text(
                        "⏹ 终止任务",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusRed,
                        modifier = Modifier.padding(vertical = 12.dp).wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Column {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextDim)
    }
}

private fun formatDetailDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
