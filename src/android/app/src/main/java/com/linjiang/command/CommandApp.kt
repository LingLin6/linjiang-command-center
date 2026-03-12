package com.linjiang.command

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import com.linjiang.command.data.local.AppDatabase
import com.linjiang.command.service.RelayForegroundService

/**
 * 应用程序类
 * 
 * 负责应用程序的全局初始化
 */
class CommandApp : Application() {
    
    lateinit var database: AppDatabase
        private set
    
    companion object {
        private const val TAG = "CommandApp"
        const val CHANNEL_MESSAGES = "linjiang_messages"
        const val CHANNEL_ALERTS = "linjiang_alerts"
        
        lateinit var instance: CommandApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        Log.d(TAG, "Application started")
        createNotificationChannels()
        startRelayService()
    }
    
    /**
     * 启动前台服务，保持 WebSocket 连接
     */
    private fun startRelayService() {
        val serviceIntent = Intent(this, RelayForegroundService::class.java).apply {
            action = RelayForegroundService.ACTION_START
        }
        try {
            startForegroundService(serviceIntent)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // 消息通知渠道
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "消息通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "翎绛回复的消息"
                }
            )
            
            // 告警通知渠道
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "系统告警",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "健康检查告警、连接异常"
                }
            )
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminated")
    }
}
