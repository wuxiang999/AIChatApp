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

### v2.3.7
- ✨ **AI 回复的思考内容与回复内容支持滚动并自动显示最新内容** - 流式响应实时跟随
  - 背景：此前思考内容（ReasoningCard）与回复内容（Card 内 Text）均使用普通 `Text`，内容随流式输出无限增长，单条消息会撑满甚至超出屏幕，思考阶段最新内容不易查看
  - 修复：
    - **思考内容**：`ReasoningCard` 内容 `Text` 增加 `heightIn(max = 200.dp)` + `verticalScroll`，限制最大高度并可内部滚动；新增 `isStreaming` 参数，`LaunchedEffect(content, expanded)` 在展开且流式输出时调用 `animateScrollTo(maxValue)` 自动滚动到底部，实时显示最新思考内容
    - **回复内容**：回复 `Text` 增加 `heightIn(max = 320.dp)` + `verticalScroll`，`LaunchedEffect(message.content)` 在 `isStreaming` 时自动滚动到底部，实时显示最新回复内容
    - 非流式（历史消息）时不自动滚动，默认从顶部开始阅读，符合阅读习惯
  - 效果：单条超长消息不再撑满屏幕，思考与回复各自在限定高度内滚动并自动跟随到最新内容

### v2.3.6
- 🐛 **修复设置/智能体页面顶部双重 TopAppBar 导致的留白** - 根治嵌套 Scaffold 各自渲染 TopAppBar 的问题
  - 根因：外层 `AIChatApp` 的 `Scaffold` 已有 `TopAppBar` 显示「设置」/「智能体」标题（含菜单按钮），但内层 `SettingsScreen` / `AgentsScreen` 又各自渲染了一个 `TopAppBar`，显示相同标题 + 副标题。两个 TopAppBar 堆叠，看起来像标题下方有一大块空白。v2.3.5 的 `contentWindowInsets=0` 对此无效，因为那里实际是第二个标题栏
  - 修复：将副标题（「管理 API 端点与可用模型」/「选择一个智能体开始对话」）合并到外层 `AIChatApp` 的 TopAppBar 标题 Column 中，移除 `SettingsScreen` / `AgentsScreen` 内层的 `topBar` 及 `nestedScroll`/`scrollBehavior`
  - 效果：设置/智能体页面只剩单一标题栏，无双重头部、无多余留白
- 🎨 **聊天界面 API 端点与模型选择按钮统一尺寸** - 视觉一致性优化
  - 根因：`EndpointPicker` 使用 `ExposedDropdownMenuBox` + `OutlinedTextField`（带 label，高度较大），`ModelPicker` 使用 `Surface` + `Row` + `Column`（label 与值纵向排列，高度较小），两者结构不同导致尺寸不一致
  - 修复：将 `EndpointPicker` 触发器重写为与 `ModelPicker` 完全一致的 `Surface` + `Row` + `Column(label, value)` + `KeyboardArrowDown` 图标结构，相同的 padding(12/6)、圆角(12dp)、配色，下拉改用 `DropdownMenu`
  - 效果：API 端点按钮与模型选择按钮高度、样式完全一致

### v2.3.5
- 🐛 **修复 TopAppBar 与内容之间大片留白** - 根治嵌套 Scaffold 双重 windowInsets padding 问题
  - 根因：外层 `AIChatApp` 的 `Scaffold` 已有 `TopAppBar` 消费了状态栏 inset，但内层各页面（ChatScreen / SettingsScreen / AgentsScreen / ImageGenScreen）的 `Scaffold` 默认 `contentWindowInsets = ScaffoldDefaults.contentWindowInsets`（系统栏）会再次把状态栏高度作为 padding 加到内容顶部，导致标题「月下AI」与下方人设指示条之间出现一大块空白，设置/智能体/图片生成页面同样存在
  - 修复：为四个内层 `Scaffold` 显式设置 `contentWindowInsets = WindowInsets(0, 0, 0, 0)`，不再重复应用系统栏 inset，消除双重 padding 产生的空白
  - 效果：所有页面 TopAppBar 紧贴内容，无多余空白

### v2.3.4
- 🐛 **修复 AI 回复时屏幕不自动滚动** - 流式响应实时跟随到底部
  - 根因：原 `LaunchedEffect(messages.size, isLoading)` 只在消息数量变化时触发滚动，但流式响应时消息数量不变（只是最后一条消息的 content 不断更新），导致 AI 回复时屏幕不滚动，用户看不到实时加载内容
  - 修复：`LaunchedEffect` 增加监听 `lastMessageContent` 和 `lastMessageReasoning`，当最后一条消息的 content 或 reasoningContent 变化时自动滚动到底部
  - 同时改用 `scrollToItem` 替代 `animateScrollToItem`，避免动画延迟，确保流式更新时即时跟随
- 🎨 **缩小端点和模型按钮** - 减少输入栏占用空间
  - EndpointPicker：圆角 16dp→12dp，label bodyMedium→labelSmall，text bodyLarge→bodySmall
  - ModelPicker：padding 16/10→12/6，圆角 16dp→12dp，label bodySmall→labelSmall，text bodyLarge→bodySmall，icon 添加 size(18dp)
