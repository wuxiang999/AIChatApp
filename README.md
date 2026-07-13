# 月下AI - Android 聊天应用

一个纯本地运行的 AI 聊天 Android 应用，直接调用 OpenAI 兼容 API，无需外部 PHP 后端。所有数据本地存储，开箱即用。

## ✨ 功能特性

### 核心功能
- 💬 **多对话管理** - 支持创建、删除、重命名多个独立对话
- 🚀 **流式响应** - 实时 SSE 流式接收 AI 回复，打字机效果
- 🤖 **多模型支持** - 自动获取可用模型列表，聊天界面一键切换
- 🔌 **多 API 端点** - 支持添加/编辑/删除多个 API 服务器，快速切换
- 🖼️ **图片生成** - 整合到聊天中，输入 `/img` 命令或切换图片模式即可生成
- 📤 **图片上传** - 支持上传图片进行多模态对话
- 🎨 **月下主题** - 深蓝夜空 + 银白月光配色，现代化 UI 设计
- 💾 **本地持久化** - Room 数据库存储所有对话历史，离线可查看
- 🔐 **隐私安全** - 所有数据本地存储，API Key 仅保存在本地

### 用户体验
- 📱 **完美适配** - 响应式布局，适配各种屏幕尺寸
- 🌓 **主题切换** - 自动跟随系统深色/浅色模式
- 📋 **侧边导航** - 抽屉式导航栏，快速切换功能模块，支持关闭按钮
- ⚡ **流畅动画** - Jetpack Compose 声明式 UI，流畅过渡动画
- 🧪 **连接测试** - 每个端点独立支持测试连接和加载模型

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

# 发布版 APK
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

## 📋 变更记录

### v2.2.5
- 🐛 **修复点击选择模型按钮闪退** - 根治 ExposedDropdownMenu + LazyColumn 固有测量崩溃
  - 根因：`ModelPicker` 的 `ExposedDropdownMenu` 内部嵌套了 `LazyColumn`。`ExposedDropdownMenu` 内部有 `width(IntrinsicSize.Min)`，触发固有宽度测量传播到 `LazyColumn`，抛出 `IllegalStateException: LazyColumn does not support intrinsic measurements`
  - 修复：用 `ModalBottomSheet` 彻底重写 `ModelPicker`，不再使用 `ExposedDropdownMenu`。`ModalBottomSheet` 不依赖 Popup、不走固有测量，`LazyColumn` 可安全使用
  - 搜索框从 Popup 内移至 BottomSheet 内，消除焦点/IME 冲突
- 🎨 **bottomBar inset 适配** - 修复 edge-to-edge 下输入栏被导航栏/IME 遮挡
  - `ChatInputBar` 根 `Surface` 添加 `navigationBarsPadding()` 和 `imePadding()`
  - `MainActivity` 添加 `windowSoftInputMode="adjustResize"`

### v2.2.4
- 🐛 **彻底修复选择端点/模型时闪退** - 根治 Retrofit 非法 URL 导致的 IllegalArgumentException 崩溃
  - `ApiManager.updateRetrofit()` 入口处用 `toHttpUrlOrNull()` 校验 URL 合法性，非法 URL（缺少 http/https 前缀、空串等）不再抛异常，保持旧 service 不变
  - `ApiManager.getApiServiceForEndpoint()` / `testEndpoint()` 同步加入 URL 校验
  - `ApiManager` 三个可变字段（currentEndpoint/retrofit/apiService）标记 `@Volatile`，修复多线程并发读写竞态
  - `ApiManager.deleteEndpoint()` 回退选择端点时补充 try-catch
  - 移除未使用且无异常保护的 `setCurrentEndpoint()` 方法
- 🛡️ **全链路异常保护加固** - 所有 ViewModel 的协程方法补充 try-catch，杜绝未捕获异常导致闪退
  - `ChatViewModel`: `selectEndpoint` 的 `loadModels()` 移入 try 成功分支；`setModel`/`clearConversation`/`revokeMessage` 补充异常保护
  - `SettingsViewModel`: `addEndpoint`/`updateEndpoint`/`deleteEndpoint`/`testEndpoint`/`testEndpointForId`/`loadEndpoints` 补充异常保护
  - `ConversationsViewModel`: `loadConversations`/`deleteConversation`/`updateConversationTitle` 补充异常保护

### v2.2.3
- 🐛 **闪退修复** - 修复进入聊天界面时闪退问题
  - `ChatViewModel` 的 `initializeAgents` / `loadMessages` / `loadConversationInfo` / `loadEndpoints` 四个 init 协程补充 try-catch 异常保护，避免 DB 或 Flow 异常作为未捕获异常导致崩溃
  - `AIChatApplication.onCreate()` 的 `apiManager.initialize()` 协程补充 `CoroutineExceptionHandler` 与 try-catch，避免初始化失败（如 URL 格式异常）导致启动崩溃
