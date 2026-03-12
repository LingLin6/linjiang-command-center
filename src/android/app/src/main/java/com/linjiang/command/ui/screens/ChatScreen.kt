package com.linjiang.command.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.linjiang.command.data.HealthAlert
import com.linjiang.command.data.model.AiThinkingState
import com.linjiang.command.data.model.Message
import com.linjiang.command.data.model.MessageLifeState
import com.linjiang.command.data.model.MessageProcessContext
import com.linjiang.command.data.model.MessageType
import com.linjiang.command.data.model.ProcessPhase
import com.linjiang.command.data.model.ToolCallRecord
import com.linjiang.command.data.model.ToolStatus
import com.linjiang.command.data.model.SubAgent
import com.linjiang.command.data.model.toDisplayText
import com.linjiang.command.data.model.toIcon
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 对话界面 — HUD 指挥中心科幻风格 v4.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val subAgents by viewModel.subAgents.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val healthAlerts by viewModel.healthAlerts.collectAsState()
    val instances by viewModel.instances.collectAsState()
    val aiThinkingState by viewModel.aiThinkingState.collectAsState()
    val instanceName by viewModel.currentInstanceName.collectAsState()
    val instanceEmoji by viewModel.currentInstanceEmoji.collectAsState()
    val activeProcess by viewModel.activeProcess.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var showSubAgentsSheet by remember { mutableStateOf(false) }
    var showInstanceMenu by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 自动滚动到底部
    val lastMessageContent = messages.lastOrNull()?.content
    LaunchedEffect(messages.size, lastMessageContent) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // 顶部栏 — 毛玻璃质感，底部微渐变替代硬线
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgCard.copy(alpha = 0.85f),
            shadowElevation = 4.dp
        ) {
            // 底部微渐变分隔
            Box {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 实例选择按钮
                    Box {
                        TextButton(onClick = { showInstanceMenu = true }) {
                            val selectedInstance = viewModel.getSelectedInstance()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedInstance?.online == true) "🟢" else "🔴",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedInstance?.getDisplayName() ?: "未连接",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showInstanceMenu,
                            onDismissRequest = { showInstanceMenu = false }
                        ) {
                            if (instances.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("暂无可用实例") },
                                    onClick = { }
                                )
                            } else {
                                instances.forEach { instance ->
                                    val instColor = getInstanceColor(instance.id, instance.name)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (instance.online) "🟢" else "🔴",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    instance.getDisplayName(),
                                                    color = instColor
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectInstance(instance.id)
                                            showInstanceMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Row {
                        IconButton(onClick = { viewModel.refreshSubAgents() }) {
                            Icon(Icons.Default.Refresh, "刷新 Sub-agents", modifier = Modifier.size(20.dp), tint = TextSecondary)
                        }
                        
                        IconButton(onClick = { showSubAgentsSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (subAgents.isNotEmpty()) {
                                        Badge(containerColor = Accent) { Text("${subAgents.size}", color = TextPrimary) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.List, "Sub-agents", tint = TextSecondary)
                            }
                        }
                    }
                }
                // 底部 HUD 装饰线（青蓝微发光）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, WarmGlow.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )
            }
        }
        
        // Sub-agents 底部面板
        if (showSubAgentsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubAgentsSheet = false },
                containerColor = BgElevated,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sub-agents (${subAgents.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { viewModel.refreshSubAgents() }) {
                            Icon(Icons.Default.Refresh, "刷新")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (subAgents.isEmpty()) {
                        Text(
                            text = "暂无运行中的 sub-agent\n点击右上角 🔄 刷新",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDim
                        )
                    } else {
                        LazyColumn {
                            items(subAgents) { agent ->
                                SubAgentCard(agent)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // 健康警告卡片
        healthAlerts.lastOrNull()?.let { alert ->
            HealthAlertCard(alert = alert)
        }
        
        // 消息列表 — 增加间距到 12dp
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BgDeep)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (messages.isEmpty() && notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "开始对话吧 💬",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextDim
                        )
                    }
                }
            } else {
                val allItems = (messages.map { ItemWrapper.MessageItem(it) } + 
                               notifications.map { ItemWrapper.NotificationItem(it) })
                    .sortedBy { 
                        when (it) {
                            is ItemWrapper.MessageItem -> it.message.timestamp
                            is ItemWrapper.NotificationItem -> it.notification.timestamp
                        }
                    }
                
                items(allItems) { item ->
                    when (item) {
                        is ItemWrapper.MessageItem -> MessageBubble(
                            message = item.message,
                            instanceName = instanceName,
                            instanceEmoji = instanceEmoji,
                            onRetry = { viewModel.retryMessage(item.message.id) }
                        )
                        is ItemWrapper.NotificationItem -> NotificationCard(notification = item.notification)
                    }
                }
            }
            
            // 流式过程 UI
            activeProcess?.let { process ->
                if (process.phase != ProcessPhase.IDLE && process.phase != ProcessPhase.COMPLETE) {
                    item {
                        ProcessStatusCard(
                            process = process,
                            onRetry = { viewModel.sendMessage(messages.lastOrNull { it.type == MessageType.USER }?.content ?: "") }
                        )
                    }
                }
            }
        }
        
        // AI 思考状态栏
        AnimatedVisibility(
            visible = aiThinkingState != AiThinkingState.IDLE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AiThinkingBar(
                state = aiThinkingState,
                instanceName = instanceName,
                instanceEmoji = instanceEmoji
            )
        }
        
        // 快捷指令栏 — pill 形状，毛玻璃底色
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgSurface.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCommandChip("📊 状态") { viewModel.sendMessage("/status") }
                QuickCommandChip("🧠 记忆") { viewModel.sendMessage("最近在做什么？") }
                QuickCommandChip("📋 待办") { viewModel.sendMessage("看看待办事项") }
                QuickCommandChip("🔍 诊断") { viewModel.sendMessage("跑一下系统自检") }
            }
        }
        
        // 输入框 — 毛玻璃背景 + 柔和边框
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgCard.copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            // 顶部微渐变分隔
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, WarmGlow.copy(alpha = 0.10f), Color.Transparent)
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...", color = TextDim) },
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = WarmGlow.copy(alpha = 0.4f),
                            unfocusedBorderColor = WarmGlowBorder,
                            cursorColor = Accent,
                            focusedContainerColor = BgSurface,
                            unfocusedContainerColor = BgSurface
                        )
                    )
                    
                    // 发送按钮 — 绛色
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && instances.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            disabledContainerColor = Accent.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("发送", color = TextPrimary)
                    }
                }
            }
        }
    }
}