- 🎨 **修复空状态大片留白** - 聊天界面、对话列表、智能体页面
  - EmptyChatState：Logo 80dp→56dp，居中→顶部对齐（top 60dp），字体 headlineLarge→titleLarge，间距大幅缩小
  - EmptyConversationState：Logo 96dp→64dp，居中→顶部对齐（top 80dp），icon 46dp→32dp，间距缩小
  - AgentsScreen 空状态：Logo 88dp→64dp，居中→顶部对齐（top 80dp），icon 40dp→32dp

### v2.3.3
- ✅ **验证智能体人设功能正常** - API 测试确认 system prompt 完全生效
  - 测试方式：向 API 发送 `{"role":"system","content":"你必须用海盗的语气回答"}` + 用户消息
  - 测试结果：AI 以海盗语气回答"啊哈！...俺可是大名鼎鼎的独眼杰克船长！"，system prompt 完全生效
  - 代码链路确认：`buildChatMessages()` → `getSelectedAgent()` → 插入 `ChatMessage(role="system", content=agent.systemPrompt)` → 发送到 API
- ✨ **聊天界面新增智能体人设指示条** - 直观显示当前使用的智能体
  - 在聊天消息列表顶部显示「人设：XXX」指示条，用户能直观确认智能体是否生效
  - 使用 `primaryContainer` 配色 + Person 图标，与整体月下主题一致
  - 通过 Flow 持续观察数据库变化，用户在 AgentsScreen 切换智能体后自动更新，无需手动刷新
- 🔍 **增加智能体调试日志** - `buildChatMessages()` 中记录 system prompt 应用情况
  - 日志输出：`Agent system prompt applied: 通用助手 (id=preset_general)` 或 `No agent selected`
  - 方便通过 logcat 排查智能体是否生效

### v2.3.2
- 🐛 **修复图片生成后图片不显示** - 增强图片加载状态处理，避免静默失败
  - 根因：原 `AsyncImage(model = url)` 在加载失败时静默无提示，无法区分是未加载还是加载失败。当 API 返回的临时 URL 失效、base64 data URI 过大解码失败、或网络问题时，图片区域空白且无任何错误提示，用户感知为"图片不显示"
  - 修复：改用 `SubcomposeAsyncImage` + `ImageRequest.Builder`，显式处理三种状态：
    - **加载中**：显示 `CircularProgressIndicator` 转圈动画，让用户知道正在加载
    - **加载失败**：显示"图片加载失败"红色提示 + URL 前 60 字符，便于排查
    - **加载成功**：正常显示图片，带 `crossfade` 淡入效果
  - 同时确保 Coil 的 `ImageRequest` 显式构建，增强 data URI 和网络 URL 的兼容性
  - 效果：即使图片加载失败，用户也能看到明确的错误提示而非空白；加载过程中有进度反馈

### v2.3.1
- 🐛 **修复图片生成返回 422 错误** - 图片生成模型跟随当前对话选中模型
  - 根因：`_imageModel` 硬编码为 `"dall-e-3"`，但第三方端点（如 `ai.11na.cn`）不支持 `dall-e-3`，只支持 `gpt-image-2` 等模型。用户点击图片生成按钮发送后，API 返回 `422 {"error":{"message":"model not found: dall-e-3"}}`
  - 修复：`generateImage()` / `editImage()` 改用 `_currentModel.value`（当前对话选中的模型）作为图片生成模型。用户在对话界面顶部选择 `gpt-image-2` 后，图片生成自动使用该模型
  - 效果：用户选择对应端点支持的图片模型即可正常生成，不再因硬编码 `dall-e-3` 导致 422

### v2.3.0
- 🐛 **修复图片生成时发送按钮不显示停止状态** - 同步 `isLoading` 与 `isGeneratingImage` 状态
  - 根因：`generateImage()` / `editImage()` 只设置 `_isGeneratingImage.value = true`，但 UI 的发送/停止按钮切换依赖 `_isLoading`。图片生成期间发送按钮仍显示为发送图标而非停止图标，用户无法直观感知正在生成
  - 修复：图片生成开始时同步设置 `_isLoading.value = true`，结束时同步重置为 `false`，发送按钮正确切换为停止状态
  - 同时在 `generateImage()` / `editImage()` 入口增加 `_isLoading.value` 检查，避免图片生成与聊天流式响应并发
- ✅ **全功能联调测试验证** - 文生图、图生图、思考模型对话、非思考模型对话
  - 文生图（`/v1/images/generations`）：点击图片模式按钮 → 输入描述 → 发送 → API 返回 `url` + `b64_json` → APP 正确显示图片 ✅
  - 图生图（`/v1/images/edits` Multipart）：上传图片 → 自动开启图生图模式 → 输入描述 → 发送 → API 返回 `url` + `b64_json` → APP 正确显示图片 ✅
  - 思考模型对话（deepseek-v4-flash）：`reasoning_content` 思考过程 + `content` 对话内容分别独立显示 ✅
  - 非思考模型对话（gpt-5.6-luna）：`content` 流式对话内容正常显示 ✅
  - 图片下载功能：支持网络 URL 和 base64 data URI 两种格式 ✅
