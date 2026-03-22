package com.wenwentome.reader.feature.apihub

import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiModelValidationState
import com.wenwentome.reader.core.model.ApiOverBudgetAction
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.core.model.ApiUsageLog
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.data.apihub.ApiHubStore
import androidx.lifecycle.ViewModel
import com.wenwentome.reader.data.apihub.ApiHubRepository
import com.wenwentome.reader.data.apihub.ApiHubModule
import com.wenwentome.reader.data.apihub.ApiHubOverviewSnapshot
import com.wenwentome.reader.data.apihub.provider.NoOpModelDiscoveryService
import com.wenwentome.reader.data.apihub.provider.ProviderCatalogRepository
import com.wenwentome.reader.data.apihub.provider.ProviderValidationService
import com.wenwentome.reader.data.apihub.pricing.ApiPricingRepository
import com.wenwentome.reader.data.apihub.sync.ApiHubMergeResolver
import com.wenwentome.reader.data.apihub.sync.MergeBindingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class ApiHubViewModel(
    private val repository: ApiHubStore,
    private val runtimeRepository: ApiHubRepository? = repository as? ApiHubRepository,
    private val providerCatalogRepository: ProviderCatalogRepository = ProviderCatalogRepository(repository, NoOpModelDiscoveryService),
    private val providerValidationService: ProviderValidationService = ProviderValidationService(providerCatalogRepository),
    private val mergeResolver: ApiHubMergeResolver = ApiHubMergeResolver(),
    initialSnapshot: ApiHubOverviewSnapshot = ApiHubOverviewSnapshot(),
) : ViewModel() {
    constructor(module: ApiHubModule) : this(
        repository = module.repository,
        providerCatalogRepository = module.providerCatalogRepository,
        providerValidationService = module.providerValidationService,
        mergeResolver = module.mergeResolver,
        initialSnapshot = module.loadOverviewSnapshot(),
    )

    private val providerValidationLabels = linkedMapOf<String, String>()
    private var pendingRemoteDraft: ApiCapabilityBinding? = null
    private val mutableUiState = MutableStateFlow(initialSnapshot.toUiState())
    val uiState: StateFlow<ApiHubUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshState()
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            refreshState(selectedProviderId = providerId)
        }
    }

    fun validateProvider(providerId: String) {
        viewModelScope.launch {
            val result = providerValidationService.validateProvider(providerId)
            providerValidationLabels[providerId] = result.reason
            refreshState()
        }
    }

    fun toggleProviderEnabled(
        providerId: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val provider = repository.getProvider(providerId) ?: return@launch
            repository.upsertProvider(
                provider.copy(
                    enabled = enabled,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            refreshState(selectedProviderId = providerId)
        }
    }

    fun addSuggestedProvider() {
        viewModelScope.launch {
            val existingIds = repository.getProviders().map { provider -> provider.providerId }.toSet()
            val template =
                providerCatalogRepository.templates().firstOrNull { candidate ->
                    candidate.templateId !in existingIds
                } ?: return@launch
            val provider =
                providerCatalogRepository.createProviderFromTemplate(template.templateId)
                    ?: return@launch
            repository.upsertProvider(provider.copy(updatedAt = System.currentTimeMillis()))
            refreshState(selectedProviderId = provider.providerId)
        }
    }

    fun saveBinding(draft: ApiCapabilityBinding) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val existing = repository.getCapabilityBinding(draft.capabilityId)
            if (existing != null && existing != draft) {
                when (val mergeResult = mergeResolver.mergeBinding(local = existing, remote = draft)) {
                    is MergeBindingResult.Resolved -> {
                        repository.upsertCapabilityBinding(mergeResult.binding)
                        pendingRemoteDraft = null
                        mutableUiState.value = mutableUiState.value.copy(pendingConflict = null)
                        refreshState(selectedProviderId = draft.primaryProviderId)
                    }

                    is MergeBindingResult.Conflict -> {
                        pendingRemoteDraft = draft
                        mutableUiState.value = mutableUiState.value.copy(
                            pendingConflict = mergeResult.pending,
                            bindingValidationMessage = null,
                        )
                    }
                }
                return@launch
            }

            val validation =
                providerValidationService.validateBinding(
                    providerId = draft.primaryProviderId,
                    modelId = draft.primaryModelId,
                )
            if (!validation.allowed) {
                mutableUiState.value = mutableUiState.value.copy(bindingValidationMessage = validation.reason)
                return@launch
            }
            repository.upsertCapabilityBinding(draft.copy(updatedAt = System.currentTimeMillis()))
            pendingRemoteDraft = null
            mutableUiState.value = mutableUiState.value.copy(
                bindingValidationMessage = validation.reason,
                pendingConflict = null,
            )
            refreshState(selectedProviderId = draft.primaryProviderId)
        }
    }

    fun keepLocalBinding() {
        pendingRemoteDraft = null
        mutableUiState.value = mutableUiState.value.copy(pendingConflict = null)
    }

    fun useRemoteBinding() {
        val draft = pendingRemoteDraft ?: mutableUiState.value.pendingConflict?.remote ?: return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.upsertCapabilityBinding(draft.copy(updatedAt = System.currentTimeMillis()))
            pendingRemoteDraft = null
            mutableUiState.value = mutableUiState.value.copy(pendingConflict = null)
            refreshState(selectedProviderId = draft.primaryProviderId)
        }
    }

    private suspend fun refreshState(selectedProviderId: String? = mutableUiState.value.selectedProviderId) {
        val providers = repository.getProviders()
        val bindings = repository.getCapabilityBindings()
        val usageLogs = runtimeRepository?.getUsageLogs().orEmpty()
        val budgetPolicy = runtimeRepository?.getBudgetPolicy()
        val pricingRepository = runtimeRepository?.let(::ApiPricingRepository)
        val selectedId = selectedProviderId ?: providers.firstOrNull()?.providerId
        val providerCards =
            providers.map { provider ->
                ProviderCardUiState(
                    providerId = provider.providerId,
                    displayName = provider.displayName,
                    templateLabel = providerCatalogRepository.templateLabelFor(provider),
                    modelSourceLabel = provider.modelSource.toLabel(),
                    validationLabel = providerValidationLabels[provider.providerId] ?: "未校验",
                    enabled = provider.enabled,
                    availableModelCount = providerCatalogRepository.modelsFor(provider.providerId).size,
                )
            }
        val selectedModels =
            selectedId?.let { providerId ->
                providerCatalogRepository.modelsFor(providerId).map { model -> model.toUiState() }
            }.orEmpty()
        val todayStart = startOfDay(System.currentTimeMillis())
        val todayLogs = usageLogs.filter { log -> log.createdAt >= todayStart }
        val todaySpentMicros = todayLogs.sumOf { log -> log.estimatedCostMicros }
        val budgetRatio =
            budgetPolicy?.dailyLimitMicros
                ?.takeIf { limit -> limit > 0L }
                ?.let { limit -> (todaySpentMicros.toFloat() / limit.toFloat()).coerceIn(0f, 1f) }
                ?: mutableUiState.value.budgetUsageRatio
        val priceCatalogEntries =
            if (runtimeRepository == null || pricingRepository == null) {
                emptyList()
            } else {
                providers.flatMap { provider ->
                    repository.getModels(provider.providerId).map { model ->
                        val resolvedPrice = pricingRepository.resolve(provider.providerId, model.modelId)
                        model.toPriceCatalogUiState(
                            providerLabel = provider.displayName,
                            override =
                                runtimeRepository.findPriceOverride(
                                    providerId = provider.providerId,
                                    modelId = model.modelId,
                                ),
                            inputPriceLabel = formatCost(resolvedPrice?.inputPricePer1kMicros),
                            outputPriceLabel = formatCost(resolvedPrice?.outputPricePer1kMicros),
                            requestPriceLabel = formatCost(resolvedPrice?.requestPricePerCallMicros),
                        )
                    }
                }
            }
        mutableUiState.value =
            mutableUiState.value.copy(
                enabledProviderCount = providers.count { provider -> provider.enabled },
                boundCapabilityCount = bindings.count { binding -> binding.enabled },
                todayCallCount = todayLogs.size,
                budgetUsageRatio = budgetRatio,
                latestError = usageLogs.firstOrNull { log -> !log.success }?.errorMessage,
                budgetSummary =
                    BudgetSummaryUiState(
                        dailyLimitLabel = formatCost(budgetPolicy?.dailyLimitMicros),
                        monthlyLimitLabel = formatCost(budgetPolicy?.monthlyLimitMicros),
                        todaySpentLabel = formatCost(todaySpentMicros),
                        projectedUsageLabel = "${(budgetRatio * 100).toInt()}%",
                        overBudgetActionLabel = budgetPolicy?.overBudgetAction.toLabel(),
                        fallbackEnabled = budgetPolicy?.fallbackToLowerCostModel ?: true,
                    ),
                priceCatalogEntries = priceCatalogEntries,
                usageLogEntries = usageLogs.take(20).map { log -> log.toUsageLogUiState() },
                providerCards = providerCards,
                capabilityBindings = bindings.map { binding -> binding.toUiState() },
                selectedProviderId = selectedId,
                selectedProviderModels = selectedModels,
            )
    }
}

