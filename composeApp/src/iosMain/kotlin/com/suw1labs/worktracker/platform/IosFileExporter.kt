package com.suw1labs.worktracker.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Writes the export into the temp directory and presents the iOS share sheet for it.
 */
class IosFileExporter : FileExporter {

    /** The document picker only holds a weak reference to its delegate; keep it alive while it is shown. */
    private var pickerDelegate: PickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun writeTempFile(content: String, filename: String): String? {
        val path = NSTemporaryDirectory() + filename
        val written = withContext(Dispatchers.IO) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            (content as NSString).writeToFile(
                path = path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        }
        return if (written) path else null
    }

    /** "Save file": the Files export picker (On My iPhone, iCloud Drive, …). */
    override suspend fun saveText(content: String, filename: String, mimeType: String) {
        val path = writeTempFile(content, filename) ?: return
        withContext(Dispatchers.Main) {
            val presenter = topViewController() ?: return@withContext
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun shareText(content: String, filename: String, mimeType: String, title: String) {
        val path = writeTempFile(content, filename) ?: return

        withContext(Dispatchers.Main) {
            val presenter = topViewController() ?: return@withContext
            val fileUrl = NSURL.fileURLWithPath(path)
            val activityController = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null
            )
            // iPad requires an anchor for the popover presentation.
            activityController.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(activityController, animated = true, completion = null)
        }
    }

    /** "Open file": the Files picker; the document is copied into the sandbox, so no security scope is needed. */
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun openText(mimeTypes: List<String>, extensions: List<String>): String? {
        val url = withContext(Dispatchers.Main) { pickDocument(extensions) } ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                NSString.create(contentsOfURL = url, encoding = NSUTF8StringEncoding, error = null)?.toString()
            }.getOrNull()
        }
    }

    private suspend fun pickDocument(extensions: List<String>): NSURL? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController()
        if (presenter == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val types = extensions.mapNotNull { UTType.typeWithFilenameExtension(it) }.ifEmpty { listOf(UTTypeJSON) }
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        val delegate = PickerDelegate { urls ->
            pickerDelegate = null
            if (continuation.isActive) continuation.resume(urls?.firstOrNull())
        }
        pickerDelegate = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    private class PickerDelegate(private val onResult: (List<NSURL>?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onResult(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onResult(null)
        }
    }

    private fun topViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        val keyWindow = scenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: return null

        var top = keyWindow.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }
}
