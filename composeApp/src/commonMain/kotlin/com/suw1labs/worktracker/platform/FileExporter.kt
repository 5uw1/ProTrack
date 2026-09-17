package com.suw1labs.worktracker.platform

/**
 * Platform-specific file exchange for generated files (CSV reports, JSON backups):
 * share sheet on Android/iOS, save/open dialogs on desktop.
 */
interface FileExporter {
    /** Opens the system share sheet (e-mail, Teams, chat, AirDrop, …). */
    suspend fun shareText(content: String, filename: String, mimeType: String, title: String)

    /** Lets the user pick a location and saves the file to local storage / Files / Drive. */
    suspend fun saveText(content: String, filename: String, mimeType: String)

    /**
     * Lets the user pick a file (Files app, document picker, open dialog) and returns its text,
     * or null when the picker was cancelled or the file could not be read.
     */
    suspend fun openText(mimeTypes: List<String>, extensions: List<String>): String?
}

enum class ExportAction { SAVE, SHARE }

object NoOpFileExporter : FileExporter {
    override suspend fun shareText(content: String, filename: String, mimeType: String, title: String) = Unit
    override suspend fun saveText(content: String, filename: String, mimeType: String) = Unit
    override suspend fun openText(mimeTypes: List<String>, extensions: List<String>): String? = null
}
