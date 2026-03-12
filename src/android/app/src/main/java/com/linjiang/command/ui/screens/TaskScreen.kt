package com.linjiang.command.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.linjiang.command.data.model.OpenClawInstance
import com.linjiang.command.data.model.SubAgentTask
import com.linjiang.command.data.model.TaskStatus
import com.linjiang.command.ui.theme.*

/**
 * 任务模板
 */
data class TaskTemplate(
    val icon: String,
    val name: String,
    val prompt: String,
    val timeoutMinutes: Int
)

private val TASK_TEMPLATES = listOf(
    TaskTemplate("🔍", "代码审查", "审查最近的代码变更，检查质量和安全问题", 10),
    TaskTemplate("🧪", "测试", "执行测试套件，报告结果", 15),
    TaskTemplate("📝", "文档", "更新项目文档，确保最新", 10),
    TaskTemplate("🛠️", "自检", "执行系统自检：磁盘/内存/API余额/服务状态", 5),
    TaskTemplate("🚀", "部署", "构建并部署最新版本", 20),
    TaskTemplate("✏️", "自定义", "", 10)
)

/**
 * 筛选 Tab
 */
private enum class TaskFilter(val label: String) {
    ALL("全部"),
    RUNNING("执行中"),
    COMPLETED("已完成"),
    FAILED("失败")
}

/**
 * 任务页 — HUD 指挥中心科幻风格 v4.0
 */
