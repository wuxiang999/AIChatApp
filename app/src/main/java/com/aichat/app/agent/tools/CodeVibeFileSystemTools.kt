package com.aichat.app.agent.tools

import android.util.Log
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

private const val TAG = "CodeVibeFileTools"
private val DEFAULT_ENCODINGS = listOf("UTF-8", "GBK", "ISO-8859-1", "Shift_JIS", "EUC-KR")

@Singleton
class ListFilesTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "list_files",
        description = "列出目录中的文件和子目录",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "目录路径", required = true),
            ParameterSchema("max_depth", ParameterType.INTEGER, "最大递归深度（默认1，-1为无限）", required = false)
        ),
        action = "list_files",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val maxDepth = (args["max_depth"] as? Number)?.toInt() ?: 1
        val dir = File(path)
        if (!dir.exists()) return ToolResult.Error("Directory does not exist: $path")
        if (!dir.isDirectory) return ToolResult.Error("Path is not a directory: $path")

        return try {
            val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)
            val entries = mutableListOf<String>()
            listDir(dir, "", maxDepth, entries, dateFormat)
            ToolResult.Success("Directory listing for $path:\n${entries.joinToString("\n")}")
        } catch (e: Exception) {
            Log.e(TAG, "Error listing directory", e)
            ToolResult.Error("Error listing directory: ${e.message}")
        }
    }

    private fun listDir(dir: File, prefix: String, maxDepth: Int, result: MutableList<String>, dateFormat: SimpleDateFormat) {
        if (maxDepth == 0) return
        val files = dir.listFiles()?.sortedBy { it.name } ?: return
        for (file in files) {
            val type = if (file.isDirectory) "d" else "-"
            val perms = buildString {
                append(if (file.canRead()) 'r' else '-')
                append(if (file.canWrite()) 'w' else '-')
                append(if (file.canExecute()) 'x' else '-')
            }
            val size = file.length().toString().padStart(8)
            val modified = dateFormat.format(Date(file.lastModified()))
            result.add("$type$perms $size $modified $prefix${file.name}")
            if (file.isDirectory && (maxDepth > 1 || maxDepth == -1)) {
                listDir(file, "$prefix  ", if (maxDepth > 0) maxDepth - 1 else -1, result, dateFormat)
            }
        }
    }
}

@Singleton
class FileReadTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "read_file",
        description = "读取文件内容。自动检测编码（UTF-8/GBK等）",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件路径", required = true),
            ParameterSchema("offset", ParameterType.INTEGER, "起始行号（从1开始）", required = false),
            ParameterSchema("limit", ParameterType.INTEGER, "最大读取行数", required = false)
        ),
        action = "read_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val offset = (args["offset"] as? Number)?.toInt() ?: 1
        val limit = (args["limit"] as? Number)?.toInt() ?: Int.MAX_VALUE
        val file = File(path)

        if (!file.exists()) return ToolResult.Error("File does not exist: $path")
        if (!file.isFile) return ToolResult.Error("Path is not a file: $path")
        if (file.length() > 10 * 1024 * 1024) return ToolResult.Error("File too large (>10MB)")

        return try {
            val content = file.readText(DEFAULT_ENCODINGS)
            val lines = content.lines()
            val totalLines = lines.size
            val startIndex = (offset - 1).coerceIn(0, totalLines - 1)
            val endIndex = (startIndex + limit).coerceIn(0, totalLines)
            val selected = lines.subList(startIndex, endIndex).joinToString("\n")
            val summary = if (totalLines > limit) "\n\n[Showing lines ${startIndex + 1}-$endIndex of $totalLines]"
            else "\n\n[Total $totalLines lines]"
            ToolResult.Success(selected + summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file", e)
            ToolResult.Error("Failed to read file: ${e.message}")
        }
    }
}

@Singleton
class FileWriteTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "write_file",
        description = "写入文件内容。如果文件不存在则创建，支持覆盖或追加",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件路径", required = true),
            ParameterSchema("content", ParameterType.STRING, "文件内容", required = true),
            ParameterSchema("append", ParameterType.BOOLEAN, "是否追加（默认false=覆盖）", required = false)
        ),
        action = "write_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val content = args["content"]?.toString() ?: return ToolResult.Error("Missing content parameter")
        val append = (args["append"] as? Boolean) ?: false
        val file = File(path)

        if (content.length > 5 * 1024 * 1024) return ToolResult.Error("Content too large (>5MB)")

        return try {
            file.parentFile?.mkdirs()
            if (append) file.appendText(content) else file.writeText(content)
            ToolResult.Success("Written ${file.absolutePath} (${content.length} chars)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file", e)
            ToolResult.Error("Failed to write file: ${e.message}")
        }
    }
}

