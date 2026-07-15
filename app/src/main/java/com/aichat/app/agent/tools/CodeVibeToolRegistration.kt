package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized registration of all 25+ adapted tools from Operit into CodeVibe's ToolRegistry.
 *
 * Tools are grouped by category:
 * - File System: 14 tools
 * - Terminal: 4 tools
 * - Web: 2 tools
 * - Memory: 2 tools
 * - Utility: 3 tools
 * - Code Search: 1 tool
 * Total: 26 tools
 */
@Singleton
class CodeVibeToolRegistration @Inject constructor(
    private val registry: ToolRegistry,
    private val listFilesTool: ListFilesTool,
    private val readFileTool: FileReadTool,
    private val writeFileTool: FileWriteTool,
    private val editFileTool: EditFileTool,
    private val deleteFileTool: DeleteFileTool,
    private val copyFileTool: CopyFileTool,
    private val makeDirectoryTool: MakeDirectoryTool,
    private val findFilesTool: FindFilesTool,
    private val fileInfoTool: FileInfoTool,
    private val fileExistsTool: FileExistsTool,
    private val moveFileTool: MoveFileTool,
    private val grepCodeTool: GrepCodeTool,
    private val zipFilesTool: ZipFilesTool,
    private val unzipFilesTool: UnzipFilesTool,
    private val executeShellTool: ExecuteShellTool,
    private val createTerminalSessionTool: CreateTerminalSessionTool,
    private val executeInTerminalSessionTool: ExecuteInTerminalSessionTool,
    private val closeTerminalSessionTool: CloseTerminalSessionTool,
    private val visitWebTool: VisitWebTool,
    private val httpRequestTool: HttpRequestTool,
    private val queryMemoryTool: QueryMemoryTool,
    private val createMemoryTool: CreateMemoryTool,
    private val sleepTool: SleepTool,
    private val calculateTool: CalculateTool,
    private val deviceInfoTool: DeviceInfoTool
) {
    fun registerAll() {
        registry.registerAll(
            // ===== File System (14 tools) =====
            listFilesTool,           // list_files - 列出目录内容
            readFileTool,            // read_file - 读取文件内容
            writeFileTool,           // write_file - 写入/覆盖文件
            editFileTool,            // edit_file - 精确文本替换编辑
            deleteFileTool,          // delete_file - 删除文件/目录
            copyFileTool,            // copy_file - 复制文件/目录
            makeDirectoryTool,       // make_directory - 创建目录
            findFilesTool,           // find_files - 按模式搜索文件
            fileInfoTool,            // file_info - 获取文件详细信息
            fileExistsTool,          // file_exists - 检查文件是否存在
            moveFileTool,            // move_file - 移动/重命名
            grepCodeTool,            // grep_code - 代码内容搜索
            zipFilesTool,            // zip_files - 压缩为ZIP
            unzipFilesTool,          // unzip_files - 解压ZIP

            // ===== Terminal (4 tools) =====
            executeShellTool,               // execute_shell - 执行shell命令
            createTerminalSessionTool,      // create_terminal_session - 创建终端会话
            executeInTerminalSessionTool,   // execute_in_terminal_session - 在会话中执行命令
            closeTerminalSessionTool,       // close_terminal_session - 关闭会话

            // ===== Web (2 tools) =====
            visitWebTool,            // visit_web - 访问网页
            httpRequestTool,         // http_request - HTTP请求

            // ===== Memory (2 tools) =====
            queryMemoryTool,         // query_memory - 查询记忆
            createMemoryTool,        // create_memory - 创建记忆

            // ===== Utility (3 tools) =====
            sleepTool,               // sleep - 延迟等待
            calculateTool,           // calculate - 数学计算
            deviceInfoTool           // device_info - 设备信息
        )
    }
}
