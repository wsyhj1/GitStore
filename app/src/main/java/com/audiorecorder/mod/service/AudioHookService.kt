package com.audiorecorder.mod.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.audiorecorder.mod.R
import com.audiorecorder.mod.hook.AudioHookManager
import com.orhanobut.logger.Logger

class AudioHookService : Service() {
    
    private lateinit var audioHookManager: AudioHookManager
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "AudioHookService"
        private const val CHANNEL_NAME = "Audio Hook Service"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // 初始化音频Hook管理器
            audioHookManager = AudioHookManager(this)
            audioHookManager.initialize()
            
            // 创建通知渠道
            createNotificationChannel()
            
            Logger.i("AudioHookService created successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to create AudioHookService: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // 启动前台服务
            startForeground(NOTIFICATION_ID, createNotification())
            
            Logger.i("AudioHookService started")
            
            // 返回START_STICKY，确保服务被杀死后会重启
            return START_STICKY
            
        } catch (e: Exception) {
            Logger.e("Failed to start AudioHookService: ${e.message}")
            return START_NOT_STICKY
        }
    }
    
    override fun onDestroy() {
        try {
            // 清理资源
            audioHookManager.cleanup()
            
            Logger.i("AudioHookService destroyed")
            
        } catch (e: Exception) {
            Logger.e("Failed to destroy AudioHookService: ${e.message}")
        }
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio Hook Service Notification"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Logger.i("Notification channel created")
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioRecorder Mod")
            .setContentText("音频Hook服务运行中")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    fun updateNotification(status: String) {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AudioRecorder Mod")
                .setContentText("状态: $status")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            Logger.d("Notification updated: $status")
            
        } catch (e: Exception) {
            Logger.e("Failed to update notification: ${e.message}")
        }
    }
    
    fun isRunning(): Boolean {
        return audioHookManager.isRunning()
    }
    
    fun getStatus(): String {
        return if (isRunning()) "运行中" else "已停止"
    }
} 