- ⌨️ **输入框文字显示修复** - 修复聊天输入框文字被截断问题
  - 输入框移除固定 `.height(44.dp)` 约束，改为 `.heightIn(min = 44.dp)`，输入框可随内容自适应增高
  - `maxLines` 从 2 提升到 5，多行输入文字完整显示不被裁剪

### v2.2.2
- 🔧 **参考 v1.8.0 优化端点/模型选择器显示**
  - 移除 EndpointPicker / ModelPicker 的固定 `.height(44.dp)` 约束，避免文字被截断
  - 文字字号从 `bodySmall` / `bodyMedium` 统一为 `bodyLarge`（显示值）和 `bodyMedium`（标签/菜单项）
  - 模型列表项 `maxLines` 从 1 提升到 2，长模型名不再被省略号截断
  - 下拉菜单圆角统一为 16dp，与 v1.8.0 视觉风格一致
  - 模型搜索框字号提升到 `bodyMedium`，placeholder 和输入文字更清晰
  - 模型列表高度从 200dp 提升到 240dp，显示更多模型

### v2.2.1
- 🐛 **闪退修复** - 修复进入对话列表、选择 API 端点时崩溃问题
  - `ApiManager.initialize()` 插入默认端点后使用数据库真实 id，修复内存 id=0 与数据库不一致
  - `selectEndpoint()` 找不到目标 id 时回退到第一个端点，避免 `apiService` 为 null 导致崩溃
  - `getApiService()` 在 `apiService` 为 null 时尝试用 `currentEndpoint` 创建临时 service
  - `addEndpoint()` 返回 DAO 插入的真实 id（DAO 返回类型改为 `Long`）
  - `ChatViewModel`/`SettingsViewModel` 的 `selectEndpoint` 添加 try-catch 异常保护
- 🎨 **文字显示修复** - 修复 API 端点和模型列表下拉框文字不可见问题
  - 主题显式设置 Material3 1.2.0 新增的 `surfaceContainer` / `surfaceContainerHigh` 等 7 个颜色角色
  - `menuTextFieldColors()` 改用 `surfaceVariant.copy(alpha=0.5f)`，并补充 label 颜色
  - `ModelPicker` / `SettingsScreen` 模型搜索框补充 `focusedTextColor` / `unfocusedTextColor`

### v2.2.0
- 🎨 **UI 全面重构** - 月下主题配色与字体系统升级，新增完整 Material 3 字号体系
- 🏗️ **逻辑重构** - 抽屉、ChatScreen、SettingsScreen 等模块组件化拆分，提取 `MessageBubble` 独立文件，逻辑分层更清晰
- 🌗 **粒子背景主题感知** - 粒子动画改为跟随主题主色 / 三色取色，自动适配深浅模式
- 💬 **消息气泡现代化** - AI/用户头像渐变、思考过程卡片改用 `tertiaryContainer`，撤回/下载逻辑保留
- 📋 **侧边栏重构** - 头部加入品牌渐变 Logo 与描述行，导航项支持主副双行文本
- 🗂️ **对话列表/智能体卡片** - 头像改用主色-三色渐变，模型标签独立高亮
- ⚙️ **设置界面重组** - 端点卡片视觉统一，新增分区头与端点选中渐变指示器
- 🔧 整体代码组织优化，未引入任何外部新依赖
- ✅ 保留 v2.1.x 全部功能：多对话、流式响应、思考内容、多端点、模型搜索、图片生成/图生图、文件上传、消息撤回、智能体

### v2.1.1
- 🐛 **闪退修复** - 修复聊天界面进入时闪退问题，恢复默认API端点自动创建逻辑
- 🚀 **稳定性提升** - 确保数据库为空时应用正常启动，不会因ApiManager未初始化而崩溃
- 📝 **版本更新** - 更新版本号到2.1.1

