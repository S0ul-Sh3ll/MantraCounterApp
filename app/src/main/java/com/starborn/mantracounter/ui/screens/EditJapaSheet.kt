package com.starborn.mantracounter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.ui.theme.AccentPalette
import com.starborn.mantracounter.util.grouped
import java.io.File

private val MalaPresets = listOf(27, 54, 108, 1008)

/** Japa, not malas — 1 lakh / 10 lakh / 1 crore are the usual lifetime sankalpa figures. */
private val TargetPresets = listOf(10_000L, 100_000L, 1_000_000L, 10_000_000L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJapaSheet(
    japa: Japa,
    onDismiss: () -> Unit,
    onSave: (Japa, Uri?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isNew = japa.id == 0L

    var name by remember { mutableStateOf(japa.name) }
    var deity by remember { mutableStateOf(japa.deity) }
    var malaSize by remember { mutableStateOf(japa.malaSize.toString()) }
    var target by remember {
        mutableStateOf(if (japa.lifetimeTarget > 0) japa.lifetimeTarget.toString() else "")
    }
    var accentIndex by remember { mutableIntStateOf(japa.accentIndex) }
    var existingBackground by remember { mutableStateOf(japa.backgroundUri) }
    var pickedImage by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedImage = uri }

    val parsedMala = malaSize.toIntOrNull() ?: 0
    val nameValid = name.isNotBlank()
    val malaValid = parsedMala in 1..100_000
    val canSave = nameValid && malaValid

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (isNew) "New japa" else "Edit japa",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Hare Krishna Maha Mantra") },
                singleLine = true,
                isError = name.isNotEmpty() && !nameValid,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = deity,
                onValueChange = { deity = it },
                label = { Text("Deity") },
                placeholder = { Text("Krishna") },
                singleLine = true,
                supportingText = { Text("Optional — the list can be sorted by it") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Japa per mala")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MalaPresets.forEach { preset ->
                    FilterChip(
                        selected = parsedMala == preset,
                        onClick = { malaSize = preset.toString() },
                        label = { Text(preset.toString()) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = malaSize,
                onValueChange = { malaSize = it.filter(Char::isDigit).take(6) },
                label = { Text("Custom") },
                singleLine = true,
                isError = malaSize.isNotEmpty() && !malaValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Lifetime target")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TargetPresets.forEach { preset ->
                    AssistChip(
                        onClick = { target = preset.toString() },
                        label = { Text(shortTarget(preset)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter(Char::isDigit).take(12) },
                label = { Text("Target in japa") },
                placeholder = { Text("Leave empty for no target") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    val t = target.toLongOrNull() ?: 0
                    if (t > 0 && parsedMala > 0) {
                        Text("${t.grouped()} japa · ${(t / parsedMala).grouped()} malas")
                    } else {
                        Text("No target set")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Background image")
            BackgroundPicker(
                pickedImage = pickedImage,
                existingPath = existingBackground,
                onPick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClear = {
                    pickedImage = null
                    existingBackground = null
                },
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Accent colour")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccentPalette.forEachIndexed { index, color ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .clickable { accentIndex = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == accentIndex) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.size(8.dp))
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            japa.copy(
                                name = name.trim(),
                                deity = deity.trim(),
                                malaSize = parsedMala,
                                lifetimeTarget = target.toLongOrNull() ?: 0,
                                accentIndex = accentIndex,
                                backgroundUri = existingBackground,
                            ),
                            pickedImage,
                        )
                    },
                ) { Text(if (isNew) "Add japa" else "Save") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackgroundPicker(
    pickedImage: Uri?,
    existingPath: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val model: Any? = pickedImage ?: existingPath?.let { File(it) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Selected background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black, RoundedCornerShape(14.dp)),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            )
            Text("Tap to change", color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Choose an image")
            }
        }
    }
    if (model != null) {
        TextButton(onClick = onClear) { Text("Remove image") }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

private fun shortTarget(value: Long): String = when {
    value >= 10_000_000 -> "${value / 10_000_000} cr"
    value >= 100_000 -> "${value / 100_000} lakh"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}