private fun ApiHubOverviewSnapshot.toUiState(): ApiHubUiState =
    ApiHubUiState(
        enabledProviderCount = enabledProviderCount,
        boundCapabilityCount = boundCapabilityCount,
        todayCallCount = todayCallCount,
        budgetUsageRatio = budgetUsageRatio,
        latestError = latestError,
    )

private fun ProviderModelSource.toLabel(): String =
    when (this) {
        ProviderModelSource.REMOTE_DISCOVERY -> "远端发现"
        ProviderModelSource.PRESET_CATALOG -> "预设目录"
        ProviderModelSource.MANUAL -> "手动录入"
    }

private fun ApiModelProfile.toUiState(): ModelOptionUiState =
    ModelOptionUiState(
        modelId = modelId,
        label = label,
        sourceLabel = source.toLabel(),
        validationLabel =
            when (validationState) {
                ApiModelValidationState.UNVERIFIED -> "未校验"
                ApiModelValidationState.VALID -> "已通过"
                ApiModelValidationState.FAILED -> "已失败"
            },
    )

private fun ApiCapabilityBinding.toUiState(): CapabilityBindingCardUiState =
    CapabilityBindingCardUiState(
        capabilityId = capabilityId,
        primaryLabel = "$primaryProviderId / $primaryModelId",
        fallbackLabel =
            if (fallbackProviderId.isNullOrBlank() || fallbackModelId.isNullOrBlank()) {
                null
            } else {
                "$fallbackProviderId / $fallbackModelId"
            },
        enabled = enabled,
    )

