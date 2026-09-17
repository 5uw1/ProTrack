package com.suw1labs.worktracker

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var pendingExportContent: String? = null
    private var pendingOpenResult: ((String?) -> Unit)? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** "Save file" pickers: the user chooses the folder (Downloads, Drive, …) and we write the file there. */
    private val createCsvDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), ::writePendingExport)
    private val createJsonDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json"), ::writePendingExport)

    /** "Open file" picker for restoring a backup. */
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val onResult = pendingOpenResult
        pendingOpenResult = null
        if (onResult == null) return@registerForActivityResult
        if (uri == null) {
            onResult(null)
            return@registerForActivityResult
        }
        ioScope.launch {
            val text = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            withContext(Dispatchers.Main) { onResult(text) }
        }
    }

    private fun writePendingExport(uri: Uri?) {
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) return
        ioScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, if (ok) "Saved" else "Could not save file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = WorkTrackerApplication.container(applicationContext)
        WorkTrackerApplication.fileExporter?.saveRequestHandler = { filename, mimeType, content ->
            pendingExportContent = content
            (if (mimeType == "application/json") createJsonDocument else createCsvDocument).launch(filename)
        }
        WorkTrackerApplication.fileExporter?.openRequestHandler = { mimeTypes, onResult ->
            pendingOpenResult?.invoke(null)
            pendingOpenResult = onResult
            openDocument.launch(mimeTypes)
        }

        setContent {
            App(container)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            WorkTrackerApplication.fileExporter?.saveRequestHandler = null
            WorkTrackerApplication.fileExporter?.openRequestHandler = null
        }
        super.onDestroy()
    }
}