/**
 * AI 思考状态栏
 */
@Composable
fun AiThinkingBar(state: AiThinkingState, instanceName: String, instanceEmoji: String) {
    val displayText = state.toDisplayText(instanceName, instanceEmoji)
    
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThinkingDots()
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = alpha)
            )
        }
    }
}

/**
 * 跳动的三个点动画 — accent 色脉冲
 */
@Composable
fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 0), repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 133), repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 266), repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )
    
    // accent 色脉冲动画
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600), repeatMode = RepeatMode.Reverse
        ), label = "dotAlpha"
    )
    
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(dot1Offset, dot2Offset, dot3Offset).forEach { offset ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offset.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(50),
                    color = Accent.copy(alpha = dotAlpha)
                ) {}
            }
        }
    }
}

/**
 * 消息气泡组件 — 毛玻璃科幻风格
 */
@Composable
fun MessageBubble(
    message: Message,
    instanceName: String,
    instanceEmoji: String,
    onRetry: () -> Unit = {}
) {
    val isUser = message.type == MessageType.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val backgroundColor = when (message.type) {
        MessageType.USER -> {
            if (message.lifeState == MessageLifeState.LOST) UserMessageBg.copy(alpha = 0.5f)
            else UserMessageBg
        }
        MessageType.ASSISTANT -> AssistantMessageBg
        MessageType.SYSTEM -> SystemMessageBg
    }
    val bubbleBorderColor = when (message.type) {
        MessageType.USER -> WarmGlow.copy(alpha = 0.12f)       // 暖金 12% 边框
        MessageType.ASSISTANT -> WarmGlow.copy(alpha = 0.10f)  // 暖金 10% 边框
        MessageType.SYSTEM -> WarmGlow.copy(alpha = 0.05f)
    }
    val textColor = TextPrimary
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // 发送者标签
        Text(
            text = when (message.type) {
                MessageType.USER -> "你"
                MessageType.ASSISTANT -> message.from.ifBlank { "$instanceEmoji $instanceName" }
                MessageType.SYSTEM -> "系统"
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        
        // 消息内容 — 毛玻璃气泡，圆角 18dp
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = backgroundColor,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, bubbleBorderColor),
            shadowElevation = if (isUser) 4.dp else 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isStreaming && message.content.isEmpty()) {
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                    val cursorAlpha by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500), repeatMode = RepeatMode.Reverse
                        ), label = "cursorAlpha"
                    )
                    Text(
                        text = "▌",
                        color = textColor.copy(alpha = cursorAlpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = message.content + if (message.isStreaming) " ▌" else "",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 底部状态栏
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim
                    )
                    
                    if (isUser && message.lifeState != MessageLifeState.COMPLETE) {
                        val statusText = message.lifeState.toDisplayText()
                        
                        when (message.lifeState) {
                            MessageLifeState.BORN, MessageLifeState.FLYING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Accent.copy(alpha = 0.7f)
                                )
                                Text(statusText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            MessageLifeState.RECEIVED -> {
                                val statusIcon = message.lifeState.toIcon()
                                Text("$statusIcon $statusText", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            MessageLifeState.LOST -> {
                                Text("⚠️ $statusText", style = MaterialTheme.typography.labelSmall, color = StatusRed)
                            }
                            else -> {}
                        }
                    }
                    
                    if (isUser && message.lifeState == MessageLifeState.COMPLETE) {
                        Text("✓", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
        
        // 重试按钮
        if (isUser && message.lifeState == MessageLifeState.LOST) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("🔄 重新发送", style = MaterialTheme.typography.labelSmall, color = Accent)
            }
        }
    }
}

/**
 * Sub-agent 卡片 — 毛玻璃风格
 */
@Composable
fun SubAgentCard(agent: SubAgent) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = WarmGlowBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = when (agent.status) {
            "running" -> StatusBlueDim.copy(alpha = 0.5f)
            "completed" -> StatusGreenDim.copy(alpha = 0.5f)
            "failed" -> StatusRedDim.copy(alpha = 0.5f)
            else -> BgCard.copy(alpha = 0.85f)
        },
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = agent.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (agent.status) {
                            "running" -> Icons.Default.PlayArrow
                            "completed" -> Icons.Default.Check
                            "failed" -> Icons.Default.Close
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (agent.status) {
                            "running" -> StatusBlue
                            "completed" -> StatusGreen
                            "failed" -> StatusRed
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = agent.status, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = agent.sessionKey.take(80) + if (agent.sessionKey.length > 80) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
            Text(
                text = "更新: ${formatTimestamp(agent.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * 健康警告卡片 — 毛玻璃风格
 */
@Composable
fun HealthAlertCard(alert: HealthAlert) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = when (alert.severity) {
            "warning" -> StatusYellowDim.copy(alpha = 0.5f)
            "critical" -> StatusRedDim.copy(alpha = 0.5f)
            else -> BgCard.copy(alpha = 0.85f)
        },
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = when (alert.severity) {
                    "warning" -> StatusYellow
                    "critical" -> StatusRed
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = alert.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "${alert.metric}: ${alert.value}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

/**
 * 通知卡片 — 毛玻璃风格
 */
@Composable
fun NotificationCard(notification: com.linjiang.command.data.model.Notification) {
    val icon = when (notification.severity) {
        "success" -> "✅"
        "error" -> "❌"
        "warning" -> "⚠️"
        else -> "ℹ️"
    }
    val iconColor = when (notification.severity) {
        "success" -> StatusGreen
        "error" -> StatusRed
        "warning" -> StatusYellow
        else -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = iconColor.copy(alpha = 0.08f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Top))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = notification.title, style = MaterialTheme.typography.titleSmall, color = iconColor)
                Text(text = notification.message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(text = formatTimestamp(notification.timestamp), style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

private sealed class ItemWrapper {
    data class MessageItem(val message: Message) : ItemWrapper()
    data class NotificationItem(val notification: com.linjiang.command.data.model.Notification) : ItemWrapper()
}

/**
 * 快捷指令按钮 — pill 形状毛玻璃
 */
@Composable
fun QuickCommandChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(30.dp)
            .clickable { onClick() }
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.7f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ══════════════════════════════════════════
// 流式过程 UI 组件
// ══════════════════════════════════════════

/**
 * 过程状态卡片 — 根据 ProcessPhase 显示不同 UI
 */
@Composable
fun ProcessStatusCard(
    process: MessageProcessContext,
    onRetry: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 超时警告横幅（在任意阶段都可能出现）
        val showTimeoutWarning = process.timeoutLimit > 0 &&
            process.elapsedSeconds > (process.timeoutLimit * 0.8).toInt() &&
            process.phase != ProcessPhase.TIMEOUT &&
            process.phase != ProcessPhase.ERROR
        if (showTimeoutWarning) {
            TimeoutBanner(
                elapsed = process.elapsedSeconds,
                isTimeout = false,
                onRetry = {}
            )
        }

        when (process.phase) {
            ProcessPhase.THINKING -> ThinkingCard(thinkingText = process.thinkingText)
            ProcessPhase.TOOL_CALLING -> ToolCallingCards(toolCalls = process.toolCalls)
            ProcessPhase.STREAMING -> { /* 流式文本已由现有 stream_chunk 机制处理 */ }
            ProcessPhase.TIMEOUT -> TimeoutBanner(
                elapsed = process.elapsedSeconds,
                isTimeout = true,
                onRetry = onRetry
            )
            ProcessPhase.ERROR -> ErrorCard(onRetry = onRetry)
            else -> {}
        }
    }
}

/**
 * 思考动画卡片 — 🧠 脉冲点 + 可选思考内容
 */
@Composable
private fun ThinkingCard(thinkingText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "process_thinking")

    // 3 个点的呼吸脉冲，各自错开
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0), repeatMode = RepeatMode.Reverse
        ), label = "ptd1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse
        ), label = "ptd2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse
        ), label = "ptd3"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = BgCard
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🧠", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "正在思考",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGlow,
                    fontWeight = FontWeight.Medium
                )
                // 3 个脉冲点
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(WarmGlow.copy(alpha = alpha))
                        )
                    }
                }
            }

            // 思考内容（如果有）
            if (thinkingText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = thinkingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 工具调用卡片列表
 */
@Composable
private fun ToolCallingCards(toolCalls: List<ToolCallRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        toolCalls.forEach { toolCall ->
            ToolCallCard(toolCall = toolCall)
        }
    }
}

