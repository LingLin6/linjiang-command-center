package com.linjiang.command

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linjiang.command.ui.screens.ChatScreen
import com.linjiang.command.ui.screens.DashboardScreen
import com.linjiang.command.ui.screens.InstanceListScreen
import com.linjiang.command.ui.screens.MemoryScreen
import com.linjiang.command.ui.screens.SettingsScreen
import com.linjiang.command.ui.screens.TaskScreen
import com.linjiang.command.ui.screens.WelcomeScreen
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.ChatViewModel
import com.linjiang.command.viewmodel.DashboardViewModel
import com.linjiang.command.viewmodel.InstanceViewModel
import com.linjiang.command.viewmodel.MemoryViewModel

/**
 * 主活动
 * 
 * 5 Tab 导航：态势 / 任务 / 对话 / 记忆 / 设置
 * 默认首页是态势大盘，不是聊天框。
 */
class MainActivity : ComponentActivity() {
    
    private var chatViewModel: ChatViewModel? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinjiangCommandTheme {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("linjiang_command", Context.MODE_PRIVATE)
                var isAuthenticated by remember {
                    mutableStateOf(prefs.getString("access_code", null) != null)
                }

                if (isAuthenticated) {
                    MainScreen(
                        onChatViewModelReady = { chatViewModel = it },
                        onLogout = {
                            prefs.edit().remove("access_code").apply()
                            isAuthenticated = false
                        }
                    )
                } else {
                    WelcomeScreen(onAuthenticated = { code ->
                        prefs.edit().putString("access_code", code).apply()
                        isAuthenticated = true
                    })
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        chatViewModel?.isAppInForeground = true
    }
    
    override fun onPause() {
        super.onPause()
        chatViewModel?.isAppInForeground = false
    }
}

/**
 * Tab 定义
 */
private enum class MainTab(val icon: String, val label: String) {
    DASHBOARD("📡", "态势"),
    TASKS("📋", "任务"),
    CHAT("💬", "对话"),
    MEMORY("🧠", "记忆"),
    SETTINGS("⚙️", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onChatViewModelReady: (ChatViewModel) -> Unit = {}, onLogout: () -> Unit = {}) {
    val instanceViewModel: InstanceViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val memoryViewModel: MemoryViewModel = viewModel()
    
    LaunchedEffect(chatViewModel) {
        onChatViewModelReady(chatViewModel)
    }
    
    val instances by instanceViewModel.instances.collectAsState()
    
    LaunchedEffect(instances) {
        val connectedCount = instances.count { inst ->
            instanceViewModel.getRelayClient(inst.id)?.isConnected?.value == true
        }
        dashboardViewModel.updateOnlineCount(connectedCount)
    }
    
    instances.forEach { inst ->
        val client = instanceViewModel.getRelayClient(inst.id)
        if (client != null) {
            LaunchedEffect(client) {
                client.instanceHealthMap.collect { healthMap ->
                    healthMap.forEach { (_, health) ->
                        dashboardViewModel.updateHealth(health)
                    }
                }
            }
            LaunchedEffect(client) {
                client.alerts.collect { alertList ->
                    dashboardViewModel.updateAlerts(alertList)
                }
            }
            LaunchedEffect(client) {
                client.timelineEvents.collect { events ->
                    dashboardViewModel.updateTimelineEvents(events)
                }
            }
            LaunchedEffect(client) {
                client.subAgentTasks.collect { tasks ->
                    dashboardViewModel.updateSubAgentTasks(tasks)
                }
            }
            LaunchedEffect(client) {
                client.taskLogs.collect { logs ->
                    dashboardViewModel.updateTaskLogs(logs)
                }
            }
            LaunchedEffect(client) {
                client.isConnected.collect {
                    val count = instances.count { inst2 ->
                        instanceViewModel.getRelayClient(inst2.id)?.isConnected?.value == true
                    }
                    dashboardViewModel.updateOnlineCount(count)
                }
            }
        }
    }
    
    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    
    val connectedMap = remember(instances) {
        instances.associate { it.id to (instanceViewModel.getRelayClient(it.id)?.isConnected?.value == true) }
    }
    val nameMap = remember(instances) {
        instances.associate { it.id to it.name }
    }
    val emojiMap = remember(instances) {
        instances.associate { it.id to it.emoji }
    }
    
    Scaffold(
        containerColor = BgDeep,
        bottomBar = {
            // 科幻底部导航栏 — 毛玻璃效果，微渐变顶部分隔
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgNavBar)
            ) {
                // 顶部 HUD 青蓝微发光分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, WarmGlow.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )
                
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = TextDim,
                    tonalElevation = 0.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val isChat = tab == MainTab.CHAT
                        val isSelected = selectedTab == tab
                        
                        NavigationBarItem(
                            icon = {
                                if (isChat) {
                                    // 对话按钮凸起 8dp + accent 阴影
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.offset(y = (-8).dp)
                                    ) {
                                        // 微妙的 accent 阴影
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Accent.copy(alpha = 0.1f))
                                        )
                                        // 主按钮
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .shadow(6.dp, CircleShape, ambientColor = Accent.copy(alpha = 0.3f), spotColor = Accent.copy(alpha = 0.3f))
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Accent, Accent.copy(alpha = 0.85f))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tab.icon, fontSize = 18.sp)
                                        }
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box {
                                            Text(
                                                tab.icon,
                                                fontSize = 18.sp,
                                                color = if (isSelected) Accent else TextDim
                                            )
                                            // 任务 badge
                                            if (tab == MainTab.TASKS) {
                                                val taskCount = dashboardViewModel.subAgentTasks.collectAsState().value
                                                    .count { it.status == com.linjiang.command.data.model.TaskStatus.RUNNING }
                                                if (taskCount > 0) {
                                                    Badge(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 8.dp, y = (-4).dp),
                                                        containerColor = StatusRed
                                                    ) {
                                                        Text(
                                                            taskCount.toString(),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        // 选中 Tab 下方光点 — accent 色 4dp 呼吸动画
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            BreathingAccentDot()
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Accent else TextDim
                                )
                            },
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        connectedInstances = connectedMap,
                        instanceNames = nameMap,
                        instanceEmojis = emojiMap,
                        onInstanceClick = { instanceId ->
                            instances.find { it.id == instanceId }?.let { inst ->
                                chatViewModel.setCurrentInstance(inst)
                                val client = instanceViewModel.getRelayClient(inst.id)
                                chatViewModel.setRelayClient(client)
                                selectedTab = MainTab.CHAT
                            }
                        },
                        onNavigateToTasks = { selectedTab = MainTab.TASKS },
                        onNavigateToChat = { selectedTab = MainTab.CHAT },
                        onNavigateToSettings = { selectedTab = MainTab.SETTINGS }
                    )
                }
                MainTab.TASKS -> {
                    val tasks by dashboardViewModel.subAgentTasks.collectAsState()
                    val taskLogs by dashboardViewModel.taskLogs.collectAsState()
                    TaskScreen(
                        tasks = tasks,
                        instances = instances,
                        instanceNames = nameMap,
                        taskLogs = taskLogs,
                        onDispatchTask = { instanceId, template, prompt, timeoutMin ->
                            val client = instanceViewModel.getRelayClient(instanceId)
                            if (client != null) {
                                val taskId = "task-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
                                client.dispatchTask(
                                    targetInstanceId = null,
                                    taskId = taskId,
                                    template = template,
                                    prompt = prompt,
                                    timeout = timeoutMin * 60,
                                    priority = "normal"
                                )
                            } else {
                                val connectedClient = instances
                                    .mapNotNull { inst -> instanceViewModel.getRelayClient(inst.id)?.let { inst.id to it } }
                                    .firstOrNull { it.second.isConnected.value }
                                
                                if (connectedClient != null) {
                                    val taskId = "task-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
                                    connectedClient.second.dispatchTask(
                                        targetInstanceId = null,
                                        taskId = taskId,
                                        template = template,
                                        prompt = prompt,
                                        timeout = timeoutMin * 60,
                                        priority = "normal"
                                    )
                                }
                            }
                        },
                        onKillTask = { taskId ->
                            val task = tasks.find { it.id == taskId }
                            val client = if (task != null) {
                                instances
                                    .mapNotNull { inst -> instanceViewModel.getRelayClient(inst.id)?.let { inst.id to it } }
                                    .firstOrNull { it.second.isConnected.value }
                                    ?.second
                            } else null
                            
                            client?.cancelTask(taskId)
                        }
                    )
                }
                MainTab.CHAT -> {
                    ChatScreen(viewModel = chatViewModel)
                }
                MainTab.MEMORY -> {
                    instances.forEach { inst ->
                        val client = instanceViewModel.getRelayClient(inst.id)
                        if (client != null) {
                            LaunchedEffect(client) {
                                client.memoryResult.collect { result ->
                                    if (result != null) {
                                        if (result.startsWith("ERROR:")) {
                                            memoryViewModel.handleSearchError(result.removePrefix("ERROR:"))
                                        } else {
                                            memoryViewModel.handleMemoryResult(result)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    MemoryScreen(
                        viewModel = memoryViewModel,
                        onSearch = { query ->
                            memoryViewModel.setSearching()
                            val connectedClient = instances
                                .mapNotNull { inst -> instanceViewModel.getRelayClient(inst.id)?.let { inst.id to it } }
                                .firstOrNull { it.second.isConnected.value }
                            
                            if (connectedClient != null) {
                                connectedClient.second.searchMemory(query)
                            } else {
                                memoryViewModel.handleSearchError("没有已连接的实例")
                            }
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(instanceViewModel = instanceViewModel, onLogout = onLogout)
                }
            }
        }
    }
}

/**
 * 选中 Tab 下方呼吸动画光点 — 青蓝色 4dp（HUD 风格）
 */
@Composable
private fun BreathingAccentDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "tabDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = alpha))
    )
}

@Composable
private fun PlaceholderScreen(icon: String, title: String, desc: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 20.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, fontSize = 14.sp, color = TextDim)
        }
    }
}
