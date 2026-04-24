package com.chaos.bin.mt.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 暂未实现。后续接入 UIDocumentPickerViewController 实现导入 / 导出。
private object IosCsvFileAccessStub : CsvFileAccess {
    override suspend fun exportToUserFile(suggestedFileName: String, content: String): Boolean {
        println("[CsvFileAccess] iOS 暂不支持导出")
        return false
    }

    override suspend fun importFromUserFile(): String? {
        println("[CsvFileAccess] iOS 暂不支持导入")
        return null
    }
}

@Composable
actual fun rememberCsvFileAccess(): CsvFileAccess = remember { IosCsvFileAccessStub }