/**
 * 单个工具调用卡片 — 左侧状态色条 + 工具名 + 状态指示
 */
@Composable
private fun ToolCallCard(toolCall: ToolCallRecord) {
    val statusColor = when (toolCall.status) {
        ToolStatus.RUNNING -> StatusBlue
        ToolStatus.SUCCESS -> StatusGreen
        ToolStatus.FAILED -> StatusRed
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = BgCard
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧状态色条
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(statusColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )

            Column(modifier = Modifier.padding(10.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：工具图标 + 名称
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🔧", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = toolCall.tool,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }

                    // 右侧：状态指示
                    when (toolCall.status) {
                        ToolStatus.RUNNING -> {
                            val rotationAngle by rememberInfiniteTransition(label = "tool_spin_${toolCall.callId}")
                                .animateFloat(
                                    initialValue = 0f, targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing)
                                    ), label = "spin_${toolCall.callId}"
                                )
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp).rotate(rotationAngle),
                                strokeWidth = 1.5.dp,
                                color = StatusBlue
                            )
                        }
                        ToolStatus.SUCCESS -> {
                            Text(text = "✅", style = MaterialTheme.typography.bodySmall)
                        }
                        ToolStatus.FAILED -> {
                            Text(text = "❌", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 摘要（如果有）
                if (!toolCall.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = toolCall.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * 超时/警告横幅
 */
@Composable
private fun TimeoutBanner(
    elapsed: Int,
    isTimeout: Boolean,
    onRetry: () -> Unit
) {
    val bgColor = if (isTimeout) StatusRedDim else StatusYellowDim
    val textColor = if (isTimeout) StatusRed else StatusYellow
    val icon = if (isTimeout) "⏰" else "⏳"
    val message = if (isTimeout) "任务超时" else "任务较大，已执行 ${elapsed}s"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isTimeout) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                ) {
                    Text("重试", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * 错误状态卡片
 */
@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = StatusRedDim
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "❌", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "处理出错",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusRed,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = Accent)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
