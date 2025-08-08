@echo off
echo 开始编译 AudioRecorder Mod...
echo.

REM 检查Java环境
echo 检查Java环境...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未找到Java环境，请安装JDK 8或更高版本
    pause
    exit /b 1
)

REM 检查Android SDK
echo 检查Android SDK...
if not exist "%ANDROID_HOME%" (
    echo 警告: 未设置ANDROID_HOME环境变量
    echo 请确保Android SDK已正确安装
)

REM 清理项目
echo 清理项目...
call gradlew clean 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 警告: Gradle清理失败，尝试继续编译...
)

REM 编译Debug版本
echo 编译Debug版本...
call gradlew assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo 编译成功！
    echo APK文件位置: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo 如果编译失败，请尝试以下解决方案：
    echo 1. 使用Android Studio打开项目
    echo 2. 确保已安装Android SDK
    echo 3. 检查网络连接
    echo 4. 手动下载Gradle
) else (
    echo.
    echo 编译失败！
    echo.
    echo 解决方案：
    echo 1. 使用Android Studio打开项目: AudioRecorderMod
    echo 2. 在Android Studio中点击 Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
    echo 3. 或者运行: gradlew assembleDebug --stacktrace
    echo.
    echo 项目位置: %CD%
)

pause 