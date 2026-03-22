package com.wenwentome.reader.feature.apihub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ProviderManagementScreen(
    state: ApiHubUiState,
    onValidateProvider: (String) -> Unit,
    onToggleProviderEnabled: (String, Boolean) -> Unit,
    onAddProvider: () -> Unit,
    onSelectProvider: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("provider-management-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Provider 管理",
            style = MaterialTheme.typography.headlineSmall,
        )
        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.providerCards.isEmpty()) {
                item {
                    Text(
                        text = "还没有 Provider，先从模板新增一个。",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(
                items = state.providerCards,
                key = { provider -> provider.providerId },
            ) { provider ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectProvider(provider.providerId) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = provider.templateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = provider.modelSourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = provider.validationLabel,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${provider.availableModelCount} 个模型",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Button(onClick = { onValidateProvider(provider.providerId) }) {
                                Text("校验")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("已启用")
                                Switch(
                                    checked = provider.enabled,
                                    onCheckedChange = { checked ->
                                        onToggleProviderEnabled(provider.providerId, checked)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = onAddProvider,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("从模板新增")
        }
    }
}
