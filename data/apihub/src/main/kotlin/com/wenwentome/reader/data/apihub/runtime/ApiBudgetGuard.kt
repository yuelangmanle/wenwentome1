package com.wenwentome.reader.data.apihub.runtime

import com.wenwentome.reader.data.apihub.ApiHubRepository
import java.time.Instant
import java.time.ZoneId

sealed interface BudgetDecision {
    data class Allowed(
        val projectedDailyUsageMicros: Long,
        val projectedMonthlyUsageMicros: Long,
        val projectedUsageRatio: Float,
    ) : BudgetDecision

    data class Blocked(
        val reason: String,
    ) : BudgetDecision
}

class OverBudgetException(
    val reasonCode: String,
) : IllegalStateException(reasonCode)

class ApiBudgetGuard(
    private val repository: ApiHubRepository,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun evaluate(estimatedCostMicros: Long): BudgetDecision {
        val policy = repository.getBudgetPolicy()
        val logs = repository.getUsageLogs()
        val now = nowProvider()
        val dayStart = startOfDay(now)
        val monthStart = startOfMonth(now)
        val dailySpent =
            logs.filter { log -> log.createdAt >= dayStart }
                .sumOf { log -> log.estimatedCostMicros }
        val monthlySpent =
            logs.filter { log -> log.createdAt >= monthStart }
                .sumOf { log -> log.estimatedCostMicros }
        val projectedDaily = dailySpent + estimatedCostMicros
        val projectedMonthly = monthlySpent + estimatedCostMicros

        policy?.dailyLimitMicros?.let { dailyLimit ->
            if (projectedDaily > dailyLimit) {
                return BudgetDecision.Blocked("daily_limit")
            }
        }
        policy?.monthlyLimitMicros?.let { monthlyLimit ->
            if (projectedMonthly > monthlyLimit) {
                return BudgetDecision.Blocked("monthly_limit")
            }
        }

        val ratio =
            policy?.dailyLimitMicros
                ?.takeIf { limit -> limit > 0L }
                ?.let { limit -> (projectedDaily.toFloat() / limit.toFloat()).coerceIn(0f, 1f) }
                ?: 0f
        return BudgetDecision.Allowed(
            projectedDailyUsageMicros = projectedDaily,
            projectedMonthlyUsageMicros = projectedMonthly,
            projectedUsageRatio = ratio,
        )
    }

    suspend fun ensureAllowed(estimatedCostMicros: Long) {
        when (val decision = evaluate(estimatedCostMicros)) {
            is BudgetDecision.Allowed -> Unit
            is BudgetDecision.Blocked -> throw OverBudgetException(decision.reason)
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(timestamp)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun startOfMonth(timestamp: Long): Long {
        val zone = ZoneId.systemDefault()
        val zoned = Instant.ofEpochMilli(timestamp).atZone(zone)
        return zoned.toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}
