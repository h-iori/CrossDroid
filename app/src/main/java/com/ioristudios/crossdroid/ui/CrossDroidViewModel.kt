package com.ioristudios.crossdroid.ui

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.crossdroid.data.DeviceNode
import com.ioristudios.crossdroid.data.FileItem
import com.ioristudios.crossdroid.data.FileKind
import com.ioristudios.crossdroid.data.HistoryItem
import com.ioristudios.crossdroid.data.MockData
import com.ioristudios.crossdroid.data.FileType
import com.ioristudios.crossdroid.ui.theme.HapticHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class Screen {
    HOME, DEVICES, HISTORY, HISTORY_DETAIL, SEND, QR_SCAN, ENTER_CODE, RADAR, TRANSFER, RECEIVE, ABOUT
}

data class TransferBubble(
    val id: String = UUID.randomUUID().toString(),
    val file: FileItem,
    val progress: Float,
    val speed: String,
    val status: String, // "Pending", "Sending", "Receiving", "Paused", "Failed", "Completed"
    val isIncoming: Boolean
)

data class HistorySession(
    val id: String,
    val deviceName: String,
    val date: String,
    val isIncoming: Boolean,
    val isSuccess: Boolean,
    val records: List<HistoryItem>
)

class CrossDroidViewModel : ViewModel() {

    // Navigation State
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _navigationHistory = MutableStateFlow<List<Screen>>(listOf(Screen.HOME))

    // File Selection State (Send Flow)
    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles.asStateFlow()

    private val _activeFilter = MutableStateFlow(FileType.ALL)
    val activeFilter: StateFlow<FileType> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMode = MutableStateFlow(false)
    val searchMode: StateFlow<Boolean> = _searchMode.asStateFlow()

    // Internal file manager state (Send Flow)
    private val storageRoot: File = Environment.getExternalStorageDirectory()

    private val _currentDirectoryPath = MutableStateFlow(storageRoot.absolutePath)
    val currentDirectoryPath: StateFlow<String> = _currentDirectoryPath.asStateFlow()

    private val _fileManagerEntries = MutableStateFlow<List<FileItem>>(emptyList())
    val fileManagerEntries: StateFlow<List<FileItem>> = _fileManagerEntries.asStateFlow()

    private val _isFileManagerLoading = MutableStateFlow(false)
    val isFileManagerLoading: StateFlow<Boolean> = _isFileManagerLoading.asStateFlow()

    private val _fileManagerError = MutableStateFlow<String?>(null)
    val fileManagerError: StateFlow<String?> = _fileManagerError.asStateFlow()

    // Enter Code PIN Input
    private val _pinCode = MutableStateFlow("")
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Receive Screen discoverable simulation
    private val _showReceivePopup = MutableStateFlow(false)
    val showReceivePopup: StateFlow<Boolean> = _showReceivePopup.asStateFlow()

    // Transfer Activity States
    private val _transferDevice = MutableStateFlow<DeviceNode?>(null)
    val transferDevice: StateFlow<DeviceNode?> = _transferDevice.asStateFlow()

    private val _transferBubbles = MutableStateFlow<List<TransferBubble>>(emptyList())
    val transferBubbles: StateFlow<List<TransferBubble>> = _transferBubbles.asStateFlow()

    private val _isTransferActive = MutableStateFlow(false)
    val isTransferActive: StateFlow<Boolean> = _isTransferActive.asStateFlow()

    private val _isTransferPaused = MutableStateFlow(false)
    val isTransferPaused: StateFlow<Boolean> = _isTransferPaused.asStateFlow()

    private val _isTransferComplete = MutableStateFlow(false)
    val isTransferComplete: StateFlow<Boolean> = _isTransferComplete.asStateFlow()

    // Dynamic History list that appends simulated finishes
    private val _historyRecords = MutableStateFlow(MockData.historyItems)
    val historyRecords: StateFlow<List<HistoryItem>> = _historyRecords.asStateFlow()

