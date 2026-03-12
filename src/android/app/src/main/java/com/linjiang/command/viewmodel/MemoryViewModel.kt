package com.linjiang.command.viewmodel

import androidx.lifecycle.ViewModel
import com.linjiang.command.data.model.MemoryCategory
import com.linjiang.command.data.model.MemoryItem
import com.linjiang.command.data.model.MemorySearchState
import com.linjiang.command.data.model.ProjectProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 记忆页 ViewModel
 * 管理搜索状态、记忆列表、分类和项目看板
 */
class MemoryViewModel : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _searchState = MutableStateFlow(MemorySearchState.IDLE)
    val searchState: StateFlow<MemorySearchState> = _searchState
    
    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories
    
    private val _categories = MutableStateFlow(defaultCategories())
    val categories: StateFlow<List<MemoryCategory>> = _categories
    
    private val _projects = MutableStateFlow<List<ProjectProgress>>(emptyList())
    val projects: StateFlow<List<ProjectProgress>> = _projects
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    /**
     * 更新搜索关键词
     */
    fun updateQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 设置搜索状态为加载中
     */
    fun setSearching() {
        _searchState.value = MemorySearchState.LOADING
    }
    
    /**
     * 处理搜索结果（从 RelayClient 的 memory_result 回调）
     */
    fun handleMemoryResult(resultText: String) {
        _searchState.value = MemorySearchState.SUCCESS
        
        // 解析返回的文本为记忆条目
        val items = parseMemoryResult(resultText)
        _memories.value = items
        
        // 从结果中提取分类计数和项目进度（如果可用）
        updateCategoriesFromResult(resultText)
        updateProjectsFromResult(resultText)
    }
    
    /**
     * 处理搜索错误
     */
    fun handleSearchError(error: String) {
        _searchState.value = MemorySearchState.ERROR
        _errorMessage.value = error
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 解析记忆搜索结果文本为 MemoryItem 列表
     * 支持多种格式：Markdown 列表、标题+内容块等
     */
    private fun parseMemoryResult(text: String): List<MemoryItem> {
        val items = mutableListOf<MemoryItem>()
        
        if (text.isBlank()) return items
        
        // 尝试按 ## 标题分块解析
        val blocks = text.split(Regex("(?=^## )", RegexOption.MULTILINE))
        
        if (blocks.size > 1) {
            for (block in blocks) {
                if (block.isBlank()) continue
                val lines = block.trim().split("\n")
                val titleLine = lines.firstOrNull()?.removePrefix("## ")?.trim() ?: continue
                val summary = lines.drop(1).joinToString("\n").trim().take(200)
                val tags = extractTags(block)
                
                items.add(MemoryItem(
                    title = titleLine,
                    summary = summary.ifEmpty { titleLine },
                    tags = tags,
                    category = inferCategory(titleLine, block)
                ))
            }
        } else {
            // 按行解析，每个 - 开头的是一条记忆
            val lines = text.split("\n")
            var currentTitle = ""
            var currentContent = StringBuilder()
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    // 保存前一条
                    if (currentTitle.isNotEmpty()) {
                        items.add(MemoryItem(
                            title = currentTitle,
                            summary = currentContent.toString().trim().take(200).ifEmpty { currentTitle },
                            tags = extractTags(currentContent.toString()),
                            category = inferCategory(currentTitle, currentContent.toString())
                        ))
                    }
                    currentTitle = trimmed.removePrefix("- ").removePrefix("* ").trim()
                    currentContent = StringBuilder()
                } else if (trimmed.isNotEmpty() && currentTitle.isNotEmpty()) {
                    currentContent.append(trimmed).append(" ")
                }
            }
            
            // 最后一条
            if (currentTitle.isNotEmpty()) {
                items.add(MemoryItem(
                    title = currentTitle,
                    summary = currentContent.toString().trim().take(200).ifEmpty { currentTitle },
                    tags = extractTags(currentContent.toString()),
                    category = inferCategory(currentTitle, currentContent.toString())
                ))
            }
            
            // 如果以上解析都没拿到内容，把整段作为一条
            if (items.isEmpty() && text.isNotBlank()) {
                val firstLine = text.lines().firstOrNull { it.isNotBlank() } ?: text.take(50)
                items.add(MemoryItem(
                    title = firstLine.take(50),
                    summary = text.take(300),
                    tags = extractTags(text)
                ))
            }
        }
        
        return items
    }
    
    /**
     * 从文本中提取标签
     */
    private fun extractTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        
        // 提取 [xxx] 格式的标签
        Regex("\\[([^\\]]+)\\]").findAll(text).forEach { match ->
            val tag = match.groupValues[1]
            if (tag.length in 1..10) {
                tags.add(tag)
            }
        }
        
        // 提取 #xxx 格式的标签
        Regex("#([\\w\\u4e00-\\u9fff]+)").findAll(text).forEach { match ->
            val tag = match.groupValues[1]
            if (tag.length in 1..10 && !tag.startsWith("#")) {
                tags.add(tag)
            }
        }
        
        return tags.distinct().take(5)
    }
    
    /**
     * 推断记忆分类
     */
    private fun inferCategory(title: String, content: String): String {
        val combined = "$title $content".lowercase()
        return when {
            combined.contains("项目") || combined.contains("project") || 
            combined.contains("开发") || combined.contains("版本") -> "project"
            combined.contains("教训") || combined.contains("lesson") || 
            combined.contains("注意") || combined.contains("坑") -> "lesson"
            combined.contains("人物") || combined.contains("person") || 
            combined.contains("联系") -> "person"
            combined.contains("基建") || combined.contains("infra") || 
            combined.contains("服务器") || combined.contains("部署") -> "infra"
            else -> "project"
        }
    }
    
    /**
     * 从结果中更新分类计数
     */
    private fun updateCategoriesFromResult(text: String) {
        val projectCount = Regex("(?i)(项目|project|开发|版本)").findAll(text).count().coerceAtLeast(0)
        val lessonCount = Regex("(?i)(教训|lesson|注意|坑|错误)").findAll(text).count().coerceAtLeast(0)
        val personCount = Regex("(?i)(人物|person|联系人)").findAll(text).count().coerceAtLeast(0)
        val infraCount = Regex("(?i)(基建|infra|服务器|部署)").findAll(text).count().coerceAtLeast(0)
        
        _categories.value = listOf(
            MemoryCategory("project", "项目", "📂", projectCount.coerceAtLeast(1)),
            MemoryCategory("lesson", "教训", "💡", lessonCount.coerceAtLeast(1)),
            MemoryCategory("person", "人物", "👤", personCount.coerceAtLeast(1)),
            MemoryCategory("infra", "基建", "🔧", infraCount.coerceAtLeast(1))
        )
    }
    
    /**
     * 从结果中更新项目进度
     */
    private fun updateProjectsFromResult(text: String) {
        val projects = mutableListOf<ProjectProgress>()
        
        // 尝试匹配 "项目名 M数字 进度%" 格式
        Regex("([\\u4e00-\\u9fff\\w]+)\\s+(M\\d+)?\\s*(?:[█░]*\\s*)?(\\d+)%").findAll(text).forEach { match ->
            val name = match.groupValues[1]
            val milestone = match.groupValues[2]
            val progress = match.groupValues[3].toIntOrNull() ?: 0
            projects.add(ProjectProgress(
                name = name,
                milestone = milestone,
                progress = progress,
                isCompleted = progress >= 100
            ))
        }
        
        if (projects.isNotEmpty()) {
            _projects.value = projects
        }
    }
    
    companion object {
        fun defaultCategories(): List<MemoryCategory> = listOf(
            MemoryCategory("project", "项目", "📂", 0),
            MemoryCategory("lesson", "教训", "💡", 0),
            MemoryCategory("person", "人物", "👤", 0),
            MemoryCategory("infra", "基建", "🔧", 0)
        )
    }
}
