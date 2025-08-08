package com.audiorecorder.mod.hook

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import com.orhanobut.logger.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class AudioProcessor(private val context: Context) {
    
    private var isRunning = false
    private var processingThread: Thread? = null
    private var audioQueue = LinkedBlockingQueue<AudioData>()
    private val executor = Executors.newCachedThreadPool()
    
    // 音频配置
    private var sampleRate = 16000
    private var channelConfig = AudioFormat.CHANNEL_IN_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // 输出配置
    private var outputDir: File? = null
    private var enableRecording = true
    private var enableProcessing = true
    
    companion object {
        private const val TAG = "AudioProcessor"
        private const val BUFFER_SIZE = 4096
        private const val MAX_QUEUE_SIZE = 1000
    }
    
    data class AudioData(
        val data: ByteArray,
        val offset: Int,
        val size: Int,
        val timestamp: Long,
        val source: String
    )
    
    fun initialize() {
        try {
            // 创建输出目录
            outputDir = File(context.getExternalFilesDir(null), "audio_captures")
            outputDir?.mkdirs()
            
            // 启动处理线程
            startProcessingThread()
            
            isRunning = true
            Logger.i("AudioProcessor initialized successfully")
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioProcessor: ${e.message}")
            throw e
        }
    }
    
    fun cleanup() {
        try {
            isRunning = false
            
            // 停止处理线程
            processingThread?.interrupt()
            processingThread = null
            
            // 清空队列
            audioQueue.clear()
            
            // 关闭线程池
            executor.shutdown()
            
            Logger.i("AudioProcessor cleaned up")
            
        } catch (e: Exception) {
            Logger.e("Failed to cleanup AudioProcessor: ${e.message}")
        }
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun processAudioData(data: ByteArray, offset: Int, size: Int) {
        if (!isRunning) return
        
        try {
            val audioData = AudioData(
                data = data.clone(),
                offset = offset,
                size = size,
                timestamp = System.currentTimeMillis(),
                source = "AudioRecord"
            )
            
            // 添加到处理队列
            if (audioQueue.size < MAX_QUEUE_SIZE) {
                audioQueue.offer(audioData)
            } else {
                Logger.w("Audio queue is full, dropping data")
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to process audio data: ${e.message}")
        }
    }
    
    fun processWeChatAudio(data: ByteArray, length: Int) {
        if (!isRunning) return
        
        try {
            val audioData = AudioData(
                data = data.clone(),
                offset = 0,
                size = length,
                timestamp = System.currentTimeMillis(),
                source = "WeChat"
            )
            
            // 添加到处理队列
            if (audioQueue.size < MAX_QUEUE_SIZE) {
                audioQueue.offer(audioData)
            }
            
            Logger.d("WeChat audio processed: $length bytes")
            
        } catch (e: Exception) {
            Logger.e("Failed to process WeChat audio: ${e.message}")
        }
    }
    
    private fun startProcessingThread() {
        processingThread = Thread {
            while (isRunning) {
                try {
                    // 从队列获取音频数据
                    val audioData = audioQueue.take()
                    
                    // 处理音频数据
                    processAudioDataAsync(audioData)
                    
                } catch (e: InterruptedException) {
                    Logger.i("Audio processing thread interrupted")
                    break
                } catch (e: Exception) {
                    Logger.e("Error in audio processing thread: ${e.message}")
                }
            }
        }
        
        processingThread?.start()
        Logger.i("Audio processing thread started")
    }
    
    private fun processAudioDataAsync(audioData: AudioData) {
        executor.execute {
            try {
                // 音频数据分析
                analyzeAudioData(audioData)
                
                // 音频数据转换
                val processedData = convertAudioFormat(audioData)
                
                // 保存音频数据
                if (enableRecording) {
                    saveAudioData(processedData, audioData.source)
                }
                
                // 音频效果处理
                if (enableProcessing) {
                    applyAudioEffects(processedData)
                }
                
                Logger.d("Audio data processed: ${audioData.size} bytes from ${audioData.source}")
                
            } catch (e: Exception) {
                Logger.e("Failed to process audio data async: ${e.message}")
            }
        }
    }
    
    private fun analyzeAudioData(audioData: AudioData) {
        try {
            // 计算音频统计信息
            val buffer = ByteBuffer.wrap(audioData.data, audioData.offset, audioData.size)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            var sum = 0.0
            var maxAmplitude = 0.0
            var minAmplitude = 0.0
            
            val samples = ShortArray(audioData.size / 2)
            for (i in samples.indices) {
                if (buffer.hasRemaining()) {
                    val sample = buffer.short
                    samples[i] = sample
                    
                    val amplitude = sample.toDouble()
                    sum += amplitude
                    maxAmplitude = maxOf(maxAmplitude, amplitude)
                    minAmplitude = minOf(minAmplitude, amplitude)
                }
            }
            
            val averageAmplitude = sum / samples.size
            val dynamicRange = maxAmplitude - minAmplitude
            
            Logger.d("Audio analysis - Avg: $averageAmplitude, Max: $maxAmplitude, Range: $dynamicRange")
            
        } catch (e: Exception) {
            Logger.e("Failed to analyze audio data: ${e.message}")
        }
    }
    
    private fun convertAudioFormat(audioData: AudioData): ByteArray {
        try {
            // 这里可以添加音频格式转换逻辑
            // 例如：PCM -> WAV, 重采样等
            
            return audioData.data
            
        } catch (e: Exception) {
            Logger.e("Failed to convert audio format: ${e.message}")
            return audioData.data
        }
    }
    
    private fun saveAudioData(data: ByteArray, source: String) {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "${source}_${timestamp}.pcm"
            val file = File(outputDir, fileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(data)
                fos.flush()
            }
            
            Logger.d("Audio data saved: ${file.absolutePath}")
            
        } catch (e: IOException) {
            Logger.e("Failed to save audio data: ${e.message}")
        }
    }
    
    private fun applyAudioEffects(data: ByteArray) {
        try {
            // 这里可以添加音频效果处理
            // 例如：降噪、增益调节、均衡器等
            
            // 示例：简单的音量调节
            val buffer = ByteBuffer.wrap(data)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            val processedData = ByteArray(data.size)
            val processedBuffer = ByteBuffer.wrap(processedData)
            processedBuffer.order(ByteOrder.LITTLE_ENDIAN)
            
            while (buffer.hasRemaining()) {
                val sample = buffer.short
                // 音量增益（1.5倍）
                val amplifiedSample = (sample * 1.5).toInt().toShort()
                processedBuffer.putShort(amplifiedSample)
            }
            
            // 将处理后的数据重新注入音频流
            injectAudioData(processedData)
            
        } catch (e: Exception) {
            Logger.e("Failed to apply audio effects: ${e.message}")
        }
    }
    
    private fun injectAudioData(data: ByteArray) {
        try {
            // 这里实现音频数据重新注入的逻辑
            // 需要Hook相应的音频输出API
            
            Logger.d("Audio data injected: ${data.size} bytes")
            
        } catch (e: Exception) {
            Logger.e("Failed to inject audio data: ${e.message}")
        }
    }
    
    // 配置方法
    fun setSampleRate(rate: Int) {
        sampleRate = rate
        Logger.i("Sample rate set to: $sampleRate")
    }
    
    fun setChannelConfig(config: Int) {
        channelConfig = config
        Logger.i("Channel config set to: $channelConfig")
    }
    
    fun setAudioFormat(format: Int) {
        audioFormat = format
        Logger.i("Audio format set to: $audioFormat")
    }
    
    fun setEnableRecording(enable: Boolean) {
        enableRecording = enable
        Logger.i("Recording enabled: $enableRecording")
    }
    
    fun setEnableProcessing(enable: Boolean) {
        enableProcessing = enable
        Logger.i("Processing enabled: $enableProcessing")
    }
    
    fun setOutputDirectory(dir: File) {
        outputDir = dir
        outputDir?.mkdirs()
        Logger.i("Output directory set to: ${outputDir?.absolutePath}")
    }
} 