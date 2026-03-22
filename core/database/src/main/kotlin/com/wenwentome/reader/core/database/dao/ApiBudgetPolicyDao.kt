package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiBudgetPolicyEntity
import com.wenwentome.reader.core.model.DEFAULT_API_BUDGET_POLICY_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiBudgetPolicyDao {
    @Upsert
    suspend fun upsert(entity: ApiBudgetPolicyEntity)

    @Query("SELECT * FROM api_budget_policies WHERE policyId = :policyId LIMIT 1")
    fun observeById(policyId: String = DEFAULT_API_BUDGET_POLICY_ID): Flow<ApiBudgetPolicyEntity?>

    @Query("SELECT * FROM api_budget_policies WHERE policyId = :policyId LIMIT 1")
    suspend fun getById(policyId: String = DEFAULT_API_BUDGET_POLICY_ID): ApiBudgetPolicyEntity?

    @Query("DELETE FROM api_budget_policies WHERE policyId = :policyId")
    suspend fun deleteById(policyId: String = DEFAULT_API_BUDGET_POLICY_ID)
}