@Singleton
class EditFileTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "edit_file",
        description = "对文件进行精确的文本替换编辑。支持基于行的替换操作",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件路径", required = true),
            ParameterSchema("old_string", ParameterType.STRING, "被替换的原文（必须唯一匹配）", required = true),
            ParameterSchema("new_string", ParameterType.STRING, "替换后的新文本", required = true)
        ),
        action = "edit_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val oldString = args["old_string"]?.toString() ?: return ToolResult.Error("Missing old_string parameter")
        val newString = args["new_string"]?.toString() ?: ""
        val file = File(path)

        if (!file.exists()) return ToolResult.Error("File does not exist: $path")

        return try {
            val content = file.readText(DEFAULT_ENCODINGS)
            val count = content.split(oldString).size - 1
            if (count == 0) return ToolResult.Error("old_string not found in file")
            if (count > 1) return ToolResult.Error("Found $count matches for old_string. Please provide more surrounding context for a unique match.")

            val newContent = content.replace(oldString, newString)
            file.writeText(newContent)
            ToolResult.Success("File edited successfully at $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit file", e)
            ToolResult.Error("Failed to edit file: ${e.message}")
        }
    }
}

@Singleton
class DeleteFileTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "delete_file",
        description = "删除文件或空目录。recursive=true时可递归删除非空目录",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件或目录路径", required = true),
            ParameterSchema("recursive", ParameterType.BOOLEAN, "是否递归删除（用于目录）", required = false)
        ),
        action = "delete_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val recursive = (args["recursive"] as? Boolean) ?: false
        val file = File(path)
        if (!file.exists()) return ToolResult.Error("Path does not exist: $path")

        return try {
            if (file.isDirectory && !recursive) {
                if (file.list()?.isNotEmpty() == true)
                    return ToolResult.Error("Directory is not empty. Use recursive=true to delete.")
            }
            file.deleteRecursively()
            ToolResult.Success("Deleted: $path")
        } catch (e: Exception) {
            ToolResult.Error("Failed to delete: ${e.message}")
        }
    }
}

@Singleton
class CopyFileTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "copy_file",
        description = "复制文件或目录到目标位置",
        parameters = listOf(
            ParameterSchema("source", ParameterType.STRING, "源路径", required = true),
            ParameterSchema("destination", ParameterType.STRING, "目标路径", required = true)
        ),
        action = "copy_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val source = args["source"]?.toString() ?: return ToolResult.Error("Missing source parameter")
        val dest = args["destination"]?.toString() ?: return ToolResult.Error("Missing destination parameter")
        val srcFile = File(source)
        val destFile = File(dest)
        if (!srcFile.exists()) return ToolResult.Error("Source does not exist: $source")

        return try {
            srcFile.copyRecursively(destFile, overwrite = true)
            ToolResult.Success("Copied: $source -> $dest")
        } catch (e: Exception) {
            ToolResult.Error("Failed to copy: ${e.message}")
        }
    }
}

@Singleton
class MakeDirectoryTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "make_directory",
        description = "创建目录。自动创建父目录",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "目录路径", required = true)
        ),
        action = "make_directory",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        return try {
            File(path).mkdirs()
            ToolResult.Success("Directory created: $path")
        } catch (e: Exception) {
            ToolResult.Error("Failed to create directory: ${e.message}")
        }
    }
}

@Singleton
class FindFilesTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "find_files",
        description = "查找文件。支持glob通配符模式（如 *.kt, **/*.java）",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "搜索根目录", required = true),
            ParameterSchema("pattern", ParameterType.STRING, "文件名匹配模式", required = true),
            ParameterSchema("max_depth", ParameterType.INTEGER, "最大递归深度", required = false)
        ),
        action = "find_files",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val basePath = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val pattern = args["pattern"]?.toString() ?: return ToolResult.Error("Missing pattern parameter")
        val maxDepth = (args["max_depth"] as? Number)?.toInt() ?: -1
        val baseDir = File(basePath)
        if (!baseDir.exists()) return ToolResult.Error("Base path does not exist: $basePath")

        return try {
            val regex = globToRegex(pattern)
            val results = mutableListOf<String>()
            searchFiles(baseDir, regex, maxDepth, 0, results)
            if (results.isEmpty()) ToolResult.Success("No files found matching pattern: $pattern")
            else ToolResult.Success("Found ${results.size} files:\n${results.joinToString("\n")}")
        } catch (e: Exception) {
            ToolResult.Error("Failed to search files: ${e.message}")
        }
    }

    private fun searchFiles(dir: File, regex: Regex, maxDepth: Int, depth: Int, results: MutableList<String>) {
        if (maxDepth >= 0 && depth > maxDepth) return
        val files = dir.listFiles() ?: return
        for (file in files.sortedBy { it.name }) {
            if (regex.containsMatchIn(file.name)) results.add(file.absolutePath)
            if (file.isDirectory) searchFiles(file, regex, maxDepth, depth + 1, results)
        }
    }
}

