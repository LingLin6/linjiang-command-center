package com.linjiang.command.data.model

/**
 * 记忆条目
 */
data class MemoryItem(
    val id: String = "${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}",
    val title: String,
    val summary: String,
    val tags: List<String> = emptyList(),
    val category: String = "",  // project, lesson, person, infra
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 记忆分类
 */
data class MemoryCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val count: Int = 0
)

/**
 * 项目进度
 */
data class ProjectProgress(
    val name: String,
    val milestone: String = "",
    val progress: Int = 0,   // 0-100
    val isCompleted: Boolean = false
)

/**
 * 记忆搜索状态
 */
enum class MemorySearchState {
    IDLE,       // 初始/空闲
    LOADING,    // 搜索中
    SUCCESS,    // 搜索完成
    ERROR       // 搜索失败
}
