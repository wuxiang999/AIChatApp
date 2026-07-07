# AI 聊天 Android App

一个纯本地运行的 AI 聊天 Android 应用，直接调用 OpenAI 兼容 API，无需外部 PHP 后端。所有数据本地存储，开箱即用。

## ✨ 功能特性

### 核心功能
- 💬 **多对话管理** - 支持创建、删除、重命名多个独立对话（新窗口/多开）
- 🚀 **流式响应** - 实时 SSE 流式接收 AI 回复，打字机效果
- 🤖 **多模型支持** - 自动获取可用模型列表，一键切换
- 🔌 **多 API 端点** - 支持添加/编辑/删除多个 API 服务器，快速切换
- 🖼️ **图片生成** - 文生图 + 图生图，可配置尺寸、数量、质量、模型
- 🎨 **Material Design 3** - 现代化 UI 设计，深色/浅色主题自动适配
- 💾 **本地持久化** - Room 数据库存储所有对话历史，离线可查看
- 🔐 **隐私安全** - 所有数据本地存储，API Key 仅保存在本地

### 用户体验
- 📱 **完美适配** - 响应式布局，适配各种屏幕尺寸
- 🌓 **主题切换** - 自动跟随系统深色/浅色模式，支持动态取色
- 📋 **侧边导航** - 抽屉式导航栏，快速切换功能模块
- ⚡ **流畅动画** - Jetpack Compose 声明式 UI，流畅过渡动画
- 🧪 **连接测试** - 内置 API 连通性测试，一键验证配置

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin |
| **UI 框架** | Jetpack Compose + Material 3 |
| **架构模式** | MVVM + Repository |
| **依赖注入** | Hilt (Dagger) |
| **网络请求** | Retrofit + OkHttp (直接调用 OpenAI 兼容 API) |
| **本地数据库** | Room (SQLite) |
| **异步处理** | Kotlin Coroutines + Flow |
| **图片加载** | Coil |
| **序列化** | Gson |
| **构建工具** | Gradle KTS |
| **最低 SDK** | API 26 (Android 8.0) |
| **目标 SDK** | API 34 (Android 14) |

## 📁 项目结构

```
AIChatApp/
├── app/
│   └── src/main/
│       ├── java/com/aichat/app/
│       │   ├── AIChatApplication.kt      # Application 入口
│       │   ├── data/
│       │   │   ├── model/                # 数据模型
│       │   │   │   └── Models.kt         # Conversation/Message/ApiEndpoint
│       │   │   ├── local/                # 本地数据库层
│       │   │   │   ├── AppDatabase.kt    # Room 数据库
│       │   │   │   ├── ConversationDao.kt
│       │   │   │   ├── MessageDao.kt
│       │   │   │   └── ApiEndpointDao.kt
│       │   │   ├── remote/               # 网络层 (OpenAI 兼容 API)
│       │   │   │   ├── OpenAIApiService.kt  # API 接口定义
│       │   │   │   └── ApiManager.kt     # 端点管理 + Retrofit 实例
│       │   │   └── repository/           # 数据仓库层
│       │   │       └── ChatRepository.kt # 业务逻辑封装
│       │   ├── di/                       # 依赖注入
│       │   │   └── AppModule.kt
│       │   └── ui/
│       │       ├── MainActivity.kt       # 主 Activity
│       │       ├── AIChatApp.kt          # App 根组件 + 侧边导航
│       │       ├── theme/                # Material 3 主题
│       │       │   ├── Color.kt
│       │       │   ├── Theme.kt
│       │       │   └── Typography.kt
│       │       ├── navigation/           # NavHost 导航
│       │       │   └── NavGraph.kt
│       │       ├── chat/                 # 聊天界面
│       │       │   ├── ChatScreen.kt
│       │       │   └── ChatViewModel.kt
│       │       ├── conversations/        # 对话列表
│       │       │   ├── ConversationsScreen.kt
│       │       │   └── ConversationsViewModel.kt
│       │       ├── imagegen/             # 图片生成
│       │       │   ├── ImageGenScreen.kt
│       │       │   └── ImageGenViewModel.kt
│       │       └── settings/             # 设置界面
│       │           ├── SettingsScreen.kt
│       │           └── SettingsViewModel.kt
│       └── res/                          # 资源文件
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- minSdk 26 (Android 8.0)

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd AIChatApp
```

2. **使用 Android Studio 打开**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择 `AIChatApp` 目录
   - 等待 Gradle 同步完成

3. **配置 API**
   - 启动 App
   - 打开侧边栏 → 设置
   - 点击右下角「添加端点」
   - 输入你的 API 信息（名称、地址、API Key）
   - 点击端点切换为当前使用
   - 点击「测试连接」验证是否可用

4. **运行**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮 ▶️

### 命令行构建

```bash
# 调试版 APK
./gradlew assembleDebug
# 输出路径: app/build/outputs/apk/debug/app-debug.apk

# 发布版 APK (需要签名配置)
./gradlew assembleRelease

# 安装到已连接的设备
./gradlew installDebug
```

