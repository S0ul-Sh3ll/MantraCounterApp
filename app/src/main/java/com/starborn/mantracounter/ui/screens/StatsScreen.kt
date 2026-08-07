package com.starborn.mantracounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.today
import com.starborn.mantracounter.ui.JapaViewModel
import com.starborn.mantracounter.util.formatDuration
import com.starborn.mantracounter.util.grouped

/**
 * Every japa with its total count and the time spent on it, and the whole-practice totals at the
 * foot. Counts are japa, not malas, as asked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: JapaViewModel,
    onBack: () -> Unit,
) {
    val japas by viewModel.allJapas.collectAsState()
    val week by viewModel.weekTotals.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshDayWindow() }

    val totalCount = japas.sumOf { it.count }
    val totalTime = japas.sumOf { it.totalTimeMs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            TotalsBar(
                japaCount = japas.size,
                totalCount = totalCount,
                totalTime = totalTime,
                week = week,
            )
        },
    ) { padding ->
        if (japas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing counted yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 12.dp,
                ),
            ) {
                items(japas, key = { it.id }) { japa ->
                    StatsRow(japa)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatsRow(japa: Japa) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (japa.archived) "${japa.name} (archived)" else japa.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDuration(japa.totalTimeMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = japa.count.grouped(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TotalsBar(
    japaCount: Int,
    totalCount: Long,
    totalTime: Long,
    week: List<Pair<String, Long>>,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "All japas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = totalCount.grouped(),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "japa counted across $japaCount mantra" +
                            if (japaCount == 1) "" else "s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = formatDuration(totalTime),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "total time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (week.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                WeekChart(week)
            }
        }
    }
}

private val WEEKDAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * This week's japa, Monday to Sunday. Bars are scaled to the busiest day rather than to a fixed
 * ceiling, so a quiet week still reads as a shape instead of seven flat stubs. Today is marked.
 */
@Composable
private fun WeekChart(week: List<Pair<String, Long>>) {
    val peak = week.maxOf { it.second }.coerceAtLeast(1L)
    val todayKey = today()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            week.forEachIndexed { index, (date, value) ->
                val isToday = date == todayKey
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (value > 0) {
                        Text(
                            text = value.grouped(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth()
                            // A day with something counted always shows at least a sliver.
                            .height(((value.toFloat() / peak) * 34f).dp.coerceAtLeast(
                                if (value > 0) 3.dp else 2.dp
                            ))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    value > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                                }
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = WEEKDAY_LETTERS[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "this week · ${week.sumOf { it.second }.grouped()} japa",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
