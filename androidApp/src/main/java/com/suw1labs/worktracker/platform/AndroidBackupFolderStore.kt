package com.suw1labs.worktracker.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android: the folder is a Storage Access Framework tree (Google Drive, OneDrive, the device, an SD
 * card, …) picked with ACTION_OPEN_DOCUMENT_TREE. The persistable permission is taken so the app
 * can keep writing there after a restart. The URI lives in SharedPreferences, not in the database.
 */
class AndroidBackupFolderStore(private val context: Context) : BackupFolderStore {
    private val prefs = context.getSharedPreferences("auto_backup", Context.MODE_PRIVATE)
    private val _folder = MutableStateFlow(
        prefs.getString(KEY_URI, null)?.let { BackupFolder(it, prefs.getString(KEY_NAME, null) ?: displayNameOf(Uri.parse(it))) }
    )
    override val folder: StateFlow<BackupFolder?> = _folder

    /** Set by the foreground activity: launches the folder picker and hands back the tree URI (null when cancelled). */
    var pickRequestHandler: ((onResult: (Uri?) -> Unit) -> Unit)? = null

    override suspend fun pickFolder(): BackupFolder? {
        val handler = pickRequestHandler ?: return null
        val uri = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Uri?> { continuation ->
                handler { picked -> if (continuation.isActive) continuation.resume(picked) }
            }
        } ?: return null
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        val chosen = BackupFolder(uri.toString(), displayNameOf(uri))
        prefs.edit().putString(KEY_URI, chosen.reference).putString(KEY_NAME, chosen.displayName).apply()
        _folder.value = chosen
        return chosen
    }

    override fun clearFolder() {
        _folder.value?.let { old ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(old.reference), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
        prefs.edit().remove(KEY_URI).remove(KEY_NAME).apply()
        _folder.value = null
    }

    override suspend fun write(filename: String, content: String) = withContext(Dispatchers.IO) {
        val tree = treeUri()
        val resolver = context.contentResolver
        val existing = findChild(tree, filename)
        val target = existing ?: DocumentsContract.createDocument(resolver, treeDocumentUri(tree), MIME_JSON, filename)
            ?: throw IOException("could not create $filename in backup folder")
        val bytes = content.toByteArray(Charsets.UTF_8)
        // "wt" truncates; a few providers only accept "w", which truncates as well for whole-file writes.
        val stream = runCatching { resolver.openOutputStream(target, "wt") }.getOrNull()
            ?: resolver.openOutputStream(target, "w")
            ?: throw IOException("backup folder not writable")
        stream.use { it.write(bytes) }
        Unit
    }

    override suspend fun read(filename: String): String? = withContext(Dispatchers.IO) {
        val doc = findChild(treeUri(), filename) ?: return@withContext null
        context.contentResolver.openInputStream(doc)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IOException("backup file not readable")
    }

    private fun treeUri(): Uri = Uri.parse(_folder.value?.reference ?: throw IOException("no backup folder chosen"))

    private fun treeDocumentUri(tree: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** The document with [name] directly inside the tree, or null. */
    private fun findChild(tree: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
            }
        } ?: throw IOException("backup folder not readable")
        return null
    }

    private fun displayNameOf(tree: Uri): String = runCatching {
        context.contentResolver.query(treeDocumentUri(tree), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: DocumentsContract.getTreeDocumentId(tree).substringAfterLast(':').ifEmpty { tree.lastPathSegment ?: "Folder" }

    private companion object {
        const val KEY_URI = "tree_uri"
        const val KEY_NAME = "display_name"
        const val MIME_JSON = "application/json"
    }
}
