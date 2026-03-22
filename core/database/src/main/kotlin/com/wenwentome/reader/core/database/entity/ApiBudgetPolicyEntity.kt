package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.ApiOverBudgetAction
import com.wenwentome.reader.core.model.DEFAULT_API_BUDGET_POLICY_ID

@Entity(tableName = "api_budget_policies")
data class ApiBudgetPolicyEntity(
    @PrimaryKey val policyId: String = DEFAULT_API_BUDGET_POLICY_ID,
    val dailyLimitMicros: Long? = null,
    val monthlyLimitMicros: Long? = null,
    val warnThreshold: Float = 0.8f,
    val requireConfirmAboveCostMicros: Long? = null,
    val overBudgetAction: ApiOverBudgetAction = ApiOverBudgetAction.ASK,
    val fallbackToLowerCostModel: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
