package com.linjiang.command.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linjiang.command.data.model.MemoryCategory
import com.linjiang.command.data.model.MemoryItem
import com.linjiang.command.data.model.MemorySearchState
import com.linjiang.command.data.model.ProjectProgress
import com.linjiang.command.ui.theme.*
import com.linjiang.command.viewmodel.MemoryViewModel

/**
 * 记忆页 — HUD 指挥中心科幻风格 v4.0
 */
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onSearch: (String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val focusManager = LocalFocusManager.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 标题
        item {
            Text(
                text = "记忆",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        
        // 搜索框 — 大号 20dp 圆角，毛玻璃底
        item {
            SearchBar(
                query = query,
                isSearching = searchState == MemorySearchState.LOADING,
                onQueryChange = { viewModel.updateQuery(it) },
                onSearch = {
                    focusManager.clearFocus()
                    onSearch(query)
                }
            )
        }
        
        // 错误提示
        if (errorMessage != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, StatusRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = StatusRedDim.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "❌ $errorMessage",
                        color = StatusRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        
        // 搜索中状态
        if (searchState == MemorySearchState.LOADING) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Accent,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "搜索中...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        
        // 最近记忆 / 搜索结果
        if (memories.isNotEmpty()) {
            item {
                Text(
                    text = if (query.isNotEmpty()) "搜索结果" else "最近记忆",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            
            items(memories) { memory ->
                MemoryCard(memory)
            }
        }
        
        // 空状态
        if (memories.isEmpty() && searchState != MemorySearchState.LOADING) {
            item {
                EmptyMemoryState(
                    hasSearched = searchState == MemorySearchState.SUCCESS,
                    query = query,
                    onInitialSearch = { onSearch("") }
                )
            }
        }
        
        // 分类浏览
        item {
            Text(
                text = "分类",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
        
        item {
            CategoryGrid(
                categories = categories,
                onCategoryClick = { cat ->
                    viewModel.updateQuery(cat.name)
                    onSearch(cat.name)
                }
            )
        }
        
        // 项目看板
        if (projects.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "项目看板",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
            
            item {
                ProjectBoard(projects)
            }
        }
    }
}

/**
 * 搜索框 — 大号圆角毛玻璃
 */
@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("🔍 搜索记忆...", color = TextDim, fontSize = 14.sp)
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Accent,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Accent
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BgCard,
            unfocusedContainerColor = BgCard,
            focusedBorderColor = WarmGlow.copy(alpha = 0.4f),
            unfocusedBorderColor = WarmGlowBorder,
            cursorColor = Accent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

/**
 * 记忆卡片 — 毛玻璃风格
 */
@Composable
private fun MemoryCard(memory: MemoryItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = memory.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (memory.summary != memory.title && memory.summary.isNotEmpty()) {
                Text(
                    text = memory.summary,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            
            if (memory.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(memory.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AccentGlow)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = Accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分类网格（2x2）— 每个卡片有对应色 3% 底色调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryGrid(
    categories: List<MemoryCategory>,
    onCategoryClick: (MemoryCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in categories.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in row) {
                    val catTintColor = when (cat.name) {
                        "项目" -> Accent
                        "教训" -> StatusYellow
                        "人物" -> StatusBlue
                        "基建" -> StatusGreen
                        else -> Accent
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = catTintColor.copy(alpha = 0.03f),
                        shadowElevation = 2.dp,
                        onClick = { onCategoryClick(cat) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = cat.emoji, fontSize = 28.sp)
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${cat.count}",
                                fontSize = 18.sp,
                                color = catTintColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 项目看板 — 毛玻璃风格
 */
@Composable
private fun ProjectBoard(projects: List<ProjectProgress>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WarmGlowBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = BgCard.copy(alpha = 0.85f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            projects.forEach { project ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(0.35f)) {
                        Text(
                            text = project.name,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (project.milestone.isNotEmpty()) {
                            Text(
                                text = project.milestone,
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }
                    
                    // 进度条 — accent 渐变
                    Box(modifier = Modifier.weight(0.5f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BgDeep)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((project.progress / 100f).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (project.isCompleted) StatusGreen
                                        else Accent
                                    )
                            )
                        }
                    }
                    
                    Text(
                        text = if (project.isCompleted) "完成" else "${project.progress}%",
                        fontSize = 12.sp,
                        color = if (project.isCompleted) StatusGreen else TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyMemoryState(
    hasSearched: Boolean,
    query: String,
    onInitialSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧠", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasSearched && query.isNotEmpty()) {
                    "没有找到「$query」相关记忆"
                } else {
                    "输入关键词搜索记忆库"
                },
                fontSize = 14.sp,
                color = TextSecondary
            )
            if (!hasSearched) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onInitialSearch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("加载最近记忆", fontSize = 13.sp)
                }
            }
        }
    }
}