### v2.1.0
- 🤖 **智能体系统** - 新增智能体系统，支持8个预设角色（通用助手、程序员、写作、翻译、学习导师、数据分析师、心理咨询师、营销策划）
- ➕ **自定义智能体** - 支持创建、删除自定义智能体，配置专属系统提示词
- 🔒 **安全修复** - 移除代码中硬编码的 API Key，保护用户账户安全
- 🔒 **安全增强** - 签名密钥改为通过环境变量/GitHub Secrets 注入，不再硬编码
- 🎨 **设置界面优化** - 精简当前API端点卡片布局，去除冗余信息
- 🔧 **合并重复按钮** - 端点管理中的"测试"和"模型"按钮合并为统一的"加载模型"
- 📱 **模型选择器重构** - 全新模型列表，支持搜索过滤，选中后显示对号标记
- 📝 **模型显示优化** - 列表式展示，模型名称完整显示不截断
- ⌨️ **输入框扁平化** - 聊天输入框更扁更紧凑，提升屏幕利用率
- 🎯 **下拉选择器优化** - API端点和模型选择器高度减小，样式更精致
- ⚡ 整体UI布局优化和细节改进

### v2.0.2
- 🔧 修复 GitHub Actions 构建失败问题（android-actions/setup-android API 更新）
- ⚡ 优化 CI/CD 工作流配置

### v2.0.0
- 🔄 发送者消息支持长按撤回
- 🧠 AI 机器人完整支持显示 reasoning_content 思考内容
- ⚡ 代码逻辑优化和 Bug 修复

### v1.9.0
- 🧠 修复思考模型 content 为空的问题：支持 `message` 格式完整内容替换
- ⌨️ 发送消息后自动回收输入法（隐藏软键盘）
- ⚡ 代码逻辑优化和 Bug 修复

### v1.8.0
- 🖥️ 修复下拉菜单文本显示问题（API端点、模型、数量、尺寸）
- 🧠 增强思考内容解析，支持多种字段名（reasoning_content、thinking_content、reasoning、thought）
- 💬 增强响应内容解析，支持delta和message两种格式
- 🔄 优化流式输出显示，添加转圈动画提示
- ⚡ 代码逻辑优化和 Bug 修复

### v1.7.0
- 🔧 重构图生图模式逻辑，上传图片后自动开启图生图
- 🎯 优化发送按钮逻辑，根据模式正确判断启用条件
- ✨ 修复图生图发送后状态重置问题
- 🔐 签名验证通过，确保可覆盖安装
- ⚡ 代码逻辑优化和 Bug 修复

### v1.6.0
- 🧠 修复思考内容显示问题，增强多种格式兼容性
- 🎨 重新设计整体UI，浅蓝简约风格
- 🖼️ 新增图生图功能（Multipart表单格式）
- 📐 图片生成支持选择生成数量和尺寸
- 🔌 聊天界面支持快速切换API端点
- ✏️ 侧边栏UI重新设计，更加简洁美观
- 🔐 统一签名密钥，确保可覆盖安装
- ⚡ 优化网络请求稳定性

### v1.5.0
- 🖼️ 修复图片生成 503 错误（从 Multipart 表单改为 JSON 请求体格式）
- 💾 支持下载生成的图片到本地相册
- 🔌 聊天界面新增 API 端点快速切换功能
- ⚙️ 修复 GitHub Actions 工作流构建配置
- 🚀 优化网络请求稳定性

### v1.4.0
- 🧠 支持显示 AI 思考内容 (reasoning_content)，可展开/收起
- 📁 添加文件上传功能，支持 txt、php 等文本文件
- 🎨 UI 改为简约白色主题，清爽配色
- ✨ 添加粒子特效背景，增强视觉效果
- ⚡ 性能优化和代码清理

### v1.3.0
- 🗑️ 删除右下角浮动 + 号按钮，统一在右上角 TopAppBar
- 🎯 优化 UI 布局结构，避免双重 TopAppBar
- 🐛 修复对话列表空状态提示文字
- ⚡ 性能优化和代码清理

### v1.2.0
- 🎉 应用更名为「月下AI」
- 📍 聊天界面「+」号按钮移到左上角 TopAppBar
- ⌨️ 回车键改为换行，不再直接发送消息
- 🐛 修复 AI 回复空白问题（确保消息正确发送到 API）
- 🖼️ 图片生成整合到聊天界面（输入 `/img 描述` 即可生成）
- 📤 支持上传图片进行多模态对话
- 🎨 更新月下主题配色（深蓝夜空 + 银白月光）
- 🔧 每个 API 端点独立支持测试连接和加载模型

### v1.1.0
- 🎨 全新月下主题 UI（深蓝夜空 + 银白月光配色方案）
- ✕ 侧边栏新增关闭按钮
- ⌨️ 聊天支持回车发送消息
- 🤖 支持在聊天界面选择模型进行对话
- 🖼️ 图片生成整合到聊天中
- 📤 聊天支持上传图片文件
- 🧪 设置中每个端点独立支持测试连接和加载模型

### v1.0.0
- 🎉 初始版本发布
- 💬 多对话管理
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
