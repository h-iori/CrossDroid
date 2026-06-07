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
    val signalStrength: Int,
    val osType: String,
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
