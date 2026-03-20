package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.ReaderMode

@Composable
fun ReaderModePicker(
    selectedMode: ReaderMode,
    onModeSelected: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("reader-mode-picker"),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ReaderMode.entries.forEach { mode ->
                TextButton(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.testTag("reader-mode-${mode.name.lowercase()}"),
                ) {
                    Text(
                        text = mode.label(),
                        color = if (mode == selectedMode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

private fun ReaderMode.label(): String =
    when (this) {
        ReaderMode.SIMULATED_PAGE_TURN -> "仿真"
        ReaderMode.HORIZONTAL_PAGING -> "横向"
        ReaderMode.VERTICAL_SCROLL -> "纵向"
    }
