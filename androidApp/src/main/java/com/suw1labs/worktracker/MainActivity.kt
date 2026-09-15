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
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** "Save file" picker: the user chooses the folder (Downloads, Drive, …) and we write the CSV there. */
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) return@registerForActivityResult
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
        WorkTrackerApplication.fileExporter?.saveRequestHandler = { filename, _, content ->
            pendingExportContent = content
            createDocument.launch(filename)
        }

        setContent {
            App(container)
        }
    }

    override fun onDestroy() {
        if (WorkTrackerApplication.fileExporter?.saveRequestHandler != null && isFinishing) {
            WorkTrackerApplication.fileExporter?.saveRequestHandler = null
        }
        super.onDestroy()
    }
}
