package com.audiorecorder.mod.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.orhanobut.logger.Logger

class AudioProcessService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Logger.i("AudioProcessService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("AudioProcessService started")
        return START_STICKY
    }
    
    override fun onDestroy() {
        Logger.i("AudioProcessService destroyed")
        super.onDestroy()
    }
} 