package io.legado.app.ui.wenwen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.feature.apihub.ApiHubOverviewScreen
import com.wenwentome.reader.feature.apihub.ApiHubUiState
import io.legado.app.utils.toastOnUi

class WenwenApiHubActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WenwenApiHubScreen(
                    onBackClick = ::finish,
                    onOpenProviders = { showComingSoon("供应商管理") },
                    onOpenBindings = { showComingSoon("能力绑定") },
                    onOpenBudgets = { showComingSoon("预算与回退") },
                    onOpenPrices = { showComingSoon("价格目录") },
                    onOpenUsageLogs = { showComingSoon("调用记录") },
                )
            }
        }
    }

    private fun showComingSoon(label: String) {
        toastOnUi("$label 已接到新入口，下一步继续补真实配置页。")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WenwenApiHubScreen(
    onBackClick: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenBindings: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenPrices: () -> Unit,
    onOpenUsageLogs: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 中心") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IntroBlock(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp),
                )
                ApiHubOverviewScreen(
                    state = ApiHubUiState(
                        providerStatusLabel = "AI 入口已接入，可继续扩展真实供应商配置",
                        bindingStatusLabel = "支持继续挂接 AI 朗读、书摘整理与问答能力",
                        usageStatusLabel = "已提供入口与状态总览",
                        latestError = "当前环境未安装 Java，完整编译与联调将走 GitHub Actions 云端验证",
                    ),
                    onOpenProviders = onOpenProviders,
                    onOpenBindings = onOpenBindings,
                    onOpenBudgets = onOpenBudgets,
                    onOpenPrices = onOpenPrices,
                    onOpenUsageLogs = onOpenUsageLogs,
                )
            }
        }
    }
}

@Composable
private fun IntroBlock(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "这里是文文tome 的 AI 能力入口。",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "当前已接入宿主入口和总览页，后续可继续补供应商配置、能力绑定和调用明细。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