- 🔗 **跨端点兼容性验证** - 两种 `finish_reason` 格式均被 v2.2.9 修复覆盖
  - `fengsili.online`：中间 chunk `finish_reason: ""`（空字符串）
  - `ai.11na.cn`：中间 chunk `finish_reason: null`（JSON null）
  - `isNullOrEmpty().not()` 判断同时兼容两种格式

### v2.2.9
- 🐛 **严重修复：思考模型只显示对话内容第一个 chunk 就中断** - 根治 `finish_reason` 空字符串判断错误
  - 根因：`processStreamResponse()` 中判断 `if (choice.finish_reason != null) break`，但 API 流式响应的每个 chunk 都带 `finish_reason: ""`（空字符串），只有最后一个 chunk 是 `"stop"`。空字符串 `""` 不是 null，导致第一个 content chunk 到达后就 break，后续对话内容全部丢失
  - 修复：改为 `if (choice.finish_reason.isNullOrEmpty().not()) break`，只有 `finish_reason` 为非空字符串（如 `"stop"`/`"length"`）才退出循环
  - 现象：deepseek-v4-flash 等思考模型只显示思考过程卡片 + 对话气泡的"是的"开头，后续内容全丢
- 🚀 **网络超时优化** - 适配深度思考模型的长响应时间
  - `readTimeout` 从 120s 提升到 300s，避免思考阶段耗时较长导致连接超时断开
  - `writeTimeout` 从 30s 提升到 60s，适配图片上传等大请求体
- 🔇 **日志级别优化** - `HttpLoggingInterceptor` 从 `BODY` 降为 `BASIC`
  - `BODY` 级别会对流式响应（SSE）进行缓冲复制，可能干扰流式读取并产生大量日志
  - `BASIC` 级别仅记录请求/响应行，避免对流式响应的潜在干扰
- ✅ **测试验证 gpt-image-2 绘画功能** - 文生图 + 图生图全链路联调确认正常
  - 测试端点：`https://ai.11na.cn/v1`，模型：`gpt-image-2`
  - 文生图（`/v1/images/generations`）：成功返回 `url` + `b64_json`，APP 正确显示图片
  - 图生图（`/v1/images/edits` Multipart 表单）：成功返回 `url` + `b64_json`，APP 正确显示图片
  - 图片下载功能支持网络 URL 和 base64 data URI 两种格式

### v2.2.8
- ✅ **测试验证 DeepSeek 风格 reasoning_content 思考内容显示** - 全链路联调确认正常
  - 测试端点：`https://api.fengsili.online/v1`，模型：`deepseek-v4-flash`
  - 验证 API 返回流式 SSE：先输出 `delta.reasoning_content`（思考过程），再输出 `delta.content`（最终回答）
  - 全链路打通：`StreamDelta` 解析 `reasoning_content`/`thinking_content`/`reasoning`/`thought` 多字段 → `ChatViewModel.processStreamResponse()` 累积 `fullReasoning` → `MessageDao.updateMessageReasoning()` 持久化 → `MessageBubble` 渲染 `ReasoningCard` 可展开收起的「思考过程」卡片
  - 对话内容与思考内容分别独立显示，互不干扰，UI 体验符合预期

### v2.2.7
- 🐛 **修复点击当前模型不显示模型列表** - 根治 readOnly TextField 点击事件被拦截问题
  - 根因：ModelPicker 触发器使用了 `readOnly = true` 的 `OutlinedTextField` + `Modifier.clickable`。readOnly TextField 的内部 `pointerInput` 会拦截点击事件（用于获取焦点），导致外部 `Modifier.clickable` 收不到点击，`onExpandedChange(true)` 永远不会被调用，ModalBottomSheet 无法展开
  - 修复：用 `Surface` + `Row` + `Text` + `Icon` 重写触发器，整个区域可点击，避免 TextField 的点击事件拦截
- ✅ 排查确认 EndpointPicker 和 ImageOptionsRow 点击正常（使用标准 `ExposedDropdownMenuBox` + `menuAnchor()`，无同类问题）

### v2.2.6
- ✨ **模型选择器增强** - 完善当前端点模型选择与搜索功能
  - 移除 `availableModels.isNotEmpty()` 显示条件，模型选择器始终显示，确保随时可选择模型
  - 新增刷新按钮，可手动重新拉取当前端点的模型列表（网络失败或模型未加载时可一键刷新）
  - 当前端点无模型时显示友好提示「当前端点暂无模型，点击刷新重新加载」，点击区域可触发刷新
  - 标题栏显示模型总数（共 N 个），搜索无结果时显示「未找到匹配「关键词」的模型」
  - 搜索功能支持实时过滤，不区分大小写

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
