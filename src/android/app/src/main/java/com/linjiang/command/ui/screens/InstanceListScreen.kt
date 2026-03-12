package com.linjiang.command.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linjiang.command.data.model.ConnectionStatus
import com.linjiang.command.data.model.OpenClawInstance
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.InstanceViewModel

/**
 * 实例列表界面 — 科幻毛玻璃风格 v3.0
 */
@Composable
fun InstanceListScreen(
    viewModel: InstanceViewModel,
    onInstanceSelected: (OpenClawInstance) -> Unit
) {
    val instances by viewModel.instances.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInstance by remember { mutableStateOf<OpenClawInstance?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "实例管理",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Surface(
                modifier = Modifier
                    .border(0.5.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Text(
                    "➕ 添加实例",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (instances.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无实例\n点击右上角添加",
                        fontSize = 14.sp,
                        color = TextDim,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(instances) { instance ->
                    InstanceCard(
                        instance = instance,
                        onConnect = { viewModel.connectInstance(instance.id) },
                        onDisconnect = { viewModel.disconnectInstance(instance.id) },
                        onDelete = { viewModel.removeInstance(instance.id) },
                        onEdit = { editingInstance = instance },
                        onSelect = { onInstanceSelected(instance) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddInstanceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                viewModel.addInstance(name, url)
                showAddDialog = false
            }
        )
    }
    
    editingInstance?.let { instance ->
        EditInstanceDialog(
            instance = instance,
            onDismiss = { editingInstance = null },
            onConfirm = { name, url ->
                viewModel.updateInstance(instance.id, name, url)
                editingInstance = null
            }
        )
    }
}

/**
 * 实例卡片 — 毛玻璃科幻风格
 */
@Composable
fun InstanceCard(
    instance: OpenClawInstance,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        // 在线实例微妙绿色底色调
        val bgTint = when (instance.status) {
            ConnectionStatus.CONNECTED -> StatusGreen.copy(alpha = 0.01f)
            else -> Color.Transparent
        }
        Column(
            modifier = Modifier
                .background(bgTint)
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (instance.status == ConnectionStatus.CONNECTED) {
                        BreathingDot(color = StatusGreen)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TextDim)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = instance.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                
                val statusText = when (instance.status) {
                    ConnectionStatus.CONNECTED -> "在线"
                    ConnectionStatus.CONNECTING -> "连接中"
                    ConnectionStatus.DISCONNECTED -> "离线"
                }
                val statusColor = when (instance.status) {
                    ConnectionStatus.CONNECTED -> StatusGreen
                    ConnectionStatus.CONNECTING -> StatusYellow
                    ConnectionStatus.DISCONNECTED -> TextDim
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = instance.url,
                fontSize = 12.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (instance.status == ConnectionStatus.CONNECTED) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { onDisconnect() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            "断开",
                            fontSize = 13.sp,
                            color = StatusRed,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect() },
                        shape = RoundedCornerShape(12.dp),
                        color = Accent
                    ) {
                        Text(
                            "对话",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable(enabled = instance.status != ConnectionStatus.CONNECTING) { onConnect() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (instance.status == ConnectionStatus.CONNECTING) 
                            Color.Transparent else Accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            if (instance.status == ConnectionStatus.CONNECTING) "连接中..." else "连接",
                            fontSize = 13.sp,
                            color = Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onEdit() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        "✏️",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                
                Surface(
                    modifier = Modifier
                        .border(0.5.dp, StatusRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onDelete() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        "🗑️",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 呼吸动画绿点
 */
@Composable
private fun BreathingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * 编辑实例对话框 — 毛玻璃风格
 */
@Composable
fun EditInstanceDialog(
    instance: OpenClawInstance,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(instance.name) }
    var url by remember { mutableStateOf(instance.url) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("编辑实例", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("实例名称", color = TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgSurface,
                        unfocusedContainerColor = BgSurface,
                        focusedBorderColor = Accent.copy(alpha = 0.3f),
                        unfocusedBorderColor = GlassBorderFocus,
                        cursorColor = Accent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("WebSocket 地址", color = TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgSurface,
                        unfocusedContainerColor = BgSurface,
                        focusedBorderColor = Accent.copy(alpha = 0.3f),
                        unfocusedBorderColor = GlassBorderFocus,
                        cursorColor = Accent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = Accent)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 添加实例对话框 — 毛玻璃风格
 */
@Composable
fun AddInstanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("ws://") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("添加实例", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("实例名称", color = TextDim) },
                    placeholder = { Text("例如: 翎云", color = TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgSurface,
                        unfocusedContainerColor = BgSurface,
                        focusedBorderColor = Accent.copy(alpha = 0.3f),
                        unfocusedBorderColor = GlassBorderFocus,
                        cursorColor = Accent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("WebSocket 地址", color = TextDim) },
                    placeholder = { Text("ws://example.com:8765", color = TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgSurface,
                        unfocusedContainerColor = BgSurface,
                        focusedBorderColor = Accent.copy(alpha = 0.3f),
                        unfocusedBorderColor = GlassBorderFocus,
                        cursorColor = Accent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = Accent)
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("取消")
            }
        }
    )
}
