package com.suw1labs.worktracker.platform

/**
 * Platform-specific "save & share" for generated export files:
 * share sheet on Android/iOS, save-file dialog on desktop.
 */
interface FileExporter {
    /** Opens the system share sheet (e-mail, Teams, chat, AirDrop, …). */
    suspend fun shareText(content: String, filename: String, mimeType: String, title: String)

    /** Lets the user pick a location and saves the file to local storage / Files / Drive. */
    suspend fun saveText(content: String, filename: String, mimeType: String)
}

enum class ExportAction { SAVE, SHARE }

object NoOpFileExporter : FileExporter {
    override suspend fun shareText(content: String, filename: String, mimeType: String, title: String) = Unit
    override suspend fun saveText(content: String, filename: String, mimeType: String) = Unit
}