@Singleton
class FileInfoTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "file_info",
        description = "获取文件或目录的详细信息（大小、权限、修改时间等）",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件或目录路径", required = true)
        ),
        action = "file_info",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val file = File(path)
        if (!file.exists()) return ToolResult.Error("Path does not exist: $path")

        val type = if (file.isDirectory) "directory" else if (file.isFile) "file" else "other"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val info = buildString {
            appendLine("Path: ${file.absolutePath}")
            appendLine("Type: $type")
            appendLine("Size: ${file.length()} bytes")
            appendLine("Permissions: ${if (file.canRead()) 'r' else '-'}${if (file.canWrite()) 'w' else '-'}${if (file.canExecute()) 'x' else '-'}")
            appendLine("Last Modified: ${dateFormat.format(Date(file.lastModified()))}")
            if (file.isDirectory) {
                appendLine("Contents: ${file.list()?.size ?: 0} items")
            }
        }
        return ToolResult.Success(info.trim())
    }
}

@Singleton
class FileExistsTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "file_exists",
        description = "检查文件或目录是否存在",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件或目录路径", required = true)
        ),
        action = "file_exists",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val file = File(path)
        return ToolResult.Success("{\"exists\": ${file.exists()}, \"is_directory\": ${file.isDirectory}, \"path\": \"$path\"}")
    }
}

@Singleton
class MoveFileTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "move_file",
        description = "移动或重命名文件/目录",
        parameters = listOf(
            ParameterSchema("source", ParameterType.STRING, "源路径", required = true),
            ParameterSchema("destination", ParameterType.STRING, "目标路径", required = true)
        ),
        action = "move_file",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val source = args["source"]?.toString() ?: return ToolResult.Error("Missing source parameter")
        val dest = args["destination"]?.toString() ?: return ToolResult.Error("Missing destination parameter")
        val srcFile = File(source)
        if (!srcFile.exists()) return ToolResult.Error("Source does not exist: $source")

        return try {
            val destFile = File(dest)
            destFile.parentFile?.mkdirs()
            srcFile.renameTo(destFile)
            ToolResult.Success("Moved: $source -> $dest")
        } catch (e: Exception) {
            ToolResult.Error("Failed to move: ${e.message}")
        }
    }
}

@Singleton
class GrepCodeTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "grep_code",
        description = "在文件中搜索文本模式。支持正则表达式，可限定文件类型",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "搜索根目录或文件", required = true),
            ParameterSchema("pattern", ParameterType.STRING, "搜索模式（正则表达式）", required = true),
            ParameterSchema("file_pattern", ParameterType.STRING, "文件通配符过滤（如 *.kt, *.java）", required = false),
            ParameterSchema("max_results", ParameterType.INTEGER, "最大结果数", required = false),
            ParameterSchema("case_insensitive", ParameterType.BOOLEAN, "是否忽略大小写", required = false),
            ParameterSchema("context_lines", ParameterType.INTEGER, "上下文行数", required = false)
        ),
        action = "grep_code",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("Missing path parameter")
        val pattern = args["pattern"]?.toString() ?: return ToolResult.Error("Missing pattern parameter")
        val filePattern = args["file_pattern"]?.toString()
        val maxResults = (args["max_results"] as? Number)?.toInt() ?: 100
        val caseInsensitive = (args["case_insensitive"] as? Boolean) ?: true
        val contextLines = (args["context_lines"] as? Number)?.toInt() ?: 2

        val baseFile = File(path)
        if (!baseFile.exists()) return ToolResult.Error("Path does not exist: $path")

        return try {
            val regex = if (caseInsensitive) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
            val fileRegex = filePattern?.let { globToRegex(it) }

            val results = mutableListOf<String>()
            var count = 0
            val files = if (baseFile.isFile) listOf(baseFile) else baseFile.walkTopDown().filter { it.isFile }.toList()

            for (file in files) {
                if (count >= maxResults) break
                if (fileRegex != null && !fileRegex.containsMatchIn(file.name)) continue

                try {
                    val lines = file.readText(DEFAULT_ENCODINGS).lines()
                    for ((i, line) in lines.withIndex()) {
                        if (count >= maxResults) break
                        if (regex.containsMatchIn(line)) {
                            val startLine = maxOf(0, i - contextLines)
                            val endLine = minOf(lines.size - 1, i + contextLines)
                            results.add("${file.absolutePath}:${i + 1}: $line")
                            if (contextLines > 0) {
                                val ctx = lines.subList(startLine, endLine + 1)
                                    .mapIndexed { idx, l -> "${startLine + idx + 1}| $l" }
                                    .joinToString("\n")
                                results.add(ctx)
                            }
                            results.add("---")
                            count++
                        }
                    }
                } catch (_: Exception) { }
            }

            if (results.isEmpty()) ToolResult.Success("No matches found for: $pattern")
            else ToolResult.Success("Found $count matches:\n${results.joinToString("\n")}")
        } catch (e: Exception) {
            ToolResult.Error("Grep search failed: ${e.message}")
        }
    }
}

