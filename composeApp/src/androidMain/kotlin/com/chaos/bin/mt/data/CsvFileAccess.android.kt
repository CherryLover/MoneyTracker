package com.chaos.bin.mt.data

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private class AndroidCsvFileAccess(
    private val context: Context,
) : CsvFileAccess {

    // 同一时刻只允许一个保存 / 打开会话（避免 launcher 的 pending callback 互相覆盖）
    private val exportGate = Mutex()
    private val importGate = Mutex()

    // Composable 注入的 launcher 与当前挂起的 continuation
    var createLauncher: ActivityResultLauncher<String>? = null
    var openLauncher: ActivityResultLauncher<Array<String>>? = null

    @Volatile var pendingContent: String? = null
    @Volatile var pendingExport: ((Uri?) -> Unit)? = null
    @Volatile var pendingImport: ((Uri?) -> Unit)? = null

    override suspend fun exportToUserFile(suggestedFileName: String, content: String): Boolean = exportGate.withLock {
        val launcher = createLauncher ?: return@withLock false
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            pendingContent = content
            pendingExport = { result -> cont.resume(result) }
            try {
                launcher.launch(suggestedFileName)
            } catch (e: Exception) {
                pendingExport = null
                pendingContent = null
                cont.resume(null)
            }
        }
        if (uri == null) {
            pendingContent = null
            return@withLock false
        }
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                pendingContent = null
            }
        }
    }

    override suspend fun importFromUserFile(): String? = importGate.withLock {
        val launcher = openLauncher ?: return@withLock null
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            pendingImport = { result -> cont.resume(result) }
            try {
                launcher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"))
            } catch (e: Exception) {
                pendingImport = null
                cont.resume(null)
            }
        }
        if (uri == null) return@withLock null
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
actual fun rememberCsvFileAccess(): CsvFileAccess {
    val context = LocalContext.current
    val access = remember(context) { AndroidCsvFileAccess(context) }
    access.createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        access.pendingExport?.invoke(uri)
        access.pendingExport = null
    }
    access.openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        access.pendingImport?.invoke(uri)
        access.pendingImport = null
    }
    return access
}
