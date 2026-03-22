package com.wenwentome.reader.feature.apihub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ModelBindingScreen(
    state: ApiHubUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("model-binding-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "模型绑定",
            style = MaterialTheme.typography.headlineSmall,
        )
        state.bindingValidationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.capabilityBindings.isEmpty()) {
            Text(
                text = "尚未配置能力绑定",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.capabilityBindings, key = { binding -> binding.capabilityId }) { binding ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = binding.capabilityId,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "主模型：${binding.primaryLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            binding.fallbackLabel?.let { fallback ->
                                Text(
                                    text = "回退：$fallback",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
