package com.starborn.mantracounter.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.starborn.mantracounter.ui.JapaEvent
import com.starborn.mantracounter.ui.JapaViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JapaViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val listBackground by viewModel.listBackground.collectAsState()
    val alpha by viewModel.listBackgroundAlpha.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val bellEnabled by viewModel.bellEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is JapaEvent.Message) snackbarHostState.showSnackbar(event.text)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.setListBackground(uri) }

    // A document the user names and places themselves — no storage permission, and the backup
    // lands somewhere they can actually find it again.
    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }

    val backupOpener = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.previewRestore(uri) }

    val restorePreview by viewModel.restorePreview.collectAsState()
    val lastExport by viewModel.lastExport.collectAsState()
    val shareUri by viewModel.shareUri.collectAsState()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SettingsSection("Main screen background")
            Text(
                text = "Shown behind the list of japas. It is faded so the cards stay readable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val path = listBackground
                if (path != null) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Main screen background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black, RoundedCornerShape(14.dp)),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
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

            if (listBackground != null) {
                TextButton(onClick = { viewModel.setListBackground(null) }) {
                    Text("Remove background")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Visibility ${(alpha * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = alpha,
                    onValueChange = viewModel::setListBackgroundAlpha,
                    valueRange = 0.05f..1f,
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SettingsSection("Counting")
            Surface(
                onClick = onOpenHistory,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("History", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "See and correct past days — change a count, move it to " +
                                "another date, or add a day you counted off the app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SettingsSection("Feedback")
            SettingsSwitch(
                title = "Vibration",
                body = "A light tick on every bead, and a stronger pulse when a mala closes.",
                checked = vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
            Spacer(Modifier.height(8.dp))
            SettingsSwitch(
                title = "Bell on mala completion",
                body = "Rings once each time you finish a mala.",
                checked = bellEnabled,
                onCheckedChange = viewModel::setBellEnabled,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SettingsSection("Backup")
            Text(
                text = "Writes every japa — active and archived — with its full statistics and " +
                    "day-by-day log to a plain .txt file you choose the location for.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = { backupSaver.launch(viewModel.defaultBackupFileName()) }) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Export backup to .txt")
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Restoring reads a backup file back in and recreates the japas with their " +
                    "counts, targets, mala size, favourites and full daily history. Background " +
                    "images are photos rather than text, so they are not in the file. A japa " +
                    "whose name already exists is left untouched.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    backupOpener.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                }
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Restore from a backup file")
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Created with love & devotion by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Rishabh Dahiya",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    LaunchedEffect(shareUri) {
        val uri = shareUri ?: return@LaunchedEffect
        viewModel.clearShare()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mantra Counter backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Share backup")) }
    }

    lastExport?.let { savedTo ->
        AlertDialog(
            onDismissRequest = viewModel::clearLastExport,
            title = { Text("Backup saved") },
            text = {
                Text(
                    "The file is where you chose to put it. You can also send a copy to " +
                        "WhatsApp, Telegram, email or anywhere else that takes a file."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLastExport()
                        viewModel.prepareShare()
                    }
                ) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearLastExport) { Text("Done") }
            },
        )
    }

    restorePreview?.let { preview ->
        RestoreConfirmDialog(
            preview = preview,
            onDismiss = viewModel::cancelRestore,
            onConfirm = viewModel::confirmRestore,
        )
    }
}

@Composable
private fun RestoreConfirmDialog(
    preview: com.starborn.mantracounter.ui.RestorePreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore this backup?") },
        text = {
            Column {
                Text(
                    text = "Found ${preview.japaCount} japa" +
                        (if (preview.japaCount == 1) "" else "s") +
                        " with ${preview.dayCount} recorded days.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                preview.names.take(8).forEach { name ->
                    Text("· $name", style = MaterialTheme.typography.bodyMedium)
                }
                if (preview.names.size > 8) {
                    Text(
                        text = "· and ${preview.names.size - 8} more",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Nothing already on this phone is changed — a japa whose name " +
                        "already exists is skipped.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