    private val _selectedHistorySession = MutableStateFlow<HistorySession?>(null)
    val selectedHistorySession: StateFlow<HistorySession?> = _selectedHistorySession.asStateFlow()

    // Sidebar State
    private val _isSidebarVisible = MutableStateFlow(false)
    val isSidebarVisible: StateFlow<Boolean> = _isSidebarVisible.asStateFlow()

    fun setSidebarVisible(visible: Boolean) {
        _isSidebarVisible.value = visible
    }

    private var transferJob: Job? = null
    private var receiveSimulationJob: Job? = null

    fun navigateTo(screen: Screen, context: Context? = null) {
        val current = _currentScreen.value
        if (current != screen) {
            val history = _navigationHistory.value.toMutableList()
            if (screen == Screen.HOME) {
                history.clear()
                history.add(Screen.HOME)
            } else if (screen in listOf(Screen.DEVICES, Screen.HISTORY)) {
                history.clear()
                history.add(Screen.HOME)
                history.add(screen)
            } else {
                history.add(screen)
            }
            _navigationHistory.value = history
            _currentScreen.value = screen
        }
        context?.let { HapticHelper.triggerLight(it) }
        
        // Reset code errors when switching pages
        if (screen != Screen.ENTER_CODE) {
            _pinCode.value = ""
            _pinError.value = null
        }
        
        // Start or cancel receiver simulations based on screen
        if (screen == Screen.RECEIVE) {
            startReceiveSimulation(context)
        } else {
            cancelReceiveSimulation()
        }
    }

    fun historySessions(): List<HistorySession> = buildHistorySessions(_historyRecords.value)

    fun openHistorySession(session: HistorySession, context: Context) {
        _selectedHistorySession.value = session
        navigateTo(Screen.HISTORY_DETAIL, context)
    }

    fun openHistorySession(record: HistoryItem, context: Context) {
        val session = buildHistorySessions(_historyRecords.value).firstOrNull { candidate ->
            candidate.records.any { it.id == record.id }
        } ?: HistorySession(
            id = historySessionId(record.deviceName, record.date, record.isIncoming, record.isSuccess),
            deviceName = record.deviceName,
            date = record.date,
            isIncoming = record.isIncoming,
            isSuccess = record.isSuccess,
            records = listOf(record)
        )
        openHistorySession(session, context)
    }

    fun navigateBack(context: Context, triggerHaptic: Boolean = true) {
        val history = _navigationHistory.value.toMutableList()
        if (history.size > 1) {
            val current = history.last()
            if (current == Screen.TRANSFER && _isTransferActive.value) {
                cancelTransfer(context)
                return
            }
            if (current == Screen.SEND && _activeFilter.value == FileType.ALL) {
                if (goUpDirectory(if (triggerHaptic) context else null)) {
                    return
                }
            }
            history.removeAt(history.lastIndex)
            val prevScreen = history.last()
            _navigationHistory.value = history
            _currentScreen.value = prevScreen
            if (triggerHaptic) {
                HapticHelper.triggerLight(context)
            }
            
            // Clean/simulation logics
            if (prevScreen != Screen.ENTER_CODE) {
                _pinCode.value = ""
                _pinError.value = null
            }
            if (prevScreen == Screen.RECEIVE) {
                startReceiveSimulation(context)
            } else {
                cancelReceiveSimulation()
            }
        }
    }

    // --- Send Selection Logics ---
    fun toggleFileSelected(file: FileItem, context: Context) {
        val current = _selectedFiles.value.toMutableSet()
        val existing = current.firstOrNull { it.id == file.id }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
        HapticHelper.triggerLight(context)
    }

    fun clearSelectedFiles(context: Context? = null) {
        _selectedFiles.value = emptySet()
        context?.let { HapticHelper.triggerLight(it) }
    }

