package com.audiorecorder.mod

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.audiorecorder.mod.databinding.ActivityMainBinding
import com.audiorecorder.mod.hook.AudioHookManager
import com.audiorecorder.mod.service.AudioHookService
import com.audiorecorder.mod.service.AudioProcessService
import com.orhanobut.logger.Logger

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioHookManager: AudioHookManager
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 检查Xposed框架是否可用
        if (!isXposedAvailable()) {
            Toast.makeText(this, "未检测到Xposed框架，请安装后重试", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        audioHookManager = AudioHookManager(this)
        
        setupUI()
        checkPermissions()
    }
    
    /**
     * 检查Xposed框架是否可用
     */
    private fun isXposedAvailable(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    private fun setupUI() {
        // 启动Hook按钮
        binding.btnStartHook.setOnClickListener {
            if (checkPermissions()) {
                startAudioHook()
            }
        }
        
        // 停止Hook按钮
        binding.btnStopHook.setOnClickListener {
            stopAudioHook()
        }
        
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            openSettings()
        }
        
        // 日志按钮
        binding.btnLogs.setOnClickListener {
            showLogs()
        }
        
        // 状态显示
        updateStatus()
    }
    
    private fun startAudioHook() {
        try {
            // 启动音频Hook服务
            val hookIntent = Intent(this, AudioHookService::class.java)
            startForegroundService(hookIntent)
            
            // 启动音频处理服务
            val processIntent = Intent(this, AudioProcessService::class.java)
            startService(processIntent)
            
            // 初始化Hook管理器
            audioHookManager.initialize()
            
            updateStatus()
            Toast.makeText(this, "音频Hook已启动", Toast.LENGTH_SHORT).show()
            
            Logger.i("AudioHook started successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to start AudioHook: ${e.message}")
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopAudioHook() {
        try {
            // 停止服务
            stopService(Intent(this, AudioHookService::class.java))
            stopService(Intent(this, AudioProcessService::class.java))
            
            // 清理Hook管理器
            audioHookManager.cleanup()
            
            updateStatus()
            Toast.makeText(this, "音频Hook已停止", Toast.LENGTH_SHORT).show()
            
            Logger.i("AudioHook stopped successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop AudioHook: ${e.message}")
            Toast.makeText(this, "停止失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun openSettings() {
        // 打开设置界面
        // val settingsIntent = Intent(this, SettingsActivity::class.java)
        // startActivity(settingsIntent)
        Toast.makeText(this, "设置功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    private fun showLogs() {
        // 显示日志界面
        // val logsIntent = Intent(this, LogsActivity::class.java)
        // startActivity(logsIntent)
        Toast.makeText(this, "日志功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateStatus() {
        val isRunning = audioHookManager.isRunning()
        binding.tvStatus.text = if (isRunning) "运行中" else "已停止"
        binding.btnStartHook.isEnabled = !isRunning
        binding.btnStopHook.isEnabled = isRunning
    }
    
    private fun checkPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        
        return true
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要录音权限才能运行", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::audioHookManager.isInitialized) {
            audioHookManager.cleanup()
        }
    }
}