package com.starborn.mantracounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.JapaSort
import com.starborn.mantracounter.ui.JapaEvent
import com.starborn.mantracounter.ui.JapaViewModel
import com.starborn.mantracounter.ui.components.AdjustCountDialog
import com.starborn.mantracounter.ui.components.JapaCard
import com.starborn.mantracounter.ui.components.SearchableTopBar
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JapaListScreen(
    viewModel: JapaViewModel,
    onOpenJapa: (Long) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val japas by viewModel.activeJapas.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val searchOpen by viewModel.searchOpen.collectAsState()
    val recentCounts by viewModel.recentCounts.collectAsState()
    val archivedCount by viewModel.archivedCount.collectAsState()
    val listBackground by viewModel.listBackground.collectAsState()
    val sort by viewModel.sort.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    val listBackgroundAlpha by viewModel.listBackgroundAlpha.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Japa?>(null) }
    var confirmDelete by remember { mutableStateOf<Japa?>(null) }
    var adjusting by remember { mutableStateOf<Japa?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshDayWindow() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JapaEvent.MalaComplete ->
                    snackbarHostState.showSnackbar("Mala ${event.malaNumber} complete")

                is JapaEvent.TargetReached ->
                    snackbarHostState.showSnackbar("Lifetime target reached — ${event.name}")

                is JapaEvent.Archived -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Archived ${event.japa.name}",
                        actionLabel = "Undo",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.setArchived(event.japa, false)
                    }
                }

                is JapaEvent.Unarchived -> {}

                is JapaEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        listBackground?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = listBackgroundAlpha,
                modifier = Modifier.fillMaxSize(),
            )
        }

    Scaffold(
        containerColor = if (listBackground != null) Color.Transparent
        else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SearchableTopBar(
                title = "Mantra Counter",
                searchOpen = searchOpen,
                query = query,
                onQueryChange = viewModel::setSearchQuery,
                onSearchOpenChange = viewModel::setSearchOpen,
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            JapaSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = {
                                        if (option == sort) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        } else {
                                            Spacer(Modifier.width(24.dp))
                                        }
                                    },
                                    onClick = {
                                        sortMenuOpen = false
                                        viewModel.setSort(option)
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Stats")
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, null) },
                                trailingIcon = {
                                    if (archivedCount > 0) {
                                        Text(
                                            text = archivedCount.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = { overflowOpen = false; onOpenArchive() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { overflowOpen = false; onOpenSettings() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Japa() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New japa") },
            )
        },
    ) { padding ->
        if (japas.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                title = if (query.isBlank()) "No japas yet" else "No japas match \"$query\"",
                body = if (query.isBlank()) {
                    "Add your first mantra and start counting. Long-press any japa for archive, edit and reset."
                } else {
                    "Try a different word, or clear the search."
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = padding.calculateTopPadding() + 6.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(japas, key = { it.id }) { japa ->
                    val recent = recentCounts[japa.id]
                    JapaCard(
                        japa = japa,
                        todayCount = recent?.first ?: 0L,
                        yesterdayCount = recent?.second ?: 0L,
                        onOpen = { onOpenJapa(japa.id) },
                        onToggleFavourite = { viewModel.toggleFavourite(japa) },
                        onEdit = { editing = japa },
                        onAdjust = { adjusting = japa },
                        onArchiveToggle = { viewModel.setArchived(japa, true) },
                        onDelete = { confirmDelete = japa },
                    )
                }
            }
        }
    }
    }

    editing?.let { japa ->
        EditJapaSheet(
            japa = japa,
            onDismiss = { editing = null },
            onSave = { draft, picked ->
                viewModel.save(draft, picked)
                editing = null
            },
        )
    }

    adjusting?.let { japa ->
        AdjustCountDialog(
            japa = japa,
            currentCount = japa.count,
            onDismiss = { adjusting = null },
            onApply = { delta -> viewModel.adjustCount(japa.id, delta) },
        )
    }

    confirmDelete?.let { japa ->
        DeleteConfirmDialog(
            japa = japa,
            onDismiss = { confirmDelete = null },
            onConfirm = {
                viewModel.delete(japa)
                confirmDelete = null
            },
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
