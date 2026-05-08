# 开发笔记

记录项目开发过程中的问题、解决方案和最佳实践。

## MoneyTracker 项目背景速览
- **日期**: 2026-05-07
- **标签**: kmp, compose, sqldelight, voyager, money-tracker, background
- **问题**: 为后续开发建立项目背景：MoneyTracker 是个人记账 App，要求 Android / iOS / Desktop 三端共享 UI 与数据库，当前已完成记账闭环、分类账户管理、隐私遮蔽、自动记账和 CSV 导入导出，并有通知提醒实施计划待开工。
- **解决方案**: 项目是 Kotlin Multiplatform + Compose Multiplatform 单模块 composeApp。commonMain 按 db/data/di/ui 分层：SQLDelight .sq 生成 MtDatabase；data 层封装 Category/Account/Record/Preference/AutoRule/Csv 仓储与时间工具；AppContainer 统一持有数据库、Repository、Seeder、AutoRuleScheduler 和跨 Tab 编辑状态；App 启动时 seedIfEmpty() 并执行 autoRuleScheduler.catchUp()；Voyager TabNavigator 管首页/记一笔/设置，设置页内部用 Navigator + SlideTransition。金额统一用 Long 分存储，AppTimeZone 负责本地时区月边界与分组。平台差异主要通过 expect/actual 实现 DatabaseFactory 与 CsvFileAccess，其中 iOS CSV 目前是 stub。
- **相关文件**:
  - `README.md`
  - `docs/notification-reminder-plan.md`
  - `composeApp/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/App.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/di/AppContainer.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Repositories.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/AutoRuleScheduler.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/CsvUtils.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/CsvImportService.kt`
  - `composeApp/src/commonMain/sqldelight/com/chaos/bin/mt/db/*.sq`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/nav/AppScaffold.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/home/HomeViewModel.kt`
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/entry/EntryViewModel.kt`
- **经验教训**: 后续改动优先放 commonMain，平台能力再用 expect/actual 补齐。删除账户/分类前要用 RecordRepository countBy* 做关联检查。分类隐私变更需注意首页 JOIN 流不会自动因 Category 表变化刷新，HomeViewModel 已通过 combine 分类树覆盖 privacy。自动记账是启动时追赶而非后台定时，新规则 lastFiredAt 初始化为 now，避免回补创建前历史；当前无追赶上限是潜在风险。CSV 导入按中文名称匹配分类/账户，非法行跳过。验证基线：./gradlew :composeApp:jvmTest 当前通过，但有 kotlinx-datetime Instant/monthNumber/dayOfMonth 与 expect/actual Beta 警告。
