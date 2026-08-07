package com.starborn.mantracounter.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starborn.mantracounter.data.BackgroundStore
import com.starborn.mantracounter.data.BackupReader
import com.starborn.mantracounter.data.BackupWriter
import com.starborn.mantracounter.data.JapaWithLog
import com.starborn.mantracounter.data.DailyCount
import com.starborn.mantracounter.data.JapaSort
import com.starborn.mantracounter.data.applySort
import com.starborn.mantracounter.data.currentWeekDays
import com.starborn.mantracounter.data.today
import com.starborn.mantracounter.data.yesterday
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.JapaDatabase
import com.starborn.mantracounter.data.JapaRepository
import com.starborn.mantracounter.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JapaViewModel(app: Application) : AndroidViewModel(app) {

    private val database = JapaDatabase.get(app)
    private val repository = JapaRepository(database.japaDao(), database.dailyCountDao())
    private val settings = SettingsStore(app)

    /** One query drives every list in the app, so search carries across home and archive. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    private val _events = MutableSharedFlow<JapaEvent>(extraBufferCapacity = 8)
    val events = _events

    val sort: StateFlow<JapaSort> = settings.sort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JapaSort.DEFAULT)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeJapas: StateFlow<List<Japa>> =
        combine(_searchQuery.flatMapLatest { repository.active(it) }, settings.sort) { list, order ->
            list.applySort(order)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val timerInterval: StateFlow<Int> = settings.timerInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_INTERVAL)

    val timerEndMalas: StateFlow<Int> = settings.timerEndMalas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_END_MALAS)

    fun setTimerInterval(seconds: Int) {
        viewModelScope.launch { settings.setTimerInterval(seconds) }
    }

    fun setTimerEndMalas(malas: Int) {
        viewModelScope.launch { settings.setTimerEndMalas(malas) }
    }

    fun setSort(order: JapaSort) {
        viewModelScope.launch { settings.setSort(order) }
    }

    fun toggleFavourite(japa: Japa) {
        viewModelScope.launch { repository.setFavourite(japa.id, !japa.favourite) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val archivedJapas: StateFlow<List<Japa>> = _searchQuery
        .flatMapLatest { repository.archived(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedCount: StateFlow<Int> = repository.archivedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val listBackground: StateFlow<String?> = settings.listBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val listBackgroundAlpha: StateFlow<Float> = settings.listBackgroundAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_ALPHA)

    val vibrationEnabled: StateFlow<Boolean> = settings.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val bellEnabled: StateFlow<Boolean> = settings.bellEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setVibrationEnabled(enabled) }
    }

    fun setBellEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setBellEnabled(enabled) }
    }

    /**
     * Which two dates the cards are reporting. Held in state rather than read at query time so a
     * session left open across midnight can be corrected by [refreshDayWindow] rather than
     * silently labelling yesterday's figures as today's.
     */
    private val _dayWindow = MutableStateFlow(today() to yesterday())

    fun refreshDayWindow() {
        val current = today() to yesterday()
        if (_dayWindow.value != current) _dayWindow.value = current
    }

    /** japaId to (today, yesterday). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentCounts: StateFlow<Map<Long, Pair<Long, Long>>> = _dayWindow
        .flatMapLatest { (todayKey, yesterdayKey) ->
            repository.dates(todayKey, yesterdayKey).map { rows ->
                rows.groupBy { it.japaId }.mapValues { (_, days) ->
                    val t = days.firstOrNull { it.date == todayKey }?.count ?: 0L
                    val y = days.firstOrNull { it.date == yesterdayKey }?.count ?: 0L
                    t to y
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * This week Monday to Sunday, one entry per day whether or not anything was counted — a chart
     * with days missing would misread as a shorter week rather than an empty day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekTotals: StateFlow<List<Pair<String, Long>>> = _dayWindow
        .flatMapLatest {
            val week = currentWeekDays()
            repository.dailyTotals(week.first(), week.last()).map { rows ->
                val byDate = rows.associate { it.date to it.total }
                week.map { date -> date to (byDate[date] ?: 0L) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every japa, active and archived, for the stats screen. */
    val allJapas: StateFlow<List<Japa>> = repository.allJapas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTime(japaId: Long, millis: Long) {
        if (millis <= 0) return
        viewModelScope.launch {
            withContext(NonCancellable) { repository.addTime(japaId, millis) }
        }
    }

    fun japa(id: Long): StateFlow<Japa?> = repository.japa(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dayCount(id: Long, date: String): StateFlow<Long?> = repository.dayCount(id, date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun history(id: Long): StateFlow<List<DailyCount>> = repository.history(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The date a counting session is logged against — see [commitBeads]. */
    fun startSessionDate(): String = today()

    fun setDayCount(japaId: Long, date: String, count: Long) {
        viewModelScope.launch { repository.setDayCount(japaId, date, count) }
    }

    fun deleteDay(japaId: Long, date: String) {
        viewModelScope.launch { repository.deleteDay(japaId, date) }
    }

    fun moveDay(japaId: Long, from: String, to: String) {
        viewModelScope.launch { repository.moveDay(japaId, from, to) }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setSearchOpen(open: Boolean) {
        _searchOpen.value = open
        if (!open) _searchQuery.value = ""
    }

    /**
     * Persists beads swiped on the counter screen. The screen batches them, so this is called
     * a few times a second at most rather than once per bead. [NonCancellable] because the last
     * flush happens as the screen is leaving and must not be dropped with it.
     */
    fun commitBeads(japaId: Long, delta: Long, date: String = today()) {
        if (delta == 0L) return
        viewModelScope.launch {
            withContext(NonCancellable) { repository.commitBeads(japaId, delta, date) }
        }
    }

    /**
     * Manual correction from the Adjust count dialog. Goes through the same write path as swiped
     * beads, so the daily log stays in step. Replaces the old reset, which could wipe a lifetime
     * count with one mis-tap.
     */
    fun adjustCount(japaId: Long, delta: Long) = commitBeads(japaId, delta)

    fun setArchived(japa: Japa, archived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(japa.id, archived)
            _events.tryEmit(
                if (archived) JapaEvent.Archived(japa) else JapaEvent.Unarchived(japa.name)
            )
        }
    }

    fun delete(japa: Japa) {
        viewModelScope.launch { repository.delete(japa) }
    }

    /**
     * Saves an add or an edit. [newBackground] is a freshly picked photo that still needs
     * importing; `draft.backgroundUri` carries an already-imported path through unchanged.
     */
    fun save(draft: Japa, newBackground: Uri?) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var toSave = draft

            if (newBackground != null) {
                val imported = BackgroundStore.import(context, newBackground)
                if (imported == null) {
                    // Better a japa with no background than a silent failure the user has to
                    // discover for themselves.
                    _events.tryEmit(JapaEvent.Message("Couldn't read that image — japa saved without it"))
                } else {
                    val previous = repository.get(draft.id)?.backgroundUri
                    if (previous != null && previous != imported) BackgroundStore.remove(previous)
                    toSave = toSave.copy(backgroundUri = imported)
                }
            } else if (draft.id != 0L && draft.backgroundUri == null) {
                // Background was cleared in the editor — drop the file too.
                BackgroundStore.remove(repository.get(draft.id)?.backgroundUri)
            }

            if (toSave.id == 0L) repository.create(toSave) else repository.save(toSave)
        }
    }

    fun setListBackground(source: Uri?) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (source == null) {
                BackgroundStore.remove(listBackground.value)
                settings.setListBackground(null)
                return@launch
            }
            val imported = BackgroundStore.import(context, source)
            if (imported == null) {
                _events.tryEmit(JapaEvent.Message("Couldn't read that image"))
            } else {
                BackgroundStore.remove(listBackground.value)
                settings.setListBackground(imported)
            }
        }
    }

    fun setListBackgroundAlpha(alpha: Float) {
        viewModelScope.launch { settings.setListBackgroundAlpha(alpha) }
    }

    /**
     * The text of the last export, kept so Share can hand over a copy from our own cache.
     * Forwarding the Storage Access Framework URI directly is not dependable — whether a
     * document provider honours a re-granted permission varies by provider and by OEM — so the
     * share goes through a FileProvider file this app definitely owns.
     */
    private var lastExportText: String? = null

    private val _shareUri = MutableStateFlow<Uri?>(null)
    val shareUri: StateFlow<Uri?> = _shareUri.asStateFlow()

    fun prepareShare() {
        val text = lastExportText ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val file = File(dir, defaultBackupFileName())
                    file.writeText(text)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                }.getOrNull()
            }
            if (uri == null) {
                _events.tryEmit(JapaEvent.Message("Couldn't prepare the file to share"))
            } else {
                _shareUri.value = uri
            }
        }
    }

    fun clearShare() {
        _shareUri.value = null
    }

    private val _lastExport = MutableStateFlow<Uri?>(null)

    /** The file just written, offered for sharing until dismissed. */
    val lastExport: StateFlow<Uri?> = _lastExport.asStateFlow()

    fun clearLastExport() {
        _lastExport.value = null
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val snapshot = repository.snapshot()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = BackupWriter.render(snapshot, System.currentTimeMillis())
                    context.contentResolver.openOutputStream(destination)?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("could not open the chosen file")
                    lastExportText = text
                    text.lineSequence().count()
                }
            }
            result.fold(
                onSuccess = { _lastExport.value = destination },
                onFailure = {
                    _events.tryEmit(JapaEvent.Message("Backup failed: ${it.message}"))
                },
            )
        }
    }

    private val _restorePreview = MutableStateFlow<RestorePreview?>(null)
    val restorePreview: StateFlow<RestorePreview?> = _restorePreview.asStateFlow()

    /** Reads and parses a backup, but changes nothing until [confirmRestore]. */
    fun previewRestore(source: Uri) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val text = getApplication<Application>().contentResolver
                        .openInputStream(source)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("could not open that file")
                    BackupReader.parse(text)
                }
            }
            parsed.fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        _events.tryEmit(
                            JapaEvent.Message("No japas found in that file — is it a Mantra Counter backup?")
                        )
                    } else {
                        _restorePreview.value = RestorePreview(entries)
                    }
                },
                onFailure = { _events.tryEmit(JapaEvent.Message("Couldn't read that file: ${it.message}")) },
            )
        }
    }

    fun cancelRestore() {
        _restorePreview.value = null
    }

    fun confirmRestore() {
        val preview = _restorePreview.value ?: return
        _restorePreview.value = null
        viewModelScope.launch {
            val result = repository.restore(preview.entries)
            _events.tryEmit(
                JapaEvent.Message(
                    buildString {
                        append("Restored ${result.added} japa")
                        if (result.added != 1) append("s")
                        append(" and ${result.days} days")
                        if (result.skipped > 0) append(" · ${result.skipped} already existed")
                    }
                )
            )
        }
    }

    fun defaultBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        return "mantra-counter-backup-$stamp.txt"
    }
}

/** A parsed backup waiting for the user to confirm. */
data class RestorePreview(val entries: List<JapaWithLog>) {
    val japaCount: Int get() = entries.size
    val dayCount: Int get() = entries.sumOf { it.log.size }
    val totalJapa: Long get() = entries.sumOf { it.japa.count }
    val names: List<String> get() = entries.map { it.japa.name }
}

sealed interface JapaEvent {
    data class MalaComplete(val malaNumber: Long) : JapaEvent
    data class TargetReached(val name: String) : JapaEvent
    data class Archived(val japa: Japa) : JapaEvent
    data class Unarchived(val name: String) : JapaEvent
    data class Message(val text: String) : JapaEvent
}
