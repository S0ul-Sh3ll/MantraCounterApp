package com.starborn.mantracounter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.util.grouped

/**
 * Manual correction, in place of the old reset.
 *
 * Reset was one mis-tap away from destroying a lifetime count, and there is no undo for that.
 * Adding or removing a stated number does everything reset was actually being used for — fixing
 * a miscount — without ever putting the whole history at risk.
 */
@Composable
fun AdjustCountDialog(
    japa: Japa,
    currentCount: Long,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    val parsed = amount.toLongOrNull() ?: 0L
    val valid = parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust count") },
        text = {
            Column {
                Text(
                    text = "${japa.name} is at ${currentCount.grouped()} japa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(9) },
                    label = { Text("Number of japa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1L, 10L, japa.malaSize.toLong()).distinct().forEach { quick ->
                        AssistChip(
                            onClick = { amount = quick.toString() },
                            label = {
                                Text(
                                    if (quick == japa.malaSize.toLong()) "1 mala"
                                    else quick.toString()
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Adjustments count towards today in the daily log.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(enabled = valid, onClick = { onApply(-parsed); onDismiss() }) {
                    Text("Remove")
                }
                TextButton(enabled = valid, onClick = { onApply(parsed); onDismiss() }) {
                    Text("Add")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
