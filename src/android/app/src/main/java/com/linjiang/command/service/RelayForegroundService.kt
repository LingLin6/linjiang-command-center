package com.linjiang.command.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.linjiang.command.MainActivity
import com.linjiang.command.R

/**
 * 前台服务 — 保持 WebSocket 连接不被 Android 系统杀掉
 * 
 * 显示常驻通知："🪶 翎绛指挥中心 · 已连接 N 个实例"
 * 使用 IMPORTANCE_LOW，不出声不震动。
 * foregroundServiceType="dataSync" 满足 Android 14+ 要求。
 */
class RelayForegroundService : Service() {
    
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "relay_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
        val notification = buildForegroundNotification(0)
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY  // 被杀后自动重启
    }
    
    /**
     * 更新通知内容（在线实例数 + 名称列表）
     */
    fun updateNotification(onlineCount: Int, instanceNames: List<String> = emptyList()) {
        val notification = buildForegroundNotification(onlineCount, instanceNames)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun buildForegroundNotification(
        onlineCount: Int,
        names: List<String> = emptyList()
    ): Notification {
        val text = if (onlineCount > 0) {
            "已连接 $onlineCount 个实例" +
                if (names.isNotEmpty()) " · ${names.joinToString("、")}" else ""
        } else {
            "正在连接..."
        }
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🪶 翎绛指挥中心")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)  // 低优先级，不出声
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "连接服务",
            NotificationManager.IMPORTANCE_LOW  // 无声音/震动
        ).apply {
            description = "保持与中继服务器的连接"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
