package com.aichat.app.agent

import android.util.Log
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    private val apiManager: ApiManager
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "TaskPlanner"
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val MAX_STEPS = 10
    }

    data class PlanStep(
        val description: String,
        val tool: String,
        val expectedOutcome: String,
        val dependsOn: List<Int> = emptyList()
    )

    data class Plan(
        val goal: String,
        val steps: List<PlanStep>,
        val estimatedComplexity: String
    )

    suspend fun plan(
        userRequest: String,
        availableTools: List<ToolDefinition>
    ): Plan = withContext(Dispatchers.IO) {
        try {
            val toolsDesc = availableTools.joinToString("\n") { tool ->
                val params = tool.parameters.joinToString(", ") { p ->
                    "${p.name}: ${p.type.name}${if (!p.required) "(可选)" else ""}"
                }
                "- ${tool.name}: ${tool.description}（参数: $params）"
            }

            val planPrompt = """
            你是一个任务规划助手。请将用户请求分解为可执行的步骤列表。

            可用工具：
            $toolsDesc

            用户请求：
            $userRequest

            请分析任务并输出 JSON（不要其他内容）：
            {
              "goal": "任务目标概述",
              "complexity": "简单/中等/复杂",
              "steps": [
                {
                  "index": 1,
                  "description": "步骤描述",
                  "tool": "建议使用的工具名称（从可用工具中选择）",
                  "expected_outcome": "预期输出",
                  "depends_on": []
                }
              ]
            }

            规则：
            - 最多 $MAX_STEPS 步
            - 每步只做一件事
            - depends_on 是依赖的步骤索引（从 1 开始），如 [1] 表示依赖步骤 1
            - 步骤必须可顺序执行，不能有循环依赖
            - 选择最合适的工具，可以同一工具多次使用
            """.trimIndent()

            val request = ChatRequest(
                model = DEFAULT_MODEL,
                messages = listOf(
                    ChatMessage("system", "你是一个结构化的任务规划助手。严格按照 JSON 格式输出。"),
                    ChatMessage("user", planPrompt)
                ),
                temperature = 0.3f,
                max_tokens = 2048,
                stream = false
            )

            val response = apiManager.getApiService().chatCompletion(
                auth = apiManager.getAuthHeader(),
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Plan(
                    goal = userRequest,
                    steps = emptyList(),
                    estimatedComplexity = "未知"
                )

            val plan = parsePlan(content, userRequest)
            if (hasCycle(plan.steps)) {
                Log.w(TAG, "检测到循环依赖，返回扁平步骤")
                val flatSteps = plan.steps.map { it.copy(dependsOn = emptyList()) }
                return@withContext plan.copy(steps = flatSteps)
            }

            plan
        } catch (e: Exception) {
            Log.e(TAG, "规划任务失败", e)
            Plan(
                goal = userRequest,
                steps = emptyList(),
                estimatedComplexity = "未知"
            )
        }
    }

    private fun parsePlan(json: String, fallbackGoal: String): Plan {
        val jsonStr = extractJson(json)
        return try {
            val map = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any?>
                ?: return Plan(fallbackGoal, emptyList(), "未知")

            val goal = map["goal"]?.toString() ?: fallbackGoal
            val complexity = map["complexity"]?.toString() ?: "未知"

            val rawSteps = (map["steps"] as? List<*>) ?: emptyList<Any>()
            val steps = rawSteps.mapNotNull { step ->
                val s = step as? Map<*, *> ?: return@mapNotNull null
                PlanStep(
                    description = s["description"]?.toString() ?: return@mapNotNull null,
                    tool = s["tool"]?.toString() ?: "",
                    expectedOutcome = s["expected_outcome"]?.toString() ?: "",
                    dependsOn = (s["depends_on"] as? List<*>)?.mapNotNull {
                        (it as? Number)?.toInt()
                    } ?: emptyList()
                )
            }

            Plan(goal, steps, complexity)
        } catch (e: Exception) {
            Log.e(TAG, "解析规划结果失败", e)
            Plan(fallbackGoal, emptyList(), "未知")
        }
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1)
        }
        return text
    }

    private fun hasCycle(steps: List<PlanStep>): Boolean {
        val visited = BooleanArray(steps.size)
        val inStack = BooleanArray(steps.size)

        fun dfs(idx: Int): Boolean {
            if (inStack[idx]) return true
            if (visited[idx]) return false
            visited[idx] = true
            inStack[idx] = true
            for (dep in steps[idx].dependsOn) {
                val depIdx = dep - 1
                if (depIdx in steps.indices) {
                    if (dfs(depIdx)) return true
                }
            }
            inStack[idx] = false
            return false
        }

        for (i in steps.indices) {
            if (dfs(i)) return true
        }
        return false
    }

    fun formatPlan(plan: Plan): String {
        val sb = StringBuilder()
        sb.appendLine("## 任务规划")
        sb.appendLine("**目标**: ${plan.goal}")
        sb.appendLine("**复杂度**: ${plan.estimatedComplexity}")
        sb.appendLine()
        sb.appendLine("### 执行步骤")

        if (plan.steps.isEmpty()) {
            sb.appendLine("无需分解，可直接执行。")
            return sb.toString()
        }

        for ((i, step) in plan.steps.withIndex()) {
            sb.appendLine("**步骤 ${i + 1}**: ${step.description}")
            sb.appendLine("- 工具: `${step.tool}`")
            sb.appendLine("- 预期: ${step.expectedOutcome}")
            if (step.dependsOn.isNotEmpty()) {
                sb.appendLine("- 依赖: 步骤 ${step.dependsOn.joinToString(", ")}")
            }
            sb.appendLine()
        }

        return sb.toString()
    }
}
