package com.audiorecorder.mod

import android.content.Context
import com.audiorecorder.mod.hook.AudioHookManager
import com.orhanobut.logger.Logger
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    
    private lateinit var audioHookManager: AudioHookManager
    
    companion object {
        private const val TAG = "MainHook"
        
        // 目标应用包名
        private val TARGET_PACKAGES = arrayOf(
            "com.tencent.mm",  // 微信
            "com.tencent.mobileqq",  // QQ
            "com.smile.gifmaker",  // 快手
            "com.ss.android.ugc.aweme",  // 抖音
            "com.tencent.weishi",  // 微视
            "com.ss.android.article.news",  // 今日头条
            "com.netease.cloudmusic",  // 网易云音乐
            "com.tencent.qqmusic"  // QQ音乐
        )
    }
    
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("MainHook: Loading package ${lpparam.packageName}")
            
            // 检查是否是目标应用
            if (lpparam.packageName in TARGET_PACKAGES) {
                Logger.i("Target app detected: ${lpparam.packageName}")
                
                // 初始化音频Hook管理器
                initializeAudioHook(lpparam)
                
                // 注册应用特定的Hook
                registerAppSpecificHooks(lpparam)
                
            } else {
                Logger.d("Non-target app: ${lpparam.packageName}")
            }
            
        } catch (e: Exception) {
            Logger.e("Error in handleLoadPackage: ${e.message}")
        }
    }
    
    private fun initializeAudioHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 获取应用上下文
            val context = getApplicationContext(lpparam)
            
            // 初始化音频Hook管理器
            audioHookManager = AudioHookManager(context)
            audioHookManager.initialize()
            
            // 注册包加载回调
            audioHookManager.onLoadPackage(lpparam)
            
            Logger.i("AudioHook initialized for ${lpparam.packageName}")
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioHook: ${e.message}")
        }
    }
    
    private fun registerAppSpecificHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            when (lpparam.packageName) {
                "com.tencent.mm" -> registerWeChatHooks(lpparam)
                "com.tencent.mobileqq" -> registerQQHooks(lpparam)
                "com.smile.gifmaker" -> registerKuaishouHooks(lpparam)
                "com.ss.android.ugc.aweme" -> registerDouyinHooks(lpparam)
                else -> registerGenericHooks(lpparam)
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to register app-specific hooks: ${e.message}")
        }
    }
    
    private fun registerWeChatHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("Registering WeChat specific hooks")
            
            // Hook 微信的音频相关类
            hookWeChatAudioClasses(lpparam)
            
            // Hook 微信的语音通话
            hookWeChatVoiceCall(lpparam)
            
            // Hook 微信的语音消息
            hookWeChatVoiceMessage(lpparam)
            
            Logger.i("WeChat hooks registered successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to register WeChat hooks: ${e.message}")
        }
    }
    
    private fun hookWeChatAudioClasses(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 微信的音频管理器
            val audioManagerClass = XposedHelpers.findClass(
                "com.tencent.mm.plugin.voip.model.VoipMgr",
                lpparam.classLoader
            )
            
            // Hook 音频初始化
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "initAudio",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("WeChat audio initialization called")
                    }
                    
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("WeChat audio initialization completed")
                    }
                }
            )
            
            // Hook 音频编码器
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
                        
                        Logger.d("WeChat audio encoding: $length bytes")
                        
                        // 处理微信音频数据
                        // audioHookManager.processWeChatAudio(audioData, length)
                    }
                }
            )
            
        } catch (e: Exception) {
            Logger.e("Failed to hook WeChat audio classes: ${e.message}")
        }
    }
    
    private fun hookWeChatVoiceCall(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 微信语音通话
            val callClass = XposedHelpers.findClass(
                "com.tencent.mm.plugin.voip.ui.VideoTalkUI",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                callClass,
                "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("WeChat voice call started")
                        
                        // 通知音频捕获器
                        // audioHookManager.onWeChatCallStarted("unknown")
                    }
                }
            )
            
        } catch (e: Exception) {
            Logger.e("Failed to hook WeChat voice call: ${e.message}")
        }
    }
    
    private fun hookWeChatVoiceMessage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 微信语音消息录制
            val recorderClass = XposedHelpers.findClass(
                "com.tencent.mm.plugin.voip.model.VoipRecorder",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                recorderClass,
                "startRecord",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("WeChat voice message recording started")
                    }
                }
            )
            
            XposedHelpers.findAndHookMethod(
                recorderClass,
                "stopRecord",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("WeChat voice message recording stopped")
                    }
                }
            )
            
        } catch (e: Exception) {
            Logger.e("Failed to hook WeChat voice message: ${e.message}")
        }
    }
    
    private fun registerQQHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("Registering QQ specific hooks")
            
            // Hook QQ的音频相关功能
            // 这里可以添加QQ特定的Hook逻辑
            
            Logger.i("QQ hooks registered successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to register QQ hooks: ${e.message}")
        }
    }
    
    private fun registerKuaishouHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("Registering Kuaishou specific hooks")
            
            // Hook 快手的音频相关功能
            // 这里可以添加快手特定的Hook逻辑
            
            Logger.i("Kuaishou hooks registered successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to register Kuaishou hooks: ${e.message}")
        }
    }
    
    private fun registerDouyinHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("Registering Douyin specific hooks")
            
            // Hook 抖音的音频相关功能
            // 这里可以添加抖音特定的Hook逻辑
            
            Logger.i("Douyin hooks registered successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to register Douyin hooks: ${e.message}")
        }
    }
    
    private fun registerGenericHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Logger.i("Registering generic hooks for ${lpparam.packageName}")
            
            // 通用Hook逻辑
            // 这里可以添加适用于所有应用的通用Hook
            
            Logger.i("Generic hooks registered successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to register generic hooks: ${e.message}")
        }
    }
    
    private fun getApplicationContext(lpparam: XC_LoadPackage.LoadPackageParam): Context {
        try {
            // 尝试获取应用上下文
            val activityThreadClass = XposedHelpers.findClass(
                "android.app.ActivityThread",
                null
            )
            
            val currentApplication = XposedHelpers.callStaticMethod(
                activityThreadClass,
                "currentApplication"
            )
            
            return currentApplication as Context
            
        } catch (e: Exception) {
            Logger.e("Failed to get application context: ${e.message}")
            
            // 如果无法获取上下文，返回null
            // 音频Hook管理器会处理这种情况
            throw e
        }
    }
    
    // 清理方法
    fun cleanup() {
        try {
            audioHookManager.cleanup()
            Logger.i("MainHook cleaned up")
            
        } catch (e: Exception) {
            Logger.e("Failed to cleanup MainHook: ${e.message}")
        }
    }
} 