# YTReBroadcastMonitors Android

一个 Android 16 优先的 YouTube 频道直播/回放监控应用。

本仓库已移除原 Win10 桌面运行与打包链路，不再包含 FastAPI、Uvicorn、pywebview、WebView2、PyInstaller 或 Inno Setup。Android 版本不在手机端启动 HTTP 服务，而是把原来的 API 边界重构为 Kotlin 本地业务边界。

## 最终架构

```text
Vue 3
  ↓
Android WebView
  ↓
JavascriptInterface
  ↓
Kotlin
  ↓
Repository / Service
  ↓
YoutubeDL-Android
```

## 仓库结构

- `android/`：Android Gradle 工程，包含 WebView 宿主、JS Bridge、Repository、Service、Cache 与 YoutubeDL-Android 集成。
- `frontend/`：Vue 3 + Vite 前端源码，构建产物会在 Android Gradle 构建时自动复制到 WebView assets。
- `channels/`：频道 CSV 数据，作为 Android assets 被 `CSVManager` 读取。
- `build_android.sh`：一键构建 Android Debug APK。

## Android 分层

- `MainActivity`：创建 Android `WebView`，加载本地 Vue 页面并注入 `YVMonitorBridge`。
- `YVMonitorBridge`：Android `JavascriptInterface`，接收 Vue 的方法调用并回调 Promise。
- `ChannelRepository`：替代原 HTTP API 的频道查询与单频道检测入口。
- `ScanService`：维护扫描状态、检测结果和网络状态。
- `AvatarCache`：Android 本地头像缓存入口。
- `CSVManager`：读取 `channels/*.csv`。
- `YoutubeDlService`：封装 `YoutubeDL-Android`，负责移动端 yt-dlp 调用。

## 一键编译

环境要求：

- JDK 17+
- Android SDK，包含 Android 16 / API 36 平台
- Gradle 8.10+（或 Android Studio 中可用的 Gradle）
- Node.js 18+

执行：

```bash
./build_android.sh
```

等价于：

```bash
cd android
gradle :app:assembleDebug
```

> 注意：为兼容不支持二进制文件的拉取/补丁环境，仓库不提交 `gradle-wrapper.jar`。请使用本机已安装的 Gradle 或 Android Studio 提供的 Gradle。

Gradle 会自动执行：

1. `npm install`
2. `npm run build`
3. 将 `frontend/dist/` 复制到 Android WebView assets
4. 编译 `app-debug.apk`

APK 输出位置：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```