@Singleton
class ZipFilesTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "zip_files",
        description = "压缩文件或目录为ZIP文件",
        parameters = listOf(
            ParameterSchema("source", ParameterType.STRING, "要压缩的文件或目录路径", required = true),
            ParameterSchema("destination", ParameterType.STRING, "输出的ZIP文件路径", required = true)
        ),
        action = "zip_files",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val source = args["source"]?.toString() ?: return ToolResult.Error("Missing source parameter")
        val dest = args["destination"]?.toString() ?: return ToolResult.Error("Missing destination parameter")
        val srcFile = File(source)
        if (!srcFile.exists()) return ToolResult.Error("Source does not exist: $source")

        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(dest))).use { zos ->
                if (srcFile.isFile) {
                    zos.putNextEntry(ZipEntry(srcFile.name))
                    BufferedInputStream(FileInputStream(srcFile)).use { it.copyTo(zos) }
                    zos.closeEntry()
                } else {
                    srcFile.walkTopDown().forEach { file ->
                        val entryName = file.relativeTo(srcFile.parentFile).path
                        if (file.isDirectory) zos.putNextEntry(ZipEntry("$entryName/"))
                        else {
                            zos.putNextEntry(ZipEntry(entryName))
                            BufferedInputStream(FileInputStream(file)).use { it.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }
            ToolResult.Success("Zipped: $source -> $dest")
        } catch (e: Exception) {
            ToolResult.Error("Failed to zip: ${e.message}")
        }
    }
}

@Singleton
class UnzipFilesTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "unzip_files",
        description = "解压ZIP文件到指定目录",
        parameters = listOf(
            ParameterSchema("source", ParameterType.STRING, "ZIP文件路径", required = true),
            ParameterSchema("destination", ParameterType.STRING, "解压目标目录", required = true)
        ),
        action = "unzip_files",
        toolset = "filesystem"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val source = args["source"]?.toString() ?: return ToolResult.Error("Missing source parameter")
        val dest = args["destination"]?.toString() ?: return ToolResult.Error("Missing destination parameter")
        val zipFile = File(source)
        if (!zipFile.exists()) return ToolResult.Error("ZIP file does not exist: $source")

        return try {
            val destDir = File(dest).also { it.mkdirs() }
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = File(destDir, entry.name)
                    if (entry.isDirectory) target.mkdirs()
                    else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            ToolResult.Success("Unzipped: $source -> $dest")
        } catch (e: Exception) {
            ToolResult.Error("Failed to unzip: ${e.message}")
        }
    }
}

internal fun globToRegex(glob: String): Regex {
    val regexStr = StringBuilder("^")
    var i = 0
    while (i < glob.length) {
        val c = glob[i]
        when (c) {
            '*' -> {
                if (i + 1 < glob.length && glob[i + 1] == '*') {
                    regexStr.append(".*")
                    i++
                    if (i + 1 < glob.length && glob[i + 1] == '/') i++
                } else regexStr.append("[^/]*")
            }
            '?' -> regexStr.append("[^/]")
            '.', '+', '^', '$', '(', ')', '|', '{', '}', '[', ']', '\\' -> {
                regexStr.append('\\'); regexStr.append(c)
            }
            else -> regexStr.append(c)
        }
        i++
    }
    regexStr.append('$')
    return Regex(regexStr.toString(), RegexOption.IGNORE_CASE)
}

private fun File.readText(encodings: List<String>): String {
    val bytes = readBytes()
    for (enc in encodings) {
        try { return bytes.toString(Charsets.forName(enc)) } catch (_: Exception) { }
    }
    return bytes.toString(Charsets.UTF_8)
}
