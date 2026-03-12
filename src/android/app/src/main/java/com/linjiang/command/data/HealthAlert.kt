package com.linjiang.command.data

data class HealthAlert(
    val severity: String, // "warning" | "critical"
    val metric: String,   // "cpu" | "memory" | "disk"
    val value: String,    // "85%"
    val message: String,  // "CPU 使用率过高"
    val timestamp: Long
)
