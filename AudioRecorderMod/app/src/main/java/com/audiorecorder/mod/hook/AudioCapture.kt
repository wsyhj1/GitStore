package com.audiorecorder.mod.hook

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.orhanobut.logger.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class AudioCapture(private val context: Context) {
    
    private var isInitialized = false
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    // 音频配置
    private var sampleRate = 16000
    private var channelConfig = AudioFormat.CHANNEL_IN_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // 捕获会话管理
    private val captureSessions = ConcurrentHashMap<String, CaptureSession>()
    
    companion object {
        private const val TAG = "AudioCapture"
        private const val BUFFER_SIZE = 4096
    }
    
    data class CaptureSession(
        val id: String,
        val startTime: Long,
        val source: String,
        val outputFile: File,
        var isActive: Boolean = true
    )
    
    fun initialize() {
        try {
            // 创建输出目录
            val outputDir = File(context.getExternalFilesDir(null), "audio_captures")
            outputDir.mkdirs()
            
            // 初始化音频录制器
            initializeAudioRecord()
            
            isInitialized = true
            Logger.i("AudioCapture initialized successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioCapture: ${e.message}")
            throw e
        }
    }
    
    fun cleanup() {
        try {
            // 停止所有捕获会话
            stopAllSessions()
            
            // 释放音频录制器
            audioRecord?.release()
            audioRecord = null
            
            isInitialized = false
            Logger.i("AudioCapture cleaned up")
            
        } catch (e: Exception) {
            Logger.e("Failed to cleanup AudioCapture: ${e.message}")
        }
    }
    
    private fun initializeAudioRecord() {
        try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw Exception("Failed to initialize AudioRecord")
            }
            
            Logger.i("AudioRecord initialized - SampleRate: $sampleRate, BufferSize: $bufferSize")
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioRecord: ${e.message}")
            throw e
        }
    }
    
    fun onRecordingStarted() {
        if (!isInitialized) return
        
        try {
            val sessionId = "session_${System.currentTimeMillis()}"
            val outputFile = createOutputFile("recording_${sessionId}")
            
            val session = CaptureSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                source = "MediaRecorder",
                outputFile = outputFile
            )
            
            captureSessions[sessionId] = session
            startCaptureSession(session)
            
            Logger.i("Recording session started: $sessionId")
            
        } catch (e: Exception) {
            Logger.e("Failed to start recording session: ${e.message}")
        }
    }
    
    fun onRecordingStopped() {
        if (!isInitialized) return
        
        try {
            // 停止所有活跃的会话
            captureSessions.values.filter { it.isActive }.forEach { session ->
                stopCaptureSession(session)
            }
            
            Logger.i("Recording sessions stopped")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop recording sessions: ${e.message}")
        }
    }
    
    fun onWeChatCallStarted(phoneNumber: String) {
        if (!isInitialized) return
        
        try {
            val sessionId = "wechat_${System.currentTimeMillis()}"
            val outputFile = createOutputFile("wechat_call_${phoneNumber}_${sessionId}")
            
            val session = CaptureSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                source = "WeChat_Call",
                outputFile = outputFile
            )
            
            captureSessions[sessionId] = session
            startCaptureSession(session)
            
            Logger.i("WeChat call session started: $sessionId for $phoneNumber")
            
        } catch (e: Exception) {
            Logger.e("Failed to start WeChat call session: ${e.message}")
        }
    }
    
    private fun startCaptureSession(session: CaptureSession) {
        Thread {
            try {
                val buffer = ByteArray(BUFFER_SIZE)
                val outputStream = FileOutputStream(session.outputFile)
                
                audioRecord?.startRecording()
                isRecording = true
                
                Logger.i("Audio capture started for session: ${session.id}")
                
                while (session.isActive && isRecording) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        // 写入音频数据
                        outputStream.write(buffer, 0, bytesRead)
                        
                        // 处理音频数据
                        processCapturedAudio(buffer, bytesRead, session)
                        
                        Logger.d("Captured audio: $bytesRead bytes for session: ${session.id}")
                    }
                }
                
                outputStream.close()
                audioRecord?.stop()
                isRecording = false
                
                Logger.i("Audio capture stopped for session: ${session.id}")
                
            } catch (e: Exception) {
                Logger.e("Error in capture session ${session.id}: ${e.message}")
                session.isActive = false
            }
        }.start()
    }
    
    private fun stopCaptureSession(session: CaptureSession) {
        try {
            session.isActive = false
            
            // 等待会话结束
            var attempts = 0
            while (session.isActive && attempts < 10) {
                Thread.sleep(100)
                attempts++
            }
            
            // 从会话列表中移除
            captureSessions.remove(session.id)
            
            Logger.i("Capture session stopped: ${session.id}")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop capture session ${session.id}: ${e.message}")
        }
    }
    
    private fun stopAllSessions() {
        try {
            captureSessions.values.forEach { session ->
                stopCaptureSession(session)
            }
            
            Logger.i("All capture sessions stopped")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop all sessions: ${e.message}")
        }
    }
    
    private fun processCapturedAudio(data: ByteArray, size: Int, session: CaptureSession) {
        try {
            // 这里可以添加音频处理逻辑
            // 例如：实时分析、格式转换、效果应用等
            
            // 示例：简单的音频质量检测
            val quality = analyzeAudioQuality(data, size)
            if (quality < 0.1) {
                Logger.w("Low audio quality detected in session: ${session.id}")
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to process captured audio: ${e.message}")
        }
    }
    
    private fun analyzeAudioQuality(data: ByteArray, size: Int): Double {
        try {
            var sum = 0.0
            var count = 0
            
            // 简单的音频质量分析
            for (i in 0 until size step 2) {
                if (i + 1 < size) {
                    val sample = (data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)
                    sum += kotlin.math.abs(sample.toDouble())
                    count++
                }
            }
            
            return if (count > 0) sum / count else 0.0
            
        } catch (e: Exception) {
            Logger.e("Failed to analyze audio quality: ${e.message}")
            return 0.0
        }
    }
    
    private fun createOutputFile(prefix: String): File {
        try {
            val outputDir = File(context.getExternalFilesDir(null), "audio_captures")
            outputDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            val fileName = "${prefix}_${timestamp}.pcm"
            
            return File(outputDir, fileName)
            
        } catch (e: Exception) {
            Logger.e("Failed to create output file: ${e.message}")
            throw e
        }
    }
    
    // 配置方法
    fun setSampleRate(rate: Int) {
        sampleRate = rate
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        Logger.i("Sample rate set to: $sampleRate, buffer size: $bufferSize")
    }
    
    fun setChannelConfig(config: Int) {
        channelConfig = config
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        Logger.i("Channel config set to: $channelConfig")
    }
    
    fun setAudioFormat(format: Int) {
        audioFormat = format
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        Logger.i("Audio format set to: $audioFormat")
    }
    
    fun getActiveSessionsCount(): Int {
        return captureSessions.values.count { it.isActive }
    }
    
    fun getSessionInfo(): List<SessionInfo> {
        return captureSessions.values.map { session ->
            SessionInfo(
                id = session.id,
                source = session.source,
                startTime = session.startTime,
                duration = System.currentTimeMillis() - session.startTime,
                isActive = session.isActive,
                fileSize = session.outputFile.length()
            )
        }
    }
    
    data class SessionInfo(
        val id: String,
        val source: String,
        val startTime: Long,
        val duration: Long,
        val isActive: Boolean,
        val fileSize: Long
    )
} 