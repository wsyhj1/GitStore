# AudioRecorder Mod v2.4.1

## 项目描述

AudioRecorder Mod 是一个基于LSPosed框架的Android音频流Hook模块，专门设计用于拦截和处理系统音频流，特别是微信等应用的音频数据。

## 主要功能

### 1. 音频流拦截
- 拦截系统所有音频流
- 支持微信、QQ等主流通讯应用
- 实时捕获音频数据

### 2. 音频数据处理
- 实时音频格式转换
- 音频质量分析
- 音频数据加密/解密

### 3. 音频流注入
- 修改音频数据后重新注入
- 支持音频效果处理
- 音频数据转发

## 技术架构

### 核心组件
- **LSPosed Hook引擎**：系统级API拦截
- **AudioFlinger Hook**：音频服务拦截
- **音频HAL层Hook**：底层音频数据捕获
- **实时音频处理器**：音频数据实时处理

### 权限要求
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.CAPTURE_AUDIO_OUTPUT" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />
```

## 编译说明

### 方法1：使用Android Studio
1. 打开Android Studio
2. 选择 "Open an existing Android Studio project"
3. 选择 `AudioRecorderMod` 文件夹
4. 等待项目同步完成
5. 点击 "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"

### 方法2：使用命令行
```bash
# 进入项目目录
cd AudioRecorderMod

# 清理项目
./gradlew clean

# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease
```

### 方法3：使用批处理文件
```bash
# Windows
build.bat

# Linux/Mac
./build.sh
```

## 安装和使用

### 1. 安装APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 激活LSPosed模块
1. 打开LSPosed Manager
2. 进入"模块"页面
3. 找到"AudioRecorder Mod"
4. 点击"激活"
5. 重启设备

### 3. 配置作用域
1. 在LSPosed Manager中选择"AudioRecorder Mod"
2. 点击"作用域"
3. 勾选以下应用：
   - 微信 (com.tencent.mm)
   - QQ (com.tencent.mobileqq)
   - 抖音 (com.ss.android.ugc.aweme)
   - 快手 (com.smile.gifmaker)
   - 系统框架 (android)

### 4. 启动Hook
1. 打开AudioRecorder Mod应用
2. 点击"启动Hook"按钮
3. 确认权限请求
4. 开始使用目标应用

## 项目结构

```
AudioRecorderMod/
├── app/
│   ├── build.gradle              # 应用级构建配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/audiorecorder/mod/
│       │   ├── MainActivity.kt   # 主界面
│       │   ├── MainHook.kt       # LSPosed入口
│       │   ├── hook/             # Hook相关类
│       │   └── service/          # 服务类
│       └── res/                  # 资源文件
├── build.gradle                  # 项目级构建配置
├── settings.gradle               # Gradle设置
└── README.md                     # 项目说明
```

## 注意事项

⚠️ **重要提醒**：
- 本项目仅供学习和研究使用
- 请遵守当地法律法规
- 不得用于非法用途
- 使用本模块可能违反某些应用的服务条款

## 技术细节

### Hook目标
- `MediaRecorder` - 媒体录制API
- `AudioRecord` - 音频录制API
- `AudioManager` - 音频管理API
- 微信音频相关类

### 音频处理
- 实时PCM数据处理
- 音频格式转换
- 音频质量分析
- 音频数据保存

## 版本历史

- v2.4.1 - 初始版本
  - 基础Hook功能
  - 音频流拦截
  - 实时音频处理
  - LSPosed框架集成

## 许可证

本项目仅供学习和研究使用，请遵守相关法律法规。 