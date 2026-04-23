package com.chaos.bin.mt.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.chaos.bin.mt.data.AccountRepository
import com.chaos.bin.mt.data.AutoRuleRepository
import com.chaos.bin.mt.data.AutoRuleScheduler
import com.chaos.bin.mt.data.CategoryRepository
import com.chaos.bin.mt.data.DefaultDataSeeder
import com.chaos.bin.mt.data.PreferenceRepository
import com.chaos.bin.mt.data.RecordRepository
import com.chaos.bin.mt.db.MtDatabase
import kotlinx.coroutines.flow.MutableStateFlow

/** 按需在平台入口创建一次，所有 ViewModel / Repository 都由这里统一持有。 */
class AppContainer(val database: MtDatabase) {
    val categoryRepository = CategoryRepository(database)
    val accountRepository = AccountRepository(database)
    val recordRepository = RecordRepository(database)
    val preferenceRepository = PreferenceRepository(database)
    val autoRuleRepository = AutoRuleRepository(database)
    val autoRuleScheduler = AutoRuleScheduler(
        autoRuleRepository = autoRuleRepository,
        recordRepository = recordRepository,
    )
    val seeder = DefaultDataSeeder(
        categoryRepository = categoryRepository,
        accountRepository = accountRepository,
        preferenceRepository = preferenceRepository,
    )

    /**
     * 首页跨 tab 请求进入编辑模式时写入此 StateFlow，EntryScreen 监听后 load 并清空。
     * 解决 Voyager TabNavigator 下不同 tab 间不好传数据的问题。
     */
    val pendingEditRecordId = MutableStateFlow<Long?>(null)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
