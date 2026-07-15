# CodeVibe — Android Vibe Coding Agent

**用自然语言编写 Android 应用。**

CodeVibe 是一个开源的 Android 原生 AI 编程代理。它不只是聊天机器人——它能读写文件、执行命令、搜索网络、调用 MCP 工具链，自主完成编码任务。所有数据本地存储，隐私安全。

## ✨ 核心特性

### 🤖 AI 编程代理
- **自主编码循环**：Think → Act → Observe 闭环，AI 自动规划并执行多步骤编码任务
- **文件系统访问**：读取/写入项目文件，安全路径隔离
- **命令执行**：沙箱化的 shell 命令执行，危险命令检测
- **网络搜索**：实时搜索文档、API 参考、代码示例
- **图片生成**：调用 DALL-E / GPT-Image 生成 UI 素材与示意图
- **代码沙箱**：隔离环境执行 Python/Shell 代码片段
- **记忆系统**：自动提取对话中的关键信息，跨会话持久化

### 🔌 工具生态系统
- **MCP 协议支持**：接入 Model Context Protocol 生态（文件系统、GitHub、Playwright 等 200+ 工具）
- **工具注册表**：声明式工具注册，支持条件门控 (`check_fn`)
- **并发调度**：只读工具并行执行，写工具串行，智能分批优化

### 🛡️ 权限与安全
- **三级权限**：`ALLOW / ASK / DENY` 细粒度控制
- **用户审批**：敏感操作弹出确认对话框，支持"记住选择"
- **路径隔离**：沙箱目录保护，防止路径穿越
- **危险命令检测**：正则匹配阻止 fork bomb、rm -rf 等恶意命令

### 🧠 智能记忆
- **多级记忆**：分类（fact/preference/identity/knowledge/task）+ 重要性评分（1-10★）
- **自动提取**：对话每 5 轮自动调用 LLM 提取关键信息
- **记忆整合**：合并相似记忆，降解低频记忆，清理低价值条目
- **热度追踪**：基于访问频率和时间的 LRU 淘汰策略
- **Agent 可搜索**：Agent 可通过 `memory_search` 工具主动查询记忆

### 🎨 月下设计
- **深蓝+琥珀** 品牌色系，月夜氛围
- **Jetpack Compose + Material 3** 现代化 UI
- **深色/浅色** 自动跟随系统
- **响应式布局**，适配手机和平板

## 🏗️ 技术架构

```
┌──────────────────────────────────────────────┐
│              用户界面 (Compose)                │
│  项目 / 编码代理 / 终端 / 设置 / 角色 / 记忆  │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│             Agent 引擎 (AgentLoop)            │
│  Think → Act → Observe 循环                   │
│  工具调用 · 权限审批 · 状态追踪               │
└──────┬─────────────────────┬─────────────────┘
       │                     │
┌──────▼──────┐     ┌───────▼─────────────────┐
│  ToolRegistry │     │   LLM Provider Layer     │
│  · 注册/发现  │     │   · OpenAI Compatible     │
│  · 权限过滤   │     │   · tool_choice="auto"    │
│  · 并发调度   │     │   · parallel_tool_calls   │
└──────┬──────┘     └───────────┬───────────────┘
       │                        │
┌──────▼────────────────────────▼───────────────┐
│              工具执行层                        │
│  ReadFile · WriteFile · Bash · WebSearch      │
│  CodeSandbox · ImageGen · MemorySearch        │
│  MCP Client (stdio/HTTP/SSE) · TerminalLog    │
└───────────────────────────────────────────────┘
```

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin |
| **UI 框架** | Jetpack Compose + Material 3 |
| **架构模式** | MVVM + Repository + Agent |
| **依赖注入** | Hilt (Dagger) |
| **网络请求** | Retrofit + OkHttp |
| **本地数据库** | Room (SQLite) + DataStore |
| **异步处理** | Kotlin Coroutines + Flow + StateFlow |
| **图片加载** | Coil |
| **序列化** | Gson |
| **构建工具** | Gradle KTS |
| **CI/CD** | GitHub Actions |
| **最低 SDK** | API 26 (Android 8.0) |
| **目标 SDK** | API 34 (Android 14) |

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1)+
- JDK 17
- Android SDK 34

### 构建步骤

```bash
# 克隆
git clone <your-fork-url>
cd CodeVibe

# 调试版 APK
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# 发布版 APK
./gradlew assembleRelease
```

### 配置 API
1. 启动 App → 侧边栏 → 设置
2. 添加 API 端点（支持任何 OpenAI 兼容 API）
3. 在"编码代理"页面输入任务开始编程

## 📦 模块概览

```
com.aichat.app/
├── agent/                 # Agent 引擎核心
│   ├── AgentLoop.kt       # 主循环 (Think→Act→Observe)
│   ├── ToolRegistry.kt    # 工具注册表
│   ├── ToolDefinition.kt  # 工具定义 DSL
│   ├── TaskPlanner.kt     # 任务规划器
│   └── tools/             # 内置工具
│       ├── ReadFileTool.kt
│       ├── WriteFileTool.kt
│       ├── WebSearchTool.kt
│       ├── BashTool.kt
│       ├── CodeSandboxTool.kt
│       ├── ImageTool.kt
│       ├── MemorySearchTool.kt
│       └── TerminalLogTool.kt
├── mcp/                   # MCP 协议客户端
│   └── McpClient.kt
├── memory/                # 记忆系统
│   ├── AutoMemoryExtractor.kt
│   └── MemoryDao.kt
├── permission/            # 权限引擎
│   └── PermissionEngine.kt
├── data/                  # 数据层
│   ├── local/ (Room + DataStore)
│   ├── remote/ (Retrofit API)
│   ├── model/ (Entities)
│   └── repository/
└── ui/                    # UI 层
    ├── agent/             # 编码代理工作台
    ├── chat/              # 编码工作区
    ├── conversations/     # 项目列表
    ├── settings/          # 开发者设置
    ├── components/        # 通用组件
    └── theme/             # 月下主题
```

## 🔄 5 轮审计迭代

项目经历了 5 轮全量代码审计，共发现并修复了 65+ 个问题：

| 轮次 | 发现问题 | 重点修复 |
|------|---------|---------|
| R1 | 25（含2致命编译阻断） | McpClient.asMap()、AgentLoop tool_call_id |
| R2 | 12（2中危+8低危） | PermissionEngine ASK 拦截、ChatViewModel model 参数 |
| R3 | 9（2致命+2中危） | 图片生成 model、Dagger 重复绑定 |
| R4 | 15（3P0+5P1） | 权限通配覆盖、进程泄漏、Retrofit 泄漏 |
| R5 | 终审 B+ | 3个P1安全项修复（明文密码、HTTP、R8） |

最终评分：**B+** — 代码温度 100%（零 TODO/FIXME/HACK）

## 📄 License

MIT License — 详见 [LICENSE](LICENSE)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
