package com.aichat.app.permission

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

sealed class PermissionResult {
    data object Allowed : PermissionResult()
    data class Denied(val reason: String = "permission denied") : PermissionResult()
    data class NeedsApproval(val action: String, val resource: String, val context: String = "") : PermissionResult()
}

data class PermissionRule(val action: String, val resource: String = "*", val effect: PermissionEffect)

enum class PermissionEffect { ALLOW, ASK, DENY }

class PermissionEngine @Inject constructor() {

    companion object { private const val TAG = "PermissionEngine" }

    private val rules = mutableListOf<PermissionRule>()
    private val savedChoices = ConcurrentHashMap<String, PermissionEffect>()

    fun configure(newRules: List<PermissionRule>) {
        rules.clear()
        rules.addAll(newRules)
        Log.d(TAG, "Configured with ${rules.size} rules")
    }

    fun evaluate(action: String, resource: String, context: String = ""): PermissionResult {
        val key = "$action:$resource"
        val saved = savedChoices[key]
        if (saved == PermissionEffect.ALLOW) return PermissionResult.Allowed
        if (saved == PermissionEffect.DENY) return PermissionResult.Denied("User previously denied")
        for (rule in rules) {
            if (rule.action != "*" && rule.action != action) continue
            if (rule.resource != "*" && rule.resource != resource) continue
            return when (rule.effect) {
                PermissionEffect.ALLOW -> PermissionResult.Allowed
                PermissionEffect.DENY -> PermissionResult.Denied("denied: $action")
                PermissionEffect.ASK -> PermissionResult.NeedsApproval(action, resource, context)
            }
        }
        return PermissionResult.NeedsApproval(action, resource, context)
    }

    fun remember(action: String, resource: String, effect: PermissionEffect) {
        savedChoices["$action:$resource"] = effect
    }
}
