package com.wenwentome.reader.feature.apihub

import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ApiUsageLog
import com.wenwentome.reader.data.apihub.ApiHubStore
import com.wenwentome.reader.data.apihub.sync.ApiHubMergeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiHubViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun savingBinding_surfacesConflictForUserDecision() = runTest {
        val existing =
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "provider-a",
                primaryModelId = "model-a",
                updatedAt = 1_000L,
            )
        val conflictingDraft =
            existing.copy(
                primaryProviderId = "provider-b",
                primaryModelId = "model-b",
                updatedAt = 1_500L,
            )
        val repository =
            FakeApiHubStore(
                providers =
                    linkedMapOf(
                        "provider-a" to ApiProviderProfile(providerId = "provider-a", displayName = "Provider A"),
                        "provider-b" to ApiProviderProfile(providerId = "provider-b", displayName = "Provider B"),
                    ),
                bindings = linkedMapOf(existing.capabilityId to existing),
            )
        val viewModel =
            ApiHubViewModel(
                repository = repository,
                mergeResolver = ApiHubMergeResolver(conflictWindowMs = 1_000L),
            )
        advanceUntilIdle()

        viewModel.saveBinding(conflictingDraft)
        advanceUntilIdle()

        assertEquals("reader.summary", viewModel.uiState.value.pendingConflict?.capabilityId)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun addSuggestedProvider_addsFirstMissingTemplateAndSelectsIt() = runTest {
        val repository = FakeApiHubStore()
        val viewModel = ApiHubViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.addSuggestedProvider()
        advanceUntilIdle()

        assertEquals(listOf("openai-main"), repository.getProviders().map { it.providerId })
        assertEquals("openai-main", viewModel.uiState.value.selectedProviderId)
    }
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeApiHubStore(
    private val providers: LinkedHashMap<String, ApiProviderProfile> = linkedMapOf(),
    private val models: LinkedHashMap<String, MutableList<ApiModelProfile>> = linkedMapOf(),
    private val bindings: LinkedHashMap<String, ApiCapabilityBinding> = linkedMapOf(),
) : ApiHubStore {
    override suspend fun getProviders(): List<ApiProviderProfile> = providers.values.toList()

    override suspend fun getProvider(providerId: String): ApiProviderProfile? = providers[providerId]

    override suspend fun upsertProvider(profile: ApiProviderProfile) {
        providers[profile.providerId] = profile
    }

    override suspend fun getModels(providerId: String): List<ApiModelProfile> = models[providerId].orEmpty()

    override suspend fun getCapabilityBindings(): List<ApiCapabilityBinding> = bindings.values.toList()

    override suspend fun getCapabilityBinding(capabilityId: String): ApiCapabilityBinding? = bindings[capabilityId]

    override suspend fun upsertCapabilityBinding(binding: ApiCapabilityBinding) {
        bindings[binding.capabilityId] = binding
    }

    override suspend fun getUsageLogs(): List<ApiUsageLog> = emptyList()
}
