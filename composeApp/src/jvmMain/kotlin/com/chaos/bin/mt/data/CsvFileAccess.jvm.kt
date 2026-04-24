package com.chaos.bin.mt.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class JvmCsvFileAccess : CsvFileAccess {

    override suspend fun exportToUserFile(suggestedFileName: String, content: String): Boolean =
        withContext(Dispatchers.IO) {
            val chooser = JFileChooser().apply {
                dialogTitle = "导出 CSV"
                fileFilter = FileNameExtensionFilter("CSV 文件 (*.csv)", "csv")
                selectedFile = File(suggestedFileName)
            }
            val ret = chooser.showSaveDialog(null)
            if (ret != JFileChooser.APPROVE_OPTION) return@withContext false
            val chosen = chooser.selectedFile ?: return@withContext false
            val target = if (chosen.name.endsWith(".csv", ignoreCase = true)) chosen
            else File(chosen.parentFile, chosen.name + ".csv")
            try {
                target.writeText(content, Charsets.UTF_8)
                true
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun importFromUserFile(): String? =
        withContext(Dispatchers.IO) {
            val chooser = JFileChooser().apply {
                dialogTitle = "导入 CSV"
                fileFilter = FileNameExtensionFilter("CSV 文件 (*.csv)", "csv")
            }
            val ret = chooser.showOpenDialog(null)
            if (ret != JFileChooser.APPROVE_OPTION) return@withContext null
            val chosen = chooser.selectedFile ?: return@withContext null
            try {
                chosen.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
}

@Composable
actual fun rememberCsvFileAccess(): CsvFileAccess = remember { JvmCsvFileAccess() }
