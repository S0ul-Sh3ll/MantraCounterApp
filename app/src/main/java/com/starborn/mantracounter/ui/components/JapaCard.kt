package com.starborn.mantracounter.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.ui.theme.AccentPalette
import com.starborn.mantracounter.util.grouped
import java.io.File

/**
 * Deliberately short: 86dp including the progress hairline, versus the tall blocks the reference
 * app uses. Eight or so japas fit on a normal phone screen instead of four. Count, malas and
 * target progress share one meta row, today and yesterday share the next, and the lifetime-target
 * progress is a hairline along the bottom edge rather than a bar of its own.
 */
private val CardHeight = 86.dp

private val FavouriteGold = Color(0xFFE8B23A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JapaCard(
    japa: Japa,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    todayCount: Long,
    yesterdayCount: Long,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = AccentPalette[japa.accentIndex.mod(AccentPalette.size)]
    val hasImage = !japa.backgroundUri.isNullOrBlank()

    Surface(
        modifier = modifier.fillMaxWidth().height(CardHeight),
        shape = RoundedCornerShape(18.dp),
        color = if (hasImage) Color.Black else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true })
        ) {
            if (hasImage) {
                AsyncImage(
                    model = File(japa.backgroundUri!!),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Readability scrim: heaviest where the text sits, clearing toward the right so
                // the photo still reads as a photo.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black.copy(alpha = 0.72f),
                                0.75f to Color.Black.copy(alpha = 0.30f),
                                1f to Color.Black.copy(alpha = 0.20f),
                            )
                        )
                )
            } else {
                // Accent stripe stands in for the image.
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(accent)
                )
            }

            val titleColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface
            val metaColor =
                if (hasImage) Color.White.copy(alpha = 0.82f)
                else MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (hasImage) 16.dp else 18.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    // A long mantra name scrolls rather than being cut off — some of these run
                    // to a full verse and the tail is the part that distinguishes them.
                    Text(
                        text = japa.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = metaLine(japa),
                        style = MaterialTheme.typography.labelMedium,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = dayLine(japa, todayCount, yesterdayCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (japa.favourite) Icons.Default.Star
                            else Icons.Default.StarBorder,
                            contentDescription = if (japa.favourite) {
                                "Remove ${japa.name} from favourites"
                            } else {
                                "Add ${japa.name} to favourites"
                            },
                            tint = when {
                                japa.favourite -> FavouriteGold
                                hasImage -> Color.White.copy(alpha = 0.75f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    JapaCardMenu(
                        expanded = menuOpen,
                        archived = japa.archived,
                        onDismiss = { menuOpen = false },
                        onEdit = onEdit,
                        onAdjust = onAdjust,
                        onArchiveToggle = onArchiveToggle,
                        onDelete = onDelete,
                    )
                }
            }

            // Lifetime-target progress as a hairline on the bottom edge — a full progress bar
            // would cost another 20dp of card height.
            if (japa.hasTarget) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (hasImage) Color.White.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(japa.targetProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                            .background(if (hasImage) Color.White else accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun JapaCardMenu(
    expanded: Boolean,
    archived: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Edit") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = { onDismiss(); onEdit() },
        )
        DropdownMenuItem(
            text = { Text("Adjust count") },
            leadingIcon = { Icon(Icons.Default.Calculate, null) },
            onClick = { onDismiss(); onAdjust() },
        )
        DropdownMenuItem(
            text = { Text(if (archived) "Restore" else "Archive") },
            leadingIcon = {
                Icon(if (archived) Icons.Default.Unarchive else Icons.Default.Inventory2, null)
            },
            onClick = { onDismiss(); onArchiveToggle() },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = { onDismiss(); onDelete() },
        )
    }
}

private fun dayLine(japa: Japa, today: Long, yesterday: Long): String = buildString {
    if (japa.deity.isNotBlank()) {
        append(japa.deity)
        append(" · ")
    }
    append("today ")
    append(today.grouped())
    append(" · yesterday ")
    append(yesterday.grouped())
}

/** "1,296 · 12 malas · 5% of 25,000" — everything the tall card used three lines for. */
private fun metaLine(japa: Japa): String = buildString {
    append(japa.count.grouped())
    append(" · ")
    append(japa.malasCompleted.grouped())
    append(if (japa.malasCompleted == 1L) " mala" else " malas")
    if (japa.hasTarget) {
        append(" · ")
        if (japa.targetReached) {
            append("target reached")
        } else {
            append("${(japa.targetProgress * 100).toInt()}% of ${japa.lifetimeTarget.grouped()}")
        }
    }
}