    fun setFilter(filter: FileType, context: Context) {
        _activeFilter.value = filter
        HapticHelper.triggerLight(context)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchMode(context: Context) {
        _searchMode.value = !_searchMode.value
        if (!_searchMode.value) _searchQuery.value = ""
        HapticHelper.triggerLight(context)
    }

    fun hasAllFilesAccess(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    fun openAllFilesAccessSettings(context: Context) {
        val intent = android.content.Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        context.startActivity(intent)
        HapticHelper.triggerMedium(context)
    }

    fun loadCurrentDirectory() {
        loadDirectory(_currentDirectoryPath.value)
    }

    fun loadStorageRoot() {
        loadDirectory(storageRoot.absolutePath)
    }

    fun openDirectory(path: String, context: Context? = null) {
        loadDirectory(path)
        context?.let { HapticHelper.triggerLight(it) }
    }

    fun openDirectory(file: FileItem, context: Context? = null) {
        if (file.kind == FileKind.FOLDER && file.path.isNotBlank()) {
            openDirectory(file.path, context)
        }
    }

    fun goUpDirectory(context: Context? = null): Boolean {
        val current = File(_currentDirectoryPath.value)
        val rootPath = storageRoot.absolutePath
        val parent = current.parentFile
        if (current.absolutePath == rootPath || parent == null || !parent.absolutePath.startsWith(rootPath)) {
            return false
        }
        loadDirectory(parent.absolutePath)
        context?.let { HapticHelper.triggerLight(it) }
        return true
    }

    fun directoryBreadcrumbs(): List<Pair<String, String>> {
        val rootPath = storageRoot.absolutePath
        val current = File(_currentDirectoryPath.value)
        val breadcrumbs = mutableListOf("Internal storage" to rootPath)
        val relative = current.absolutePath.removePrefix(rootPath).trim(File.separatorChar)
        if (relative.isNotBlank()) {
            var path = rootPath
            relative.split(File.separatorChar).filter { it.isNotBlank() }.forEach { part ->
                path = File(path, part).absolutePath
                breadcrumbs.add(part to path)
            }
        }
        return breadcrumbs
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isFileManagerLoading.value = true
            _fileManagerError.value = null

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val directory = File(path)
                    if (!directory.exists()) {
                        throw IllegalArgumentException("Folder no longer exists.")
                    }
                    if (!directory.isDirectory) {
                        throw IllegalArgumentException("This location is not a folder.")
                    }
                    val entries = directory
                        .listFiles()
                        ?.filter { it.canRead() && !it.isHidden }
                        ?.map { it.toSendItem() }
                        ?.sortedWith(
                            compareByDescending<FileItem> { it.kind == FileKind.FOLDER }
                                .thenBy { it.name.lowercase(Locale.getDefault()) }
                        )
                        ?: emptyList()
                    directory.absolutePath to entries
                }
            }

            result
                .onSuccess { (resolvedPath, entries) ->
                    _currentDirectoryPath.value = resolvedPath
                    _fileManagerEntries.value = entries
                }
                .onFailure { throwable ->
                    _fileManagerEntries.value = emptyList()
                    _fileManagerError.value = throwable.message ?: "Unable to open this folder."
                }

            _isFileManagerLoading.value = false
        }
    }

    // --- PIN Entry Logics ---
    fun appendPinChar(char: Char, context: Context) {
        if (_pinCode.value.length < 4) {
            _pinCode.value += char
            _pinError.value = null
            HapticHelper.triggerLight(context)
        }
    }

    fun deletePinLast(context: Context) {
        if (_pinCode.value.isNotEmpty()) {
            _pinCode.value = _pinCode.value.dropLast(1)
            _pinError.value = null
            HapticHelper.triggerLight(context)
        }
    }

    fun verifyPinCode(context: Context) {
        if (_pinCode.value == "1234") {
            HapticHelper.triggerSuccess(context)
            _pinError.value = null
            // Trigger transfer with chosen files
            val defaultDevice = MockData.deviceNodes.first { it.osType == "android" }
            startTransferFlow(defaultDevice, _selectedFiles.value.toList(), isIncoming = false, context = context)
        } else {
            HapticHelper.triggerError(context)
            _pinError.value = "Invalid connection code"
        }
    }

    // --- Receive Page Simulation ---
    private fun startReceiveSimulation(context: Context?) {
        cancelReceiveSimulation()
        receiveSimulationJob = viewModelScope.launch {
            // Wait 4 seconds, then show confirmation popup
            delay(4000)
            context?.let { HapticHelper.triggerStrong(it) }
            _showReceivePopup.value = true
        }
    }

    private fun cancelReceiveSimulation() {
        receiveSimulationJob?.cancel()
        _showReceivePopup.value = false
    }

    fun acceptIncomingTransfer(context: Context) {
        _showReceivePopup.value = false
        val incomingDevice = MockData.deviceNodes.first { it.name == "STUDIO-WORKSTATION" }
        // Receive 2 mock files
        val incomingFiles = listOf(
            MockData.filesList[1], // Neon_Vibes_Chill.mp3
            MockData.filesList[2]  // IORI_Studios_Logo.png
        )
        HapticHelper.triggerSuccess(context)
        startTransferFlow(incomingDevice, incomingFiles, isIncoming = true, context = context)
    }

    fun declineIncomingTransfer(context: Context) {
        _showReceivePopup.value = false
        HapticHelper.triggerError(context)
        navigateTo(Screen.HOME, context)
    }

    // --- Active Transfer Simulation ---
    fun startTransferFlow(device: DeviceNode, files: List<FileItem>, isIncoming: Boolean, context: Context) {
        transferJob?.cancel()
        _transferDevice.value = device
        _isTransferActive.value = true
        _isTransferComplete.value = false
        _isTransferPaused.value = false

        val initialBubbles = files.map { file ->
            TransferBubble(
                file = file,
                progress = 0f,
                speed = "0 MB/s",
                status = "Pending",
                isIncoming = isIncoming
            )
        }
        _transferBubbles.value = initialBubbles
        navigateTo(Screen.TRANSFER, context)

        transferJob = viewModelScope.launch {
            val updatedBubbles = initialBubbles.toMutableList()
            for (i in updatedBubbles.indices) {
                // Simulate active sending/receiving
                updatedBubbles[i] = updatedBubbles[i].copy(status = if (isIncoming) "Receiving" else "Sending")
                _transferBubbles.value = updatedBubbles.toList()

                var currentProgress = 0f
                while (currentProgress < 1.0f) {
                    if (_isTransferPaused.value) {
                        delay(250)
                        continue
                    }
                    delay(120) // Fast progress increments
                    currentProgress += 0.08f + (Math.random() * 0.12).toFloat()
                    if (currentProgress >= 1.0f) {
                        currentProgress = 1.0f
                    }
                    val randomSpeed = (20 + (Math.random() * 35).toInt())
                    updatedBubbles[i] = updatedBubbles[i].copy(
                        progress = currentProgress,
                        speed = "$randomSpeed MB/s",
                        status = if (currentProgress >= 1.0f) "Completed" else (if (isIncoming) "Receiving" else "Sending")
                    )
                    _transferBubbles.value = updatedBubbles.toList()
                }
                
                // Finished one file: trigger quick selection pulse
                context.let { HapticHelper.triggerLight(it) }
                delay(200)
            }

            // Transfer completed successfully
            _isTransferActive.value = false
            _isTransferComplete.value = true
            context.let { HapticHelper.triggerSuccess(it) }

            // Log details in our active history records
            val dateStr = "May 23, 17:50" // Current mock date
            val historyRecordsCopy = _historyRecords.value.toMutableList()
            files.forEach { file ->
                historyRecordsCopy.add(
                    0, // add to top
                    HistoryItem(
                        fileName = file.name,
                        size = file.size,
                        date = dateStr,
                        isIncoming = isIncoming,
                        isSuccess = true,
                        deviceName = device.name
                    )
                )
            }
            _historyRecords.value = historyRecordsCopy
            
            // Clean selection
            _selectedFiles.value = emptySet()
        }
    }

    fun toggleTransferPause(context: Context) {
        _isTransferPaused.value = !_isTransferPaused.value
        val updated = _transferBubbles.value.map {
            if (it.status == "Sending" || it.status == "Receiving") {
                it.copy(status = "Paused")
            } else if (it.status == "Paused") {
                it.copy(status = if (it.isIncoming) "Receiving" else "Sending")
            } else {
                it
            }
        }
        _transferBubbles.value = updated
        HapticHelper.triggerMedium(context)
    }

    fun cancelTransfer(context: Context) {
        transferJob?.cancel()
        _isTransferActive.value = false
        _isTransferComplete.value = false
        _isTransferPaused.value = false
        
        // Mark remaining incomplete items as Failed in logs
        val incomplete = _transferBubbles.value.filter { it.progress < 1.0f }
        if (incomplete.isNotEmpty()) {
            val dateStr = "May 23, 17:50"
            val historyRecordsCopy = _historyRecords.value.toMutableList()
            incomplete.forEach { item ->
                historyRecordsCopy.add(
                    0,
                    HistoryItem(
                        fileName = item.file.name,
                        size = item.file.size,
                        date = dateStr,
                        isIncoming = item.isIncoming,
                        isSuccess = false,
                        deviceName = _transferDevice.value?.name ?: "Unknown"
                    )
                )
            }
            _historyRecords.value = historyRecordsCopy
        }
        
        HapticHelper.triggerError(context)
        navigateTo(Screen.HOME, context)
    }
}

