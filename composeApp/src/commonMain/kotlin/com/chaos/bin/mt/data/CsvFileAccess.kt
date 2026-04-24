package com.chaos.bin.mt.data

import androidx.compose.runtime.Composable

/** 平台文件选择抽象。由 [rememberCsvFileAccess] 在 Composable 中创建。 */
interface CsvFileAccess {
    /** 让用户选择一个文件保存 CSV；返回是否真的保存成功（用户取消 / 出错返回 false）。 */
    suspend fun exportToUserFile(suggestedFileName: String, content: String): Boolean

    /** 让用户选一个 CSV 文件读入。用户取消 / 出错返回 null。 */
    suspend fun importFromUserFile(): String?
}

/** 每个平台在其 sourceSet 里提供 actual 实现。 */
@Composable
expect fun rememberCsvFileAccess(): CsvFileAccess
