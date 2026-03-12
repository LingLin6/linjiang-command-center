package com.linjiang.command.data.model

/**
 * Sub-agent 数据模型
 * 
 * @property sessionKey Session 标识符
 * @property label 任务标签
 * @property updatedAt 更新时间戳
 * @property status 状态描述
 */
data class SubAgent(
    val sessionKey: String,
    val label: String,
    val updatedAt: Long,
    val status: String
)