private fun File.toSendItem(): FileItem {
    val kind = if (isDirectory) FileKind.FOLDER else FileKind.FILE
    val childCount = if (isDirectory) {
        listFiles()?.count { it.canRead() && !it.isHidden } ?: 0
    } else {
        0
    }

    return FileItem(
        id = absolutePath,
        name = name.ifBlank { absolutePath },
        size = if (isDirectory) "Folder" else length().toReadableSize(),
        type = if (isDirectory) FileType.DOCUMENT else inferFileType(name),
        detail = if (isDirectory) "Folder | $childCount items" else extensionLabel(name),
        kind = kind,
        path = absolutePath,
        childrenCount = childCount,
        lastModified = modifiedLabel(lastModified())
    )
}

private fun Long.toReadableSize(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toInt()} ${units[unitIndex]}"
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}

private fun inferFileType(fileName: String): FileType {
    return when (fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())) {
        "mp4", "mov", "mkv", "avi", "webm" -> FileType.VIDEO
        "png", "jpg", "jpeg", "webp", "gif", "heic" -> FileType.IMAGE
        "mp3", "wav", "aac", "flac", "m4a", "ogg" -> FileType.MUSIC
        else -> FileType.DOCUMENT
    }
}

private fun extensionLabel(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
    return if (extension.isBlank() || extension == fileName.uppercase(Locale.getDefault())) {
        "File"
    } else {
        "$extension file"
    }
}

private fun modifiedLabel(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}

fun buildHistorySessions(records: List<HistoryItem>): List<HistorySession> {
    return records
        .groupBy { historySessionId(it.deviceName, it.date, it.isIncoming, it.isSuccess) }
        .map { (id, groupedRecords) ->
            val first = groupedRecords.first()
            HistorySession(
                id = id,
                deviceName = first.deviceName,
                date = first.date,
                isIncoming = first.isIncoming,
                isSuccess = first.isSuccess,
                records = groupedRecords
            )
        }
}

private fun historySessionId(
    deviceName: String,
    date: String,
    isIncoming: Boolean,
    isSuccess: Boolean
): String {
    return listOf(deviceName, date, isIncoming.toString(), isSuccess.toString()).joinToString("|")
}
