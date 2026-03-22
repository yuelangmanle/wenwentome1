package com.wenwentome.reader.core.model

enum class ApiOverBudgetAction { DOWNGRADE, PAUSE, ASK }

data class ApiBudgetPolicy(
    val policyId: String = DEFAULT_API_BUDGET_POLICY_ID,
    val dailyLimitMicros: Long? = null,
    val monthlyLimitMicros: Long? = null,
    val warnThreshold: Float = 0.8f,
    val requireConfirmAboveCostMicros: Long? = null,
    val overBudgetAction: ApiOverBudgetAction = ApiOverBudgetAction.ASK,
    val fallbackToLowerCostModel: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)

const val DEFAULT_API_BUDGET_POLICY_ID = "default"
