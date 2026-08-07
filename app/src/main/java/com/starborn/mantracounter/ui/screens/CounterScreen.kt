package com.starborn.mantracounter.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.starborn.mantracounter.data.Counting
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.TimerRun
import com.starborn.mantracounter.data.today
import com.starborn.mantracounter.ui.JapaViewModel
import com.starborn.mantracounter.ui.components.AdjustCountDialog
import com.starborn.mantracounter.ui.components.DefaultBeadStep
import com.starborn.mantracounter.ui.components.MalaStrand
import com.starborn.mantracounter.ui.theme.AccentPalette
import com.starborn.mantracounter.util.grouped
import com.starborn.mantracounter.util.hapticTick
import com.starborn.mantracounter.util.rememberSoundPlayer
import com.starborn.mantracounter.util.vibrateMalaComplete
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** How long counts are held in memory before being written to the database. */
private const val FLUSH_INTERVAL_MS = 350L

/** The per-japa background sits behind the mala at this opacity rather than being scrimmed out. */
private const val BACKGROUND_ALPHA = 0.5f

/** Buttons let a tenth of the background through rather than sitting on it as solid discs. */
private const val BUTTON_ALPHA = 0.9f

/** The three ways of counting. Chosen per session, on opening a japa. */
enum class CountMode { Strand, Tap, Timer }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    japaId: Long,
    viewModel: JapaViewModel,
    onBack: () -> Unit,
) {
    // A session that runs past midnight belongs to the day it started on, so the date is fixed
    // when the screen opens rather than read at each write.
    val sessionDate = remember(japaId) { viewModel.startSessionDate() }

    val japaState = remember(japaId) { viewModel.japa(japaId) }
    val japa by japaState.collectAsState()
    val dayState = remember(japaId, sessionDate) { viewModel.dayCount(japaId, sessionDate) }
    val sessionDayCount by dayState.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val bellEnabled by viewModel.bellEnabled.collectAsState()
    val timerInterval by viewModel.timerInterval.collectAsState()
    val timerEndMalas by viewModel.timerEndMalas.collectAsState()

    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val sound = rememberSoundPlayer()
    val snackbarHostState = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Japa?>(null) }
    var confirmDelete by remember { mutableStateOf<Japa?>(null) }
    var adjusting by remember { mutableStateOf(false) }

    // Asked once per visit, as requested — not remembered between sessions.
    var mode by remember(japaId) { mutableStateOf<CountMode?>(null) }

    // The japa flow starts null while Room loads. Without this the screen would treat that first
    // frame as "deleted" and navigate straight back out again.
    var everLoaded by remember(japaId) { mutableStateOf(false) }

    // Counts are held in local state and flushed in batches: a fast swipe produces japa faster
    // than a database round trip, and the number under your thumb must never lag.
    var localCount by remember(japaId) { mutableLongStateOf(-1L) }
    var pending by remember(japaId) { mutableLongStateOf(0L) }
    var residual by remember(japaId) { mutableFloatStateOf(0f) }
    var dragging by remember(japaId) { mutableStateOf(false) }

    var timerRunning by remember(japaId) { mutableStateOf(false) }
    // The session's finish line, anchored to the real count when Start was pressed.
    var timerRun by remember(japaId) { mutableStateOf<TimerRun?>(null) }

    LaunchedEffect(japa) {
        val loaded = japa
        if (loaded != null && !everLoaded) {
            everLoaded = true
            if (localCount < 0) localCount = loaded.count
        }
    }

    LaunchedEffect(japaId) {
        while (true) {
            delay(FLUSH_INTERVAL_MS)
            if (pending != 0L) {
                val delta = pending
                pending = 0
                viewModel.commitBeads(japaId, delta, sessionDate)
            }
        }
    }

    DisposableEffect(japaId) {
        // The screen is kept awake for the whole app in MainActivity, so there is nothing to set
        // here — only the last write to make good as the screen leaves.
        onDispose {
            if (pending != 0L) {
                viewModel.commitBeads(japaId, pending, sessionDate)
                pending = 0
            }
        }
    }

    // Settle the strand back onto the centre line when the finger lifts.
    LaunchedEffect(dragging) {
        if (!dragging && residual != 0f) {
            animate(
                initialValue = residual,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
            ) { value, _ -> residual = value }
        }
    }

    val current = japa
    if (current == null) {
        if (everLoaded) LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize())
        return
    }

    val displayCount = if (localCount < 0) current.count else localCount
    val shown = current.copy(count = displayCount)
    val accent = AccentPalette[current.accentIndex.mod(AccentPalette.size)]
    val hasImage = !current.backgroundUri.isNullOrBlank()
    val stepPx = with(density) { DefaultBeadStep.toPx() }

    /**
     * Moves the count by [delta].
     *
     * The base is read from `localCount` at call time, never from a value captured when this
     * composable ran — the gesture handler outlives the composition that created it, and reading
     * a captured value there is exactly what made the number freeze after one japa.
     */
    val step: (Long) -> Unit = step@{ delta ->
        // Fall back to the stored count rather than doing nothing: a step that silently declines
        // to count left the timer ticking against a total it could never reach.
        val base = if (localCount < 0) current.count else localCount
        val next = Counting.next(base, delta)
        if (next == base) return@step

        pending += next - base
        localCount = next

        val mala = Counting.malaReached(base, next, current.malaSize)
        if (mala != null) {
            if (vibrationEnabled) context.vibrateMalaComplete()
            // In timer mode the tick already rings; a second bell on the same count would double.
            if (bellEnabled && mode != CountMode.Timer) sound.playBell()
            scope.launch { snackbarHostState.showSnackbar("Mala $mala complete") }
        } else if (vibrationEnabled) {
            view.hapticTick()
        }

        if (Counting.reachedTarget(base, next, current.lifetimeTarget)) {
            scope.launch { snackbarHostState.showSnackbar("Lifetime target reached") }
        }
    }
    val latestStep by rememberUpdatedState(step)

    // What a session would be if started now — shown before the first Start.
    val plannedTotal = timerEndMalas.coerceAtLeast(TimerRun.DEFAULT_END_MALAS).toLong() *
        current.malaSize.coerceAtLeast(1)

    // The session ends here, watching the count itself rather than trusting the ticking loop to
    // notice. Whatever moves the count — a tick, a manual adjustment — the finish is caught, and
    // a run with no session attached cannot leave the timer running against nothing.
    LaunchedEffect(japaId) {
        snapshotFlow {
            Triple(timerRunning, timerRun, if (localCount < 0) 0L else localCount)
        }.collect { (running, run, count) ->
            if (!running) return@collect
            if (run == null) {
                timerRunning = false
                return@collect
            }
            if (run.isComplete(count)) {
                timerRunning = false
                // A beat of quiet before the Om, so it does not land on top of the last bell.
                delay(1_000)
                if (bellEnabled) sound.playOm()
                snackbarHostState.showSnackbar(
                    "Session complete — $timerEndMalas " +
                        if (timerEndMalas == 1) "mala" else "malas"
                )
            }
        }
    }

    LaunchedEffect(timerRunning, timerInterval, mode, japaId) {
        if (!timerRunning || mode != CountMode.Timer) return@LaunchedEffect
        while (timerRunning) {
            delay(timerInterval.coerceAtLeast(1) * 1000L)
            val run = timerRun
            if (run == null || run.isComplete(localCount)) break
            latestStep(1)
            if (bellEnabled) sound.playBell()
        }
    }

    // Time spent, accrued only while this screen is open and the app is actually in front, so a
    // phone left face-down does not quietly inflate the figure.
    LaunchedEffect(japaId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = SystemClock.elapsedRealtime()
            try {
                while (true) {
                    delay(5_000)
                    val now = SystemClock.elapsedRealtime()
                    viewModel.addTime(japaId, now - last)
                    last = now
                }
            } finally {
                viewModel.addTime(japaId, SystemClock.elapsedRealtime() - last)
            }
        }
    }

    if (mode == null) {
        ModeChooser(onPick = { mode = it }, onCancel = onBack)
    }

    Box(Modifier.fillMaxSize()) {
        if (hasImage) {
            AsyncImage(
                model = File(current.backgroundUri!!),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = BACKGROUND_ALPHA,
                modifier = Modifier.fillMaxSize(),
            )
            // Just enough theme colour back over the top to keep text legible against a busy
            // photo, without hiding the photo the way a heavy scrim does.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Text(
                            text = current.name,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        )
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Switch counting mode") },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                                    onClick = {
                                        menuOpen = false
                                        timerRunning = false
                                        timerRun = null
                                        mode = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Adjust count") },
                                    leadingIcon = { Icon(Icons.Default.Calculate, null) },
                                    onClick = { menuOpen = false; adjusting = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuOpen = false; editing = current },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (current.archived) "Restore" else "Archive") },
                                    leadingIcon = {
                                        Icon(
                                            if (current.archived) Icons.Default.Unarchive
                                            else Icons.Default.Inventory2,
                                            null
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.setArchived(current, !current.archived)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { menuOpen = false; confirmDelete = current },
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                when (mode) {
                    CountMode.Tap -> TapCounter(
                        japa = shown,
                        today = sessionDayCount ?: 0L,
                        dayLabel = dayLabel(sessionDate),
                        accent = accent,
                        onIncrement = { latestStep(1) },
                        onDecrement = { latestStep(-1) },
                    )

                    CountMode.Timer -> TimerCounter(
                        japa = shown,
                        today = sessionDayCount ?: 0L,
                        dayLabel = dayLabel(sessionDate),
                        accent = accent,
                        interval = timerInterval,
                        endMalas = timerEndMalas,
                        running = timerRunning,
                        counted = timerRun?.counted(displayCount) ?: 0L,
                        target = timerRun?.total ?: plannedTotal,
                        onIntervalChange = viewModel::setTimerInterval,
                        onEndMalasChange = {
                            // Changing the length starts a fresh session rather than moving the
                            // finish line of one already under way.
                            timerRunning = false
                            timerRun = null
                            viewModel.setTimerEndMalas(it)
                        },
                        onToggleRun = {
                            val run = timerRun
                            if (run == null || run.isComplete(displayCount)) {
                                timerRun = TimerRun.start(
                                    currentCount = displayCount,
                                    endMalas = timerEndMalas,
                                    malaSize = current.malaSize,
                                )
                            }
                            timerRunning = !timerRunning
                        },
                        onResetRun = { timerRunning = false; timerRun = null },
                    )

                    else -> {
                        // The gesture is attached to the whole area, panel included. A phone is
                        // held low, so the natural place to start a downward swipe is over the
                        // stats — which used to swallow it. Nothing inside consumes the drag, so
                        // it arrives here wherever it starts.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(japaId, current.malaSize) {
                                    detectVerticalDragGestures(
                                        onDragStart = { dragging = true },
                                        onDragEnd = { dragging = false },
                                        onDragCancel = { dragging = false },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        var value = residual + dragAmount
                                        // Pulling the strand down counts japa on; pushing it back
                                        // up takes them off again.
                                        while (value >= stepPx) {
                                            value -= stepPx
                                            latestStep(1)
                                        }
                                        while (value <= -stepPx) {
                                            value += stepPx
                                            latestStep(-1)
                                        }
                                        residual = value
                                    }
                                }
                        ) {
                            MalaStrand(
                                count = displayCount,
                                malaSize = current.malaSize,
                                dragOffset = residual / stepPx,
                                accent = accent,
                                onDark = false,
                                modifier = Modifier.fillMaxSize(),
                            )

                            SwipeLegend(Modifier.align(Alignment.CenterStart))

                            CounterPanel(
                                japa = shown,
                                today = sessionDayCount ?: 0L,
                                dayLabel = dayLabel(sessionDate),
                                accent = accent,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    if (adjusting) {
        AdjustCountDialog(
            japa = current,
            currentCount = displayCount,
            onDismiss = { adjusting = false },
            onApply = { delta -> latestStep(delta) },
        )
    }

    editing?.let { japaToEdit ->
        EditJapaSheet(
            japa = japaToEdit,
            onDismiss = { editing = null },
            onSave = { draft, picked ->
                viewModel.save(draft, picked)
                editing = null
            },
        )
    }

    confirmDelete?.let { japaToDelete ->
        DeleteConfirmDialog(
            japa = japaToDelete,
            onDismiss = { confirmDelete = null },
            onConfirm = {
                pending = 0
                viewModel.delete(japaToDelete)
                confirmDelete = null
            },
        )
    }
}

/** "Today", or the date itself once a session has run past midnight into the next day. */
private fun dayLabel(sessionDate: String): String =
    if (sessionDate == today()) "Today" else sessionDate

/** Asked on entering a japa, so a session can be counted whichever way suits the moment. */
@Composable
private fun ModeChooser(onPick: (CountMode) -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("How do you want to count?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Just for this session — switch any time from the menu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                ModeOption(
                    title = "Bead strand",
                    body = "Swipe down the mala to count, up to undo.",
                    onClick = { onPick(CountMode.Strand) },
                )
                Spacer(Modifier.height(12.dp))
                ModeOption(
                    title = "Tap buttons",
                    body = "One large button to count, a smaller one above it to undo.",
                    onClick = { onPick(CountMode.Tap) },
                )
                Spacer(Modifier.height(12.dp))
                ModeOption(
                    title = "Timer",
                    body = "Counts by itself at a set interval, ringing each time, " +
                        "and closes with gongs.",
                    onClick = { onPick(CountMode.Timer) },
                )

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Back") }
                }
            }
        }
    }
}

