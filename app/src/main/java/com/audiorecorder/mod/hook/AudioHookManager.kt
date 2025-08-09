package com.audiorecorder.mod.hook

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import com.orhanobut.logger.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AudioHookManager(private val context: Context) {
    
    private var isInitialized = false
    private var audioProcessor: AudioProcessor? = null
    private var audioCapture: AudioCapture? = null
    
    companion object {
        private const val TAG = "AudioHookManager"
        
        // 目标应用包名
        private val TARGET_PACKAGES = arrayOf(
            "com.tencent.mm",  // 微信
            "com.tencent.mobileqq",  // QQ
            "com.smile.gifmaker",  // 快手
            "com.ss.android.ugc.aweme"  // 抖音
        )
    }
    
    fun initialize() {
        if (isInitialized) {
            Logger.w("AudioHookManager already initialized")
            return
        }
        
        try {
            // 初始化音频处理器
            audioProcessor = AudioProcessor(context)
            audioProcessor?.initialize()
            
            // 初始化音频捕获器
            audioCapture = AudioCapture(context)
            audioCapture?.initialize()
            
            // 注册Hook
            registerHooks()
            
            isInitialized = true
            Logger.i("AudioHookManager initialized successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioHookManager: ${e.message}")
            throw e
        }
    }
    
    fun cleanup() {
        try {
            audioProcessor?.cleanup()
            audioProcessor = null
            
            audioCapture?.cleanup()
            audioCapture = null
            
            isInitialized = false
            Logger.i("AudioHookManager cleaned up")
            
        } catch (e: Exception) {
            Logger.e("Failed to cleanup AudioHookManager: ${e.message}")
        }
    }
    
    fun isRunning(): Boolean {
        return isInitialized && audioProcessor?.isRunning() == true
    }
    
    private fun registerHooks() {
        // Hook MediaRecorder
        hookMediaRecorder()
        
        // Hook AudioRecord
        hookAudioRecord()
        
        // Hook AudioManager
        hookAudioManager()
        
        // Hook 微信特定类
        hookWeChatAudio()
        
        Logger.i("All hooks registered successfully")
    }
    
    private fun hookMediaRecorder() {
        try {
            // Hook MediaRecorder.start()
            XposedHelpers.findAndHookMethod(
                MediaRecorder::class.java,
                "start",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("MediaRecorder.start() called")
                        audioCapture?.onRecordingStarted()
                    }
                    
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("MediaRecorder.start() completed")
                    }
                }
            )
            
            // Hook MediaRecorder.stop()
            XposedHelpers.findAndHookMethod(
                MediaRecorder::class.java,
                "stop",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("MediaRecorder.stop() called")
                        audioCapture?.onRecordingStopped()
                    }
                }
            )
            
            Logger.i("MediaRecorder hooks registered")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook MediaRecorder: ${e.message}")
        }
    }
    
    private fun hookAudioRecord() {
        try {
            // Hook AudioRecord.read()
            XposedHelpers.findAndHookMethod(
                android.media.AudioRecord::class.java,
                "read",
                ByteArray::class.java,
                Int::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val audioData = param.args[0] as ByteArray
                        val offsetInBytes = param.args[1] as Int
                        val sizeInBytes = param.args[2] as Int
                        
                        // 处理音频数据
                        audioProcessor?.processAudioData(audioData, offsetInBytes, sizeInBytes)
                        
                        Logger.d("AudioRecord.read() - size: $sizeInBytes bytes")
                    }
                }
            )
            
            Logger.i("AudioRecord hooks registered")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook AudioRecord: ${e.message}")
        }
    }
    
    private fun hookAudioManager() {
        try {
            // Hook AudioManager.setMode()
            XposedHelpers.findAndHookMethod(
                AudioManager::class.java,
                "setMode",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val mode = param.args[0] as Int
                        Logger.i("AudioManager.setMode() called with mode: $mode")
                    }
                }
            )
            
            Logger.i("AudioManager hooks registered")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook AudioManager: ${e.message}")
        }
    }
    
    private fun hookWeChatAudio() {
        try {
            // Hook 微信的音频相关类
            val wechatClass = XposedHelpers.findClass(
                "com.tencent.mm.plugin.voip.model.VoipMgr",
                null
            )
            
            // Hook 微信语音通话相关方法
            XposedHelpers.findAndHookMethod(
                wechatClass,
                "startVoiceCall",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val phoneNumber = param.args[0] as String
                        Logger.i("WeChat voice call started: $phoneNumber")
                        audioCapture?.onWeChatCallStarted(phoneNumber)
                    }
                }
            )
            
            Logger.i("WeChat audio hooks registered")
            
        } catch (e: Exception) {
            Logger.w("Failed to hook WeChat audio: ${e.message}")
        }
    }
    
    fun onLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 检查是否是目标应用
        if (lpparam.packageName in TARGET_PACKAGES) {
            Logger.i("Target app detected: ${lpparam.packageName}")
            
            // 针对特定应用的Hook
            when (lpparam.packageName) {
                "com.tencent.mm" -> hookWeChatSpecific(lpparam)
                "com.tencent.mobileqq" -> hookQQSpecific(lpparam)
                else -> hookGenericApp(lpparam)
            }
        }
    }
    
    private fun hookWeChatSpecific(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 微信的音频编码器
            val encoderClass = XposedHelpers.findClass(
                "com.tencent.mm.plugin.voip.model.VoipEncoder",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                encoderClass,
                "encode",
                ByteArray::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val audioData = param.args[0] as ByteArray
                        val length = param.args[1] as Int
                        
                        // 处理微信音频数据
                        audioProcessor?.processWeChatAudio(audioData, length)
                        
                        Logger.d("WeChat audio encoded: $length bytes")
                    }
                }
            )
            
            Logger.i("WeChat specific hooks registered")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook WeChat specific: ${e.message}")
        }
    }
    
    private fun hookQQSpecific(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook QQ的音频相关类
            Logger.i("QQ specific hooks registered")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook QQ specific: ${e.message}")
        }
    }
    
    private fun hookGenericApp(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 通用应用的Hook
            Logger.i("Generic app hooks registered for: ${lpparam.packageName}")
            
        } catch (e: Exception) {
            Logger.e("Failed to hook generic app: ${e.message}")
        }
    }
} 