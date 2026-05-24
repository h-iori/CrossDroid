package com.ioristudios.crossdroid.data

import java.util.UUID

enum class FileType {
    ALL, VIDEO, IMAGE, MUSIC, DOCUMENT
}

enum class FileKind {
    FILE, FOLDER
}

data class FileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val type: FileType,
    val detail: String = "",
    val kind: FileKind = FileKind.FILE,
    val path: String = "",
    val childrenCount: Int = 0,
    val lastModified: String = ""
)

data class DeviceNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val status: String,
    val signalStrength: Int, // 1 to 5
    val osType: String, // "android" or "windows"
    val lastSeen: String
)

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val size: String,
    val date: String,
    val isIncoming: Boolean,
    val isSuccess: Boolean,
    val deviceName: String
)

object MockData {
    val filesList = listOf(
        FileItem(name = "Cyberpunk_City_4K.mp4", size = "1.2 GB", type = FileType.VIDEO, detail = "4K HDR | H.264"),
        FileItem(name = "Neon_Vibes_Chill.mp3", size = "12.4 MB", type = FileType.MUSIC, detail = "320kbps | Stereo"),
        FileItem(name = "IORI_Studios_Logo.png", size = "4.2 MB", type = FileType.IMAGE, detail = "PNG | 2048x2048"),
        FileItem(name = "system_config_backup.yaml", size = "48 KB", type = FileType.DOCUMENT, detail = "YAML Configuration"),
        FileItem(name = "Synthesizer_Performance.mp4", size = "342 MB", type = FileType.VIDEO, detail = "1080p | 60fps"),
        FileItem(name = "Tokyo_Midnight_Run.jpg", size = "8.7 MB", type = FileType.IMAGE, detail = "RAW Image"),
        FileItem(name = "Hyperpop_Mix_2026.wav", size = "64.2 MB", type = FileType.MUSIC, detail = "Uncompressed WAV"),
        FileItem(name = "enterprise_contract_v2.pdf", size = "2.1 MB", type = FileType.DOCUMENT, detail = "PDF Document"),
        FileItem(name = "CrossDroid_Source_Code.zip", size = "14.8 MB", type = FileType.DOCUMENT, detail = "Archive file"),
        FileItem(name = "RetroWave_Grid_HD.jpg", size = "5.1 MB", type = FileType.IMAGE, detail = "JPEG | 1920x1080")
    )

    val deviceNodes = listOf(
        DeviceNode(name = "STUDIO-WORKSTATION", status = "Connected", signalStrength = 5, osType = "windows", lastSeen = "Active now"),
        DeviceNode(name = "Aoki's Galaxy S24", status = "Paired", signalStrength = 4, osType = "android", lastSeen = "2 mins ago"),
        DeviceNode(name = "Mainframe-Central", status = "Nearby", signalStrength = 3, osType = "windows", lastSeen = "Just now"),
        DeviceNode(name = "Pixel 8 Pro - Testbed", status = "Paired", signalStrength = 5, osType = "android", lastSeen = "1 hour ago"),
        DeviceNode(name = "Dev-Book-Pro", status = "Nearby", signalStrength = 2, osType = "windows", lastSeen = "5 mins ago")
    )

    val historyItems = listOf(
        HistoryItem(fileName = "Tokyo_Midnight_Run.jpg", size = "8.7 MB", date = "May 23, 17:15", isIncoming = false, isSuccess = true, deviceName = "Aoki's Galaxy S24"),
        HistoryItem(fileName = "Neon_Vibes_Chill.mp3", size = "12.4 MB", date = "May 22, 14:02", isIncoming = true, isSuccess = true, deviceName = "STUDIO-WORKSTATION"),
        HistoryItem(fileName = "huge_uncompressed_log.txt", size = "840 MB", date = "May 21, 09:44", isIncoming = false, isSuccess = false, deviceName = "Dev-Book-Pro"),
        HistoryItem(fileName = "enterprise_contract_v2.pdf", size = "2.1 MB", date = "May 19, 11:20", isIncoming = true, isSuccess = true, deviceName = "Mainframe-Central")
    )
}