@Composable
private fun ModeOption(title: String, body: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Which way is which, printed beside the strand rather than left to be discovered. */
@Composable
private fun SwipeLegend(modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 14.dp)) {
        LegendRow(Icons.Default.ArrowUpward, "Swipe up", "removes a japa")
        Spacer(Modifier.height(22.dp))
        LegendRow(Icons.Default.ArrowDownward, "Swipe down", "counts a japa")
    }
}

@Composable
private fun LegendRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(icon, contentDescription = null)
        }
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Tap mode. The whole screen counts, wherever the thumb lands, except the undo button.
 */
@Composable
private fun TapCounter(
    japa: Japa,
    today: Long,
    dayLabel: String,
    accent: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // The whole screen counts. The undo button sits on top and takes its own taps, so it
            // is the one place a press does not add a japa.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onIncrement,
            )
            .padding(horizontal = 8.dp),
    ) {
        // A landscape phone leaves barely 300dp of height. Without the floor this arithmetic
        // goes negative and Modifier.size throws; without the shrinking undo button the circle
        // is squeezed to nothing.
        val undoSize = if (maxHeight < 520.dp) 84.dp else 124.dp
        val diameter = (minOf(maxWidth - 8.dp, maxHeight - undoSize - 32.dp) * 0.9f)
            .coerceIn(150.dp, 440.dp)
        val countStyle = if (diameter < 260.dp) MaterialTheme.typography.headlineMedium
        else MaterialTheme.typography.displayLarge
        val roomForExtras = diameter >= 220.dp

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BUTTON_ALPHA),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(undoSize)
                .clickable(onClick = onDecrement),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Remove one japa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            // Progress through the mala currently in hand, drawn around the rim.
            Canvas(Modifier.size(diameter)) {
                val stroke = 10.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = accent.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (japa.malaProgress > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * japa.malaProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = BUTTON_ALPHA),
                modifier = Modifier.size(diameter - 18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = japa.count.grouped(),
                        style = countStyle,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Text(
                        text = malaLine(japa),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    if (roomForExtras) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$dayLabel ${today.grouped()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    if (japa.hasTarget && roomForExtras) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = targetLine(japa),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Timer mode. Counts on its own at a fixed interval, ringing each time, and finishes with temple
 * gongs after the chosen number of malas.
 */
@Composable
private fun TimerCounter(
    japa: Japa,
    today: Long,
    dayLabel: String,
    accent: Color,
    interval: Int,
    endMalas: Int,
    running: Boolean,
    counted: Long,
    target: Long,
    onIntervalChange: (Int) -> Unit,
    onEndMalasChange: (Int) -> Unit,
    onToggleRun: () -> Unit,
    onResetRun: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = japa.count.grouped(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = malaLine(japa),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$dayLabel ${today.grouped()}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (japa.hasTarget) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = targetLine(japa),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))

        val remaining = (target - counted).coerceAtLeast(0)
        Text(
            text = "${counted.grouped()} / ${target.grouped()} this session" +
                if (remaining > 0) " · ${remaining.grouped()} to go" else " · complete",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(if (target > 0) (counted.toFloat() / target).coerceIn(0f, 1f) else 0f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerChip(
                title = "Interval",
                display = "${interval}s",
                options = listOf(1, 2, 3, 5, 8, 10, 15, 30, 60),
                format = { "$it seconds" },
                customLabel = "Interval in seconds",
                onPick = onIntervalChange,
                modifier = Modifier.weight(1f),
            )
            PickerChip(
                title = "Ends after",
                display = if (endMalas == 1) "1 mala" else "$endMalas malas",
                options = listOf(1, 2, 3, 5, 7, 11, 16, 108),
                format = { if (it == 1) "1 mala" else "$it malas" },
                customLabel = "Number of malas",
                onPick = onEndMalasChange,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onToggleRun) {
                Icon(
                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (running) "Pause" else if (counted > 0) "Resume" else "Start")
            }
            OutlinedButton(onClick = onResetRun, enabled = counted > 0 || running) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun PickerChip(
    title: String,
    display: String,
    options: List<Int>,
    format: (Int) -> String,
    customLabel: String,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }

    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BUTTON_ALPHA),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = display, style = MaterialTheme.typography.titleMedium)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(format(option)) },
                    onClick = { open = false; onPick(option) },
                )
            }
            DropdownMenuItem(
                text = { Text("Custom…") },
                onClick = { open = false; custom = true },
            )
        }
    }

    if (custom) {
        NumberDialog(
            label = customLabel,
            onDismiss = { custom = false },
            onConfirm = { custom = false; onPick(it) },
        )
    }
}

@Composable
private fun NumberDialog(label: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    val parsed = value.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit).take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed > 0,
                onClick = { onConfirm(parsed ?: 1) },
            ) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun malaLine(japa: Japa): String =
    "japa ${japa.beadsIntoMala.grouped()} / ${japa.malaSize.grouped()}" +
        " · ${japa.malasCompleted.grouped()} " +
        if (japa.malasCompleted == 1L) "mala" else "malas"

private fun targetLine(japa: Japa): String =
    if (japa.targetReached) {
        "Lifetime target of ${japa.lifetimeTarget.grouped()} reached"
    } else {
        "${(japa.targetProgress * 100).toInt()}% of ${japa.lifetimeTarget.grouped()} · " +
            "${japa.targetRemaining.grouped()} to go"
    }

@Composable
private fun CounterPanel(
    japa: Japa,
    today: Long,
    dayLabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        // Translucent so the strand stays visible behind it — japa pass under the stats rather
        // than stopping at them.
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = japa.count.grouped(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = malaLine(japa),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$dayLabel ${today.grouped()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (japa.hasTarget) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(japa.targetProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = targetLine(japa),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