private fun ApiModelProfile.toPriceCatalogUiState(
    providerLabel: String,
    override: ApiPriceOverride?,
    inputPriceLabel: String,
    outputPriceLabel: String,
    requestPriceLabel: String,
): PriceCatalogEntryUiState =
    PriceCatalogEntryUiState(
        providerId = providerId,
        providerLabel = providerLabel,
        modelId = modelId,
        modelLabel = label,
        inputPriceLabel =
            if (override?.inputPricePer1kMicros != null) {
                "$inputPriceLabel · 手动覆盖"
            } else {
                inputPriceLabel
            },
        outputPriceLabel =
            if (override?.outputPricePer1kMicros != null) {
                "$outputPriceLabel · 手动覆盖"
            } else {
                outputPriceLabel
            },
        requestPriceLabel = requestPriceLabel,
    )

private fun ApiUsageLog.toUsageLogUiState(): UsageLogEntryUiState =
    UsageLogEntryUiState(
        callId = callId,
        title = "$capabilityId · $providerId/$modelId",
        detail =
            listOfNotNull(
                bookId,
                chapterRef,
                errorMessage,
            ).joinToString(" · "),
        costLabel = formatCost(estimatedCostMicros),
        statusLabel = if (success) "成功" else "失败",
        usedFallback = usedFallback,
    )

private fun ApiOverBudgetAction?.toLabel(): String =
    when (this) {
        ApiOverBudgetAction.DOWNGRADE -> "自动降级"
        ApiOverBudgetAction.PAUSE -> "暂停调用"
        ApiOverBudgetAction.ASK, null -> "按需确认"
    }

private fun formatCost(valueMicros: Long?): String {
    if (valueMicros == null) return "未配置"
    return String.format(Locale.US, "%.4f 元", valueMicros / 1_000_000.0)
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
