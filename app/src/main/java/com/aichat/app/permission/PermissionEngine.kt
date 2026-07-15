package com.aichat.app.permission

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Three-level permission system for tool access control.
 *
 * Inspired by OpenCode's PermissionV2 system:
 * - allow: auto-approve, no user interaction needed
 * - ask: prompt user for approval each time
 * - deny: reject automatically
 *
 * Rules support glob pattern matching on resources.
 * "remember this choice" is stored via PermissionStorage.
 */
sealed class PermissionResult {
    data object Allowed : PermissionResult()
    data class Denied(val reason: String = "权限不足") : PermissionResult()
    data class NeedsApproval(
        val action: String,
        val resource: String,
        val context: String = ""
    ) : PermissionResult()
}

data class PermissionRule(
    val action: String,           // tool name or "*" (wildcard)
    val resource: String = "*",   // glob pattern
    val effect: PermissionEffect
)

enum class PermissionEffect {
    ALLOW,
    ASK,
    DENY
}

class PermissionEngine @Inject constructor() {

    companion object {
        private const val TAG = "PermissionEngine"
    }

    private val rules = mutableListOf<PermissionRule>()
    private val savedChoices = ConcurrentHashMap<String, PermissionEffect>()

    /**
     * Register permission rules. Rules added later have higher priority.
     */
    fun configure(rules: List<PermissionRule>) {
        synchronized(this) {
            this.rules.clear()
            this.rules.addAll(rules)
        }
        Log.d(TAG, "Configured with ${rules.size} rules")
    }

    /**
     * Evaluate a tool action against the permission rules.
     */
    fun evaluate(action: String, resource: String, context: String = ""): PermissionResult {
        // 1. Check saved choices first
        val savedKey = "$action:$resource"
        val saved = savedChoices[savedKey]
        if (saved == PermissionEffect.ALLOW) return PermissionResult.Allowed
        if (saved == PermissionEffect.DENY) return PermissionResult.Denied("User previously denied")

        // 2. Match rules (first matching rule wins)
        for (rule in rules) {
            if (!matchAction(rule.action, action)) continue
            if (!matchResource(rule.resource, resource)) continue

            return when (rule.effect) {
                PermissionEffect.ALLOW -> PermissionResult.Allowed
                PermissionEffect.DENY -> PermissionResult.Denied("规则禁止: $action $resource")
                PermissionEffect.ASK -> PermissionResult.NeedsApproval(action, resource, context)
            }
        }

        // 3. Default: ASK
        return PermissionResult.NeedsApproval(action, resource, context)
    }

    /**
     * Remember a user's choice for future executions.
     */
    fun remember(action: String, resource: String, effect: PermissionEffect) {
        val memKey = "$action:$resource"
        savedChoices[memKey] = effect
        Log.d(TAG, "Saved permission choice: $memKey = $effect")
    }

    /**
     * Simple glob-style action matching.
     * "*" matches everything, otherwise exact match.
     */
    private fun matchAction(pattern: String, action: String): Boolean {
        return pattern == "*" || pattern == action
    }

    /**
     * Simple glob-style resource matching.
     * "dir/*" matches "dir/anything" but NOT "dir_backup/anything".
     */
    private fun matchResource(pattern: String, resource: String): Boolean {
        if (pattern == "*" || pattern == resource) return true
        val prefix = pattern.removeSuffix("/*")
        if (prefix == pattern) return false // pattern didn't end with /*
        return resource.startsWith(prefix) && resource.length > prefix.length && resource[prefix.length] == '/'
    }
}
