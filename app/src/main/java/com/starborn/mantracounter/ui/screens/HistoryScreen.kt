package com.starborn.mantracounter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.starborn.mantracounter.data.DailyCount
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.today
import com.starborn.mantracounter.ui.JapaViewModel
import com.starborn.mantracounter.util.grouped
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Read and correct the daily log. Every edit here moves the japa's lifetime total by the same
 * amount, so the headline count always equals the sum of its days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: JapaViewModel,
    onBack: () -> Unit,
) {
    val active by viewModel.activeJapas.collectAsState()
    val archived by viewModel.archivedJapas.collectAsState()
    val japas = remember(active, archived) { active + archived }

    var selectedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(japas) {
        if (selectedId == null || japas.none { it.id == selectedId }) {
            selectedId = japas.firstOrNull()?.id
        }
    }

    val selected = japas.firstOrNull { it.id == selectedId }
    val historyState = remember(selectedId) { selectedId?.let { viewModel.history(it) } }
    val history by (historyState?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    var editing by remember { mutableStateOf<DailyCount?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pickerFor by remember { mutableStateOf<DailyCount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (selected != null) {
                ExtendedFloatingActionButton(
                    onClick = { adding = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add a day") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            JapaPicker(
                japas = japas,
                selected = selected,
                onSelect = { selectedId = it.id },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            if (selected == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No japas yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No days recorded for ${selected.name} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 96.dp,
                    )
                ) {
                    items(history, key = { it.date }) { day ->
                        HistoryRow(
                            day = day,
                            malaSize = selected.malaSize,
                            onEdit = { editing = day },
                            onChangeDate = { pickerFor = day },
                            onDelete = { viewModel.deleteDay(selected.id, day.date) },
                        )
                    }
                }
            }
        }
    }

    val japa = selected
    if (japa != null) {
        editing?.let { day ->
            CountEditDialog(
                title = "Japa on ${day.date}",
                initial = day.count,
                onDismiss = { editing = null },
                onConfirm = { value ->
                    viewModel.setDayCount(japa.id, day.date, value)
                    editing = null
                },
            )
        }

        if (adding) {
            AddDayDialog(
                existing = history.map { it.date }.toSet(),
                onDismiss = { adding = false },
                onConfirm = { date, count ->
                    viewModel.setDayCount(japa.id, date, count)
                    adding = false
                },
            )
        }

        pickerFor?.let { day ->
            DateMoveDialog(
                day = day,
                onDismiss = { pickerFor = null },
                onConfirm = { newDate ->
                    viewModel.moveDay(japa.id, day.date, newDate)
                    pickerFor = null
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JapaPicker(
    japas: List<Japa>,
    selected: Japa?,
    onSelect: (Japa) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected?.name ?: "Choose a japa",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose a japa")
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            japas.forEach { japa ->
                DropdownMenuItem(
                    text = {
                        Text(if (japa.archived) "${japa.name} (archived)" else japa.name)
                    },
                    onClick = { open = false; onSelect(japa) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    day: DailyCount,
    malaSize: Int,
    onEdit: () -> Unit,
    onChangeDate: () -> Unit,
    onDelete: () -> Unit,
) {
    val malas = if (malaSize > 0) day.count / malaSize else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (day.date == today()) "${day.date} · today" else day.date,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${day.count.grouped()} japa · ${malas.grouped()} " +
                    if (malas == 1L) "mala" else "malas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onChangeDate) { Text("Date") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${day.date}")
        }
    }
    HorizontalDivider()
}

@Composable
private fun CountEditDialog(
    title: String,
    initial: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var value by remember { mutableStateOf(initial.toString()) }
    val parsed = value.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(9) },
                    label = { Text("Japa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The japa's total moves by the same amount.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { onConfirm(parsed ?: 0) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDayDialog(
    existing: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var date by remember { mutableStateOf(today()) }
    var count by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val parsed = count.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a day") },
        text = {
            Column {
                Surface(
                    onClick = { showPicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(date)
                        Text("Change", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter(Char::isDigit).take(9) },
                    label = { Text("Japa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (date in existing) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "That day already has an entry — this replaces it.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { onConfirm(date, parsed ?: 0) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showPicker) {
        CalendarDialog(
            initial = date,
            onDismiss = { showPicker = false },
            onPick = { date = it; showPicker = false },
        )
    }
}

@Composable
private fun DateMoveDialog(day: DailyCount, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    CalendarDialog(initial = day.date, onDismiss = onDismiss, onPick = onConfirm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDialog(initial: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    // The picker works in UTC millis; the log is a local yyyy-MM-dd string. Parsing and
    // formatting both in UTC keeps a date from sliding a day either way.
    val utc = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    } }
    val initialMillis = remember(initial) {
        runCatching { utc.parse(initial)?.time }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onPick(utc.format(java.util.Date(it))) }
                },
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
