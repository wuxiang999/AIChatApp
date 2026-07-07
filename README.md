# AI 聊天 Android App

基于 PHP 后端 AI 聊天系统的 Android 客户端，使用 Kotlin + Jetpack Compose + Material 3 构建。

## ✨ 功能特性

### 核心功能
- 💬 **多对话管理** - 支持创建、删除、重命名多个独立对话
- 🚀 **流式响应** - 实时流式接收 AI 回复，打字机效果
- 🤖 **多模型支持** - 可切换不同的 AI 模型
- 🔌 **多 API 端点** - 支持配置和切换多个 API 服务器端点
- 🖼️ **图片生成** - 支持文生图，可配置尺寸和数量
- 📱 **完美适配** - Material Design 3 设计语言，支持深色/浅色主题

### 用户体验
- 🎨 **现代化 UI** - Jetpack Compose 声明式 UI，流畅动画
- 🌓 **主题适配** - 自动跟随系统深色/浅色模式
- 📋 **侧边导航** - 抽屉式导航栏，快速切换功能模块
- ⚡ **本地存储** - Room 数据库持久化对话历史
- 🔄 **离线可用** - 本地缓存历史记录

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin |
| **UI 框架** | Jetpack Compose + Material 3 |
| **架构模式** | MVVM + Repository |
| **依赖注入** | Hilt |
| **网络请求** | Retrofit + OkHttp |
| **本地数据库** | Room |
| **异步处理** | Kotlin Coroutines + Flow |
| **图片加载** | Coil |
| **序列化** | Gson |
| **构建工具** | Gradle KTS |

## 📁 项目结构

```
AIChatApp/
├── app/
│   └── src/main/
│       ├── java/com/aichat/app/
│       │   ├── AIChatApplication.kt      # Application 类
│       │   ├── data/
│       │   │   ├── model/                # 数据模型
│       │   │   │   └── Models.kt
│       │   │   ├── local/                # 本地数据库
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── ConversationDao.kt
│       │   │   │   ├── MessageDao.kt
│       │   │   │   └── ApiEndpointDao.kt
│       │   │   ├── remote/               # 网络层
│       │   │   │   ├── ChatApiService.kt
│       │   │   │   └── ApiManager.kt
│       │   │   └── repository/           # 数据仓库
│       │   │       └── ChatRepository.kt
│       │   ├── di/                       # 依赖注入
│       │   │   └── AppModule.kt
│       │   └── ui/
│       │       ├── MainActivity.kt       # 主 Activity
│       │       ├── AIChatApp.kt          # App 根组件
│       │       ├── theme/                # 主题配置
│       │       │   ├── Color.kt
│       │       │   ├── Theme.kt
│       │       │   └── Typography.kt
│       │       ├── navigation/           # 导航
│       │       │   └── NavGraph.kt
│       │       ├── chat/                 # 聊天界面
│       │       │   ├── ChatScreen.kt
│       │       │   └── ChatViewModel.kt
│       │       ├── conversations/        # 对话列表
│       │       │   ├── ConversationsScreen.kt
│       │       │   └── ConversationsViewModel.kt
│       │       ├── imagegen/             # 图片生成
│       │       │   └── ImageGenScreen.kt
│       │       └── settings/             # 设置界面
│       │           ├── SettingsScreen.kt
│       │           └── SettingsViewModel.kt
│       └── res/                          # 资源文件
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
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

3. **配置 API 端点**
   - 启动 App
   - 打开侧边栏 → 设置
   - 添加你的 PHP 后端地址（例如：`http://10.0.2.2/` 或你的服务器地址）
   - 选中该端点

4. **运行**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮 ▶️

### 命令行构建

```bash
# 调试版
./gradlew assembleDebug

# 发布版
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 🔌 API 接口说明

App 对接 PHP 后端的以下接口：

| 接口 | 方法 | 说明 |
|------|------|------|
| `?action=get_endpoints` | GET | 获取 API 端点列表 |
| `?action=get_announcement` | GET | 获取系统公告 |
| `?action=get_models` | GET | 获取可用模型列表 |
| `?action=chat` | POST | 非流式聊天 |
| `?action=chat_stream` | POST | 流式聊天 (SSE) |
| `?action=get_history` | POST | 获取对话历史 |
| `?action=save_message` | POST | 保存消息 |
| `?action=clear_conversation` | POST | 清空对话 |
| `?action=upload` | POST | 上传文件/图片 |
| `?action=image_generate` | POST | 图片生成 |

## 🎨 UI 设计

### 界面构成
1. **对话列表页** - 展示所有历史对话，支持新建/删除/重命名
2. **聊天页面** - 对话主界面，流式消息展示
3. **图片生成页** - AI 绘图功能
4. **设置页面** - API 端点管理、公告查看

### 设计规范
- 遵循 Material Design 3 规范
- 紫色为主色调（Primary: #6750A4）
- 圆润的卡片和按钮设计
- 清晰的视觉层级
- 流畅的过渡动画

## 📝 功能详解

### 多对话管理
- 每个对话独立存储，互不干扰
- 可自定义对话标题
- 一键新建对话
- 支持删除和重命名

### 流式响应
- SSE (Server-Sent Events) 实时接收
- 打字机效果逐步展示回复
- 支持随时停止生成
- 生成中显示加载动画

### API 端点管理
- 支持添加多个 API 服务器
- 一键切换端点
- 本地存储配置
- 自动选中当前端点

### 图片生成
- 支持自定义提示词
- 可选图片尺寸（256x256 ~ 1792x1024）
- 可选生成数量（1~10 张）
- 网格展示生成结果

## 🔧 开发指南

### 添加新功能
1. 在 `data/model` 中添加数据模型
2. 在 `data/local` 或 `data/remote` 添加数据源
3. 在 `data/repository` 添加业务逻辑
4. 在 `ui` 下创建对应 Screen 和 ViewModel
5. 在 `navigation/NavGraph.kt` 注册路由

### 数据层架构
```
UI (Composable)
    ↓
ViewModel
    ↓
Repository
    ↓
┌───────┴───────┐
│               │
Local DB      Remote API
(Room)       (Retrofit)
```

## 📄 License

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**注意**：本项目需要配合对应的 PHP 后端使用。请确保后端服务正常运行，并在 App 设置中配置正确的 API 地址。