### 兼容的 API 服务

本应用兼容任何 OpenAI 格式的 API：
- OpenAI 官方 API (`https://api.openai.com/v1`)
- 第三方代理/中转服务
- 自建 API 服务 (OneAPI、NewAPI、LM Studio 等)
- 任何兼容 `/chat/completions` 和 `/images/generations` 格式的服务

## 🔌 API 接口说明

App 直接调用 OpenAI 兼容格式的 API，主要接口：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/models` | GET | 获取可用模型列表 |
| `/chat/completions` | POST | 聊天补全（流式/非流式） |
| `/images/generations` | POST | 文生图 |
| `/images/edits` | POST | 图生图（图片编辑） |

所有请求在 Header 中携带 `Authorization: Bearer <API_KEY>`。

## 🎨 UI 设计

### 界面构成

1. **对话列表页**
   - 展示所有历史对话卡片
   - 支持点击进入、重命名、删除
   - 浮动按钮快速新建对话
   - 自动以第一条消息生成标题

2. **聊天页面**
   - 气泡式消息展示
   - AI 用户头像区分
   - 流式打字机效果
   - 生成中可随时停止
   - 底部输入框，支持多行

3. **图片生成页**
   - 自定义提示词输入
   - 模型选择 (gpt-image-2 / dall-e-3 / dall-e-2)
   - 尺寸选择 (256x256 ~ 1792x1024)
   - 数量选择 (1~10 张)
   - 质量选择 (标准 / 高清)
   - 网格展示生成结果

4. **设置页面**
   - 当前端点状态展示
   - 端点管理（添加/编辑/删除/切换）
   - 连接测试功能
   - 模型列表加载展示

### 设计规范
- Material Design 3 (Material You)
- 紫色主色调 (Primary: #6750A4)
- 圆角卡片 + 圆润按钮
- 清晰的视觉层级
- 流畅的过渡动画
- 支持动态取色 (Android 12+)

## 💬 功能详解

### 多对话管理（新窗口/多开）
- ✅ 每个对话完全独立，互不干扰
- ✅ 可同时开启多个对话（通过对话列表切换）
- ✅ 自定义对话标题
- ✅ 自动生成标题（第一条消息前 20 字）
- ✅ 一键新建对话
- ✅ 支持删除和重命名
- ✅ 按更新时间倒序排列

### 流式响应
- ✅ SSE (Server-Sent Events) 实时接收
- ✅ 打字机效果逐步展示回复
- ✅ 支持随时停止生成
- ✅ 生成中显示光标动画
- ✅ 错误信息清晰展示

### API 端点管理
- ✅ 支持添加多个 API 服务器
- ✅ 一键切换当前端点
- ✅ 端点信息本地加密存储
- ✅ 连接测试功能（获取模型数量）
- ✅ 编辑和删除端点
- ✅ 预置默认端点（可删除）

### 图片生成
- ✅ 文生图 (Text to Image)
- ✅ 图生图 (Image to Image / Edit)
- ✅ 自定义提示词
- ✅ 可选尺寸：256x256 / 512x512 / 1024x1024 / 1024x1792 / 1792x1024
- ✅ 可选数量：1~10 张
- ✅ 可选质量：标准 / 高清 (HD)
- ✅ 可选模型：gpt-image-2 / dall-e-3 / dall-e-2
- ✅ 网格展示生成结果
- ✅ 加载状态和错误提示

## 🔧 开发指南

### 架构设计

```
UI Layer (Compose + ViewModel)
           ↓
  Repository Layer
           ↓
┌──────────┴──────────┐
│                     │
Local DB           Remote API
 (Room)         (Retrofit/OkHttp)
```

### 添加新功能

1. 在 `data/model/Models.kt` 中添加数据模型
2. 如需要本地存储，在 `data/local/` 添加 DAO
3. 如需要网络请求，在 `data/remote/OpenAIApiService.kt` 添加接口
4. 在 `data/repository/ChatRepository.kt` 封装业务逻辑
5. 在 `ui/` 下创建对应 Screen 和 ViewModel
6. 在 `ui/navigation/NavGraph.kt` 注册路由

### 配置 API 地址

在应用内动态配置，无需修改代码：
- 打开侧边栏 → 设置
- 添加或编辑 API 端点
- 切换为当前端点即可生效

## 📋 变更记录

### v1.0.0
- 🎉 初始版本发布
- 💬 多对话管理（支持多个对话/新窗口）
- 🚀 流式聊天响应
- 🔌 多 API 端点管理
- 🖼️ 图片生成（文生图 + 图生图）
- 🎨 Material 3 UI + 深色/浅色主题
- 💾 Room 本地数据持久化

## 📄 License

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**本应用为纯本地客户端，所有对话数据保存在设备上。AI 能力由第三方 API 服务提供，需自行准备 API Key。**
