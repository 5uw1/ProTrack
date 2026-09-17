package com.suw1labs.worktracker.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLBookmarkCreationMinimalBookmark
import platform.Foundation.NSURLBookmarkResolutionWithoutUI
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS: a folder from the Files picker (iCloud Drive, On My iPhone, or any file provider such as
 * Google Drive). Access outside the sandbox needs a security-scoped bookmark, which is kept in
 * NSUserDefaults and re-resolved for every read / write.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackupFolderStore : BackupFolderStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _folder = MutableStateFlow(
        defaults.dataForKey(KEY_BOOKMARK)?.let { BackupFolder(KEY_BOOKMARK, defaults.stringForKey(KEY_NAME) ?: "Folder") }
    )
    override val folder: StateFlow<BackupFolder?> = _folder

    /** The picker only holds a weak reference to its delegate; keep it alive while it is shown. */
    private var pickerDelegate: PickerDelegate? = null

    override suspend fun pickFolder(): BackupFolder? {
        val url = withContext(Dispatchers.Main) { pickDirectory() } ?: return null
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val bookmark = url.bookmarkDataWithOptions(
                options = NSURLBookmarkCreationMinimalBookmark,
                includingResourceValuesForKeys = null,
                relativeToURL = null,
                error = null
            ) ?: return null
            val chosen = BackupFolder(KEY_BOOKMARK, url.lastPathComponent ?: "Folder")
            defaults.setObject(bookmark, forKey = KEY_BOOKMARK)
            defaults.setObject(chosen.displayName, forKey = KEY_NAME)
            _folder.value = chosen
            return chosen
        } finally {
            if (accessing) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun clearFolder() {
        defaults.removeObjectForKey(KEY_BOOKMARK)
        defaults.removeObjectForKey(KEY_NAME)
        _folder.value = null
    }

    override suspend fun write(filename: String, content: String) = withContext(Dispatchers.IO) {
        withFolder { dir ->
            val target = dir.URLByAppendingPathComponent(filename) ?: throw IllegalStateException("bad backup path")
            @Suppress("CAST_NEVER_SUCCEEDS")
            val ok = (content as NSString).writeToURL(target, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            if (!ok) throw IllegalStateException("backup folder not writable")
        }
        Unit
    }

    override suspend fun read(filename: String): String? = withContext(Dispatchers.IO) {
        withFolder { dir ->
            val target = dir.URLByAppendingPathComponent(filename) ?: return@withFolder null
            val path = target.path ?: return@withFolder null
            if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return@withFolder null
            NSString.create(contentsOfURL = target, encoding = NSUTF8StringEncoding, error = null)?.toString()
                ?: throw IllegalStateException("backup file not readable")
        }
    }

    /** Resolves the bookmark and runs [block] inside the security scope. */
    private inline fun <T> withFolder(block: (NSURL) -> T): T {
        val bookmark: NSData = defaults.dataForKey(KEY_BOOKMARK) ?: throw IllegalStateException("no backup folder chosen")
        val url = NSURL.URLByResolvingBookmarkData(
            bookmark,
            options = NSURLBookmarkResolutionWithoutUI,
            relativeToURL = null,
            bookmarkDataIsStale = null,
            error = null
        ) ?: throw IllegalStateException("backup folder no longer reachable")
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            return block(url)
        } finally {
            if (accessing) url.stopAccessingSecurityScopedResource()
        }
    }

    private suspend fun pickDirectory(): NSURL? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController()
        if (presenter == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder), asCopy = false)
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
        val keyWindow = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: return null
        var top = keyWindow.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }

    private companion object {
        const val KEY_BOOKMARK = "autoBackupFolderBookmark"
        const val KEY_NAME = "autoBackupFolderName"
    }
}
