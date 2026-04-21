package com.chaos.bin.mt.db

/** 每个平台提供自己的 [MtDatabase] 构造方式（驱动 + 文件位置）。 */
expect class DatabaseFactory {
    fun create(): MtDatabase
}