@Composable
fun TaskScreen(
    tasks: List<SubAgentTask>,
    instances: List<OpenClawInstance>,
    instanceNames: Map<String, String> = emptyMap(),
    taskLogs: Map<String, List<String>> = emptyMap(),
    onDispatchTask: (instanceId: String, template: String, prompt: String, timeoutMin: Int) -> Unit = { _, _, _, _ -> },
    onKillTask: (taskId: String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showDispatchDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<SubAgentTask?>(null) }

    val filteredTasks = when (selectedFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.RUNNING -> tasks.filter { it.status == TaskStatus.RUNNING || it.status == TaskStatus.PENDING }
        TaskFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
        TaskFilter.FAILED -> tasks.filter { it.status == TaskStatus.FAILED }
    }

    // 如果有选中的任务，显示详情页
    if (selectedTask != null) {
        val currentTask = tasks.find { it.id == selectedTask!!.id } ?: selectedTask!!
        val currentLogs = taskLogs[currentTask.id] ?: emptyList()
        TaskDetailScreen(
            task = currentTask,
            instanceName = instanceNames[currentTask.instanceId] ?: currentTask.instanceId,
            logs = currentLogs,
            onBack = { selectedTask = null },
            onKill = {
                onKillTask(currentTask.id)
                selectedTask = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("任务", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Accent,
                modifier = Modifier.clickable { showDispatchDialog = true }
            ) {
                Text(
                    "+ 派发",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 筛选 Tab — pill 形状，选中态青蓝微发光底 + accent 文字
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val count = when (filter) {
                    TaskFilter.ALL -> tasks.size
                    TaskFilter.RUNNING -> tasks.count { it.status == TaskStatus.RUNNING || it.status == TaskStatus.PENDING }
                    TaskFilter.COMPLETED -> tasks.count { it.status == TaskStatus.COMPLETED }
                    TaskFilter.FAILED -> tasks.count { it.status == TaskStatus.FAILED }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) WarmGlowDim.copy(alpha = 0.4f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isSelected) WarmGlow.copy(alpha = 0.25f) else WarmGlowBorder
                    ),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        if (count > 0) "${filter.label} $count" else filter.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Accent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 任务列表
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (tasks.isEmpty()) "暂无任务\n点击「+ 派发」创建第一个任务"
                        else "没有${selectedFilter.label}的任务",
                        fontSize = 14.sp,
                        color = TextDim,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskListCard(
                        task = task,
                        instanceName = instanceNames[task.instanceId] ?: task.instanceId,
                        onClick = { selectedTask = task }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // 派发 Dialog
    if (showDispatchDialog) {
        DispatchTaskDialog(
            instances = instances,
            onDismiss = { showDispatchDialog = false },
            onDispatch = { instanceId, template, prompt, timeoutMin ->
                onDispatchTask(instanceId, template, prompt, timeoutMin)
                showDispatchDialog = false
            }
        )
    }
}

// ==================== 任务卡片 — 左侧状态色条 + HUD 青蓝边框 ====================

@Composable
private fun TaskListCard(task: SubAgentTask, instanceName: String, onClick: () -> Unit = {}) {
    val isFinished = task.status == TaskStatus.COMPLETED || task.status == TaskStatus.FAILED

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFinished) 0.7f else 1f)
            .animateContentSize()
            .clickable { onClick() }
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

                        val durationText = formatTaskDuration(task.duration)
                        val statusText = when (task.status) {
                            TaskStatus.RUNNING -> "运行 $durationText"
                            TaskStatus.COMPLETED -> "完成 · 耗时 $durationText"
                            TaskStatus.FAILED -> "失败 · 耗时 $durationText"
                            TaskStatus.PENDING -> "等待中"
                        }
                        Text("${task.id} · $statusText", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                // 进度条 — accent 渐变，圆角 4dp
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
                                        Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.6f), Accent))
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

// ==================== 派发 Dialog — HUD 风格 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatchTaskDialog(
    instances: List<OpenClawInstance>,
    onDismiss: () -> Unit,
    onDispatch: (instanceId: String, template: String, prompt: String, timeoutMin: Int) -> Unit
) {
    var selectedInstance by remember { mutableStateOf(instances.firstOrNull()) }
    var selectedTemplate by remember { mutableStateOf<TaskTemplate?>(null) }
    var customPrompt by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val isCustom = selectedTemplate?.name == "自定义"
    val finalPrompt = if (isCustom) customPrompt else (selectedTemplate?.prompt ?: "")
    val canDispatch = selectedInstance != null && finalPrompt.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BgElevated,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, WarmGlowBorder),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("派发任务", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "✕",
                        fontSize = 18.sp,
                        color = TextDim,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 目标实例
                Text("目标实例", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedInstance?.let { "${it.emoji} ${it.name}" } ?: "选择实例",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BgSurface,
                            unfocusedContainerColor = BgSurface,
                            focusedBorderColor = WarmGlow.copy(alpha = 0.4f),
                            unfocusedBorderColor = WarmGlowBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        instances.forEach { inst ->
                            DropdownMenuItem(
                                text = { Text("${inst.emoji} ${inst.name}") },
                                onClick = {
                                    selectedInstance = inst
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 任务模板
                Text("任务模板", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                val rows = TASK_TEMPLATES.chunked(3)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { template ->
                            val isSelected = selectedTemplate == template
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTemplate = template },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AccentDim else BgCard.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) Accent.copy(alpha = 0.3f) else WarmGlowBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(template.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        template.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Accent else TextSecondary
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 自定义输入框
                if (isCustom) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("任务描述", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("描述你要执行的任务...", color = TextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BgSurface,
                            unfocusedContainerColor = BgSurface,
                            focusedBorderColor = Accent.copy(alpha = 0.3f),
                            unfocusedBorderColor = WarmGlowBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (selectedTemplate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgSurface.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, WarmGlowBorder)
                    ) {
                        Text(
                            selectedTemplate!!.prompt,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, WarmGlowBorder)
                    ) {
                        Text("取消", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            selectedInstance?.let { inst ->
                                val templateKey = when (selectedTemplate?.name) {
                                    "代码审查" -> "code_review"
                                    "测试" -> "test"
                                    "文档" -> "doc"
                                    "自检" -> "self_check"
                                    "部署" -> "deploy"
                                    "自定义" -> "custom"
                                    else -> "custom"
                                }
                                onDispatch(inst.id, templateKey, finalPrompt, selectedTemplate?.timeoutMinutes ?: 10)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = canDispatch,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            disabledContainerColor = Accent.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("🚀 派发")
                    }
                }
            }
        }
    }
}

private fun formatTaskDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m${seconds % 60}s"
        else -> "${seconds / 3600}h${(seconds % 3600) / 60}m"
    }
}
