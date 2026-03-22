package com.wenwentome.reader.feature.apihub

import androidx.lifecycle.ViewModel
import com.wenwentome.reader.data.apihub.ApiHubModule
import com.wenwentome.reader.data.apihub.ApiHubOverviewSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiHubViewModel(
    module: ApiHubModule,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(module.loadOverviewSnapshot().toUiState())
    val uiState: StateFlow<ApiHubUiState> = mutableUiState.asStateFlow()
}

private fun ApiHubOverviewSnapshot.toUiState(): ApiHubUiState =
    ApiHubUiState(
        enabledProviderCount = enabledProviderCount,
        boundCapabilityCount = boundCapabilityCount,
        todayCallCount = todayCallCount,
        budgetUsageRatio = budgetUsageRatio,
        latestError = latestError,
    )
