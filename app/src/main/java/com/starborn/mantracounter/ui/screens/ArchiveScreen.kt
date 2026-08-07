package com.starborn.mantracounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.groupByDeity
import com.starborn.mantracounter.ui.JapaViewModel
import com.starborn.mantracounter.ui.components.AdjustCountDialog
import com.starborn.mantracounter.ui.components.JapaCard
import com.starborn.mantracounter.ui.components.SearchableTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: JapaViewModel,
    onBack: () -> Unit,
    onOpenJapa: (Long) -> Unit,
) {
    val japas by viewModel.archivedJapas.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val searchOpen by viewModel.searchOpen.collectAsState()
    val recentCounts by viewModel.recentCounts.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Japa?>(null) }
    var confirmDelete by remember { mutableStateOf<Japa?>(null) }
    var adjusting by remember { mutableStateOf<Japa?>(null) }
    var collapsed by remember { mutableStateOf(emptySet<String>()) }

    // Archived japas are filed by deity. Anything without one goes to Uncategorized, which sits
    // last however the named folders sort.
    val folders = remember(japas) { japas.groupByDeity() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SearchableTopBar(
                title = "Archive",
                searchOpen = searchOpen,
                query = query,
                onQueryChange = viewModel::setSearchQuery,
                onSearchOpenChange = viewModel::setSearchOpen,
                placeholder = "Search archived japas",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (japas.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                title = if (query.isBlank()) "Archive is empty" else "No archived japas match \"$query\"",
                body = if (query.isBlank()) {
                    "Long-press a japa on the main list and choose Archive to move it here. Counts are kept."
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
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                folders.forEach { (deity, members) ->
                    item(key = "folder-$deity") {
                        FolderHeader(
                            deity = deity,
                            count = members.size,
                            expanded = deity !in collapsed,
                            onToggle = {
                                collapsed = if (deity in collapsed) {
                                    collapsed - deity
                                } else {
                                    collapsed + deity
                                }
                            },
                        )
                    }
                    if (deity !in collapsed) {
                        items(members, key = { it.id }) { japa ->
                            val recent = recentCounts[japa.id]
                            JapaCard(
                                japa = japa,
                                todayCount = recent?.first ?: 0L,
                                yesterdayCount = recent?.second ?: 0L,
                                onOpen = { onOpenJapa(japa.id) },
                                onToggleFavourite = { viewModel.toggleFavourite(japa) },
                                onEdit = { editing = japa },
                                onAdjust = { adjusting = japa },
                                onArchiveToggle = { viewModel.setArchived(japa, false) },
                                onDelete = { confirmDelete = japa },
                            )
                        }
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
private fun FolderHeader(
    deity: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = deity,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess
                else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $deity" else "Expand $deity",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DeleteConfirmDialog(japa: Japa, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${japa.name}?") },
        text = {
            Text(
                "This removes the japa and its count permanently. " +
                    "If you only want it off the main list, archive it instead."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
