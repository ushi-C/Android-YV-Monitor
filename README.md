# YTReBroadcastMonitors

一个用于多窗口播放 YouTube 频道直播/回放的桌面应用：
- 后端使用 **FastAPI + Uvicorn** 。
- 前端使用 **Vue 3 + Vite** 。
- 桌面端通过 **pywebview (WebView2)** 承载 UI。

## 仓库结构

- `backend/`：后端入口、API、网络测试、频道扫描、缓存与配置管理。
- `frontend/`：前端源码。
- `core/`：播放器管理等核心脚本。
- `requirements.txt`：Python 依赖。
- `build.bat` / `build_installer.bat`：Windows 构建脚本。

## 环境要求

- Python 3.10+
- Node.js 18+
- Windows 桌面运行时建议安装 WebView2 Runtime

## 更新计划

- channels推荐会保持更新，本地改动时注意不可改变第一行的列名
- debug

## License

本项目采用 [MIT License](./LICENSE)。

## Android 16 重构方向

Android 版本不迁移 FastAPI，也不在手机端启动 Uvicorn。本项目的 Android 重构目标是把原来的 HTTP API 边界改成本地 Kotlin 业务边界：

```text
Vue 3
  ↓
JavascriptInterface
  ↓
Kotlin
  ↓
Repository / Service
  ↓
YoutubeDL-Android
```

新增的 `android/` 工程采用以下分层：

- `WebView`：加载 Vue/Vite 构建后的静态页面。
- `YVMonitorBridge`：Android `JavascriptInterface`，把 Vue 调用分发给 Kotlin。
- `ChannelRepository`：替代原 FastAPI 的频道查询/单频道检测 API。
- `ScanService`：替代原扫描相关 API，维护扫描状态和检测结果。
- `AvatarCache`：Android 本地头像缓存入口。
- `CSVManager`：读取仓库现有 `channels/*.csv` 作为 Android assets。
- `YoutubeDlService`：封装 `YoutubeDL-Android`，作为移动端 yt-dlp 调用边界。

前端的 `useApiClient` 已经改成双通道：

1. Android WebView 内优先调用 `window.YVMonitorAndroid.call(...)`。
2. 桌面/Web 开发环境继续回退到 `/api/...` 或 `VITE_API_BASE_URL` 配置的 FastAPI 服务。

构建 Android 静态资源时，先执行：

```bash
cd frontend
npm run build
```

然后把 `frontend/dist/` 内容复制到：

```text
android/app/src/main/assets/www/
```

Android 工程目标为 Android 16 / API 36，最低支持 Android 8.0 / API 26。
