# MoneyTracker 记账 App 全量实现

**ID**: 001  
**状态**: 待开始  
**优先级**: high  
**负责人**: jiangjiwei  
**创建时间**: 2026-04-21 17:22  
**更新时间**: 2026-04-21 17:22  
**标签**: kmp, compose, sqldelight, voyager, money-tracker

## 背景
用户用 Claude Design（claude.ai/design）画好了记账 App 的 HTML/JS/JSX 原型（记账-handoff.zip），包含首页、记一笔、分类管理、自动记账、隐私保护等 7 个屏 + 深色模式变体。项目基础是 KMP + Compose Multiplatform 空壳工程，计划在现有 composeApp 模块里做出完整产品。

## 目标
纯本地、无云同步的多端记账 App，Android/iOS/Desktop 三平台共享 UI 和数据库。覆盖：记录 CRUD、分类/账户管理、隐私遮蔽、应用锁、自动记账（打开 App 时追赶触发）、导入导出。视觉效果对齐 warm 暖灰主题，跟随系统深浅色。

## 讨论记录
- **存储用 SQLDelight，不用 Room-KMP**
  - 原因：SQLDelight KMP 支持第一公民，生成代码更轻，记账查询场景够用；Room-KMP 还在演进且 Gradle 配置更麻烦
- **自动记账改为启动时追赶，不做真实定时**
  - 原因：纯本地应用无云同步，每次打开 App 时计算 [lastFiredAt, now] 之间本应触发的规则，逐个插入记录（时间戳用规则触发时刻）。省去 Android WorkManager / iOS BGTask / Desktop scheduler 各写一套，全共享代码
- **三平台并行推进，不走 Android 优先串行**
  - 原因：用户主攻 Android 但想要 KMP 三端一起做，已验证共享代码能无修改跑在 JVM 和 Android
- **金额用 Long 类型的『分』存储，不用 Double 或 BigDecimal**
  - 原因：避免浮点误差；显示层 formatYuan(cents) 还原为 12,345.67 字符串；Compose Multiplatform 没有开箱即用的 BigDecimal
- **UI 导航用 Voyager，不自己写**
  - 原因：用户参考的 BleDemo 就是 Voyager，TabNavigator + Navigator 栈组合够用
- **图标全部用 ImageVector.Builder 手写，不引入 material-icons-extended**
  - 原因：原型设计里的图标是自定义 1.6 描边线条，Material 图标视觉会跑偏，手写 20 多个图标比引入整个库更可控

## 实施步骤
- [x] **零期：UI 骨架**（commit `8817a78`）— 7 个屏静态画完，Voyager 导航 + warm 主题 + 手写线条图标，跑在 MockData 上
- [x] **一期：最小闭环**（待提交）— SQLDelight schema + 三平台 driver + Seeder + Repository + HomeVM/EntryVM + Home 和 Entry 接真实 DB
- [~] 二期：完整 CRUD — 首页记录行编辑删除 ✅ / 月份 picker ✅ / 分类管理 CRUD ❌ / 账户管理 ❌
- [ ] 三期：隐私 + 主题 — 隐私全局开关 + 主题手动切换 + 应用锁（Android BiometricPrompt / iOS LAContext）+ 截屏遮蔽（Android FLAG_SECURE / iOS secureText）
- [ ] 四期：自动记账 — AutoRule CRUD + 启动时追赶引擎（共享 KMP 代码，无平台定时）+ 追赶上限保护
- [ ] 五期：打磨 — 嵌入字体（Noto Sans SC/Inter）+ 空状态 UI + 过渡动画 + CSV/JSON 导入导出 + iCloud/Google Drive/本地备份 + 关键 VM/Repo 单元测试

## 当前进度（截至 2026-04-21）

### 零期（已完成，已提交）
**commit**: `8817a78 feat(ui): 基于设计原型搭建记账 App 的 Compose UI 骨架`

画完 7 个静态屏：首页（暖灰看板 + 日分组 + 隐私遮蔽）、记一笔（支出/收入切换 + 分类网格 + 小类 + 键盘）、设置入口、分类管理、自动记账、隐私保护。加了 Voyager 导航 + 底部 tab bar + 设置子页返回栈。warm 主题光/暗两套。所有图标用 `ImageVector.Builder` 手写。

### 一期（已完成并用户验收，待提交）

**已做**:
- SQLDelight 2.1.0 插件和三平台 driver（AndroidSqliteDriver / NativeSqliteDriver / JdbcSqliteDriver），通过 expect/actual 的 `DatabaseFactory`
- `.sq` schema 和查询（Category / SubCategory / Account / Record / AutoRule / Preference），Record 表带 JOIN 查询 `selectInRangeDetailed`
- Repository 层：`CategoryRepository`（大类 + 小类树、Flow<List<Category>>）、`AccountRepository`、`RecordRepository`（按月查询 + 求和 + insert + update + delete + getRawById + observeMonthlySummaries）、`PreferenceRepository`
- `DefaultDataSeeder`：首启幂等写入 8 支出分类 + 4 收入分类（带小类和隐私标记）+ 4 账户
- `HomeViewModel`：月份 StateFlow + combine 记录流/汇总流/隐私偏好流/账户流/月度汇总流
- `EntryViewModel`：单一 `EntryDraft` 数据类 + MutableStateFlow + flatMapLatest 响应 kind 切换；支持 `loadForEdit` + `save` 走 insert / update 分支
- `AppContainer` + `LocalAppContainer` CompositionLocal 依赖传递 + `pendingEditRecordId` StateFlow 跨 tab 传递编辑目标
- 金额以分（Long）存储，UI 层 `formatYuan(cents)` 格式化为 "12,345.67"
- 三平台入口（MainActivity / main.kt / MainViewController）各自构造 `AppContainer` 传给 `App(container)`
- `App.kt` LaunchedEffect 调用 `seeder.seedIfEmpty()`

### 超额完成（原属二期）

**记录编辑 / 删除**:
- 首页点记录行 → `RecordActionsDialog`（显示详情 + 编辑 + 关闭 + 删除带二次确认）
- 点编辑：`AppContainer.pendingEditRecordId` 写入 id + `TabNavigator.current = EntryTab`；Entry 监听后 `vm.loadForEdit(id)`，顶部出现"编辑记录 · 取消"灰色横幅；点完成走 update，自动切回首页
- 点删除：`RecordRepository.delete(id)`，Flow 自动刷新首页

**月份切换**:
- 首页左上角 "YYYY 年 MM 月" chip 可点 → `MonthPickerSheet` 底部弹窗
- `RecordRepository.observeMonthlySummaries()`：一次拉所有记录，按本地时区在 Kotlin 层聚合成 `Map<(year, month), MonthSummary>`（避开 SQLite strftime 的 UTC 时区坑）
- 按年倒序排，sticky header；每月显示支出/收入汇总；当前选中月 / 今天月都有视觉区分
- 默认滚动定位到今天所在月
- 非当前月时首页标题旁显示"回本月" chip

**Entry 交互优化**:
- 金额区去掉"大类 · 小类"小字头和右侧闪烁光标
- 备注改成行内 `BasicTextField`（去掉弹窗）
- 账户选择改成 `AccountPickerSheet` 底部弹窗
- 日期时间选择改成 `DateTimePickerSheet`：5 列滚轮始终挂载，每列有独立 surface 色蒙版盖住上下段（中间 36dp 空档露出选中项），点字段 → 该字段蒙版 alpha 渐变到 0 露出完整滚轮，WheelPicker 带 iOS 风格 snap 滚动

**首页其他**:
- 柱状图用真实当月每日支出归一化绘制，今天高亮 accent 色
- 字号全局 +2 批量调整（Python 脚本一次处理 14 个文件）

**验证**:
- JVM + Android 目标编译通过
- Desktop + Pixel 6a 双端跑通记账完整闭环（用户手工点测通过）

**遗留 warning**:
- kotlinx-datetime 0.7.1 `Instant` / `Clock` 迁到 `kotlin.time` 的 deprecation（不影响运行）
- `monthNumber` / `dayOfMonth` 改名为 `month` / `day` 的 deprecation
- expect/actual 类 Beta 警告

### 二期剩余

- 分类管理 CRUD（目前 `CategoriesScreen` 仍读 `MockData`，需接到 `CategoryRepository` + 增删改排序 + 隐私标记）
- 账户管理（原型没这个入口，需要在设置里加一个 `AccountsScreen` + CRUD）

### 三个设置子页状态
- 分类管理：挂在 `MockData` 上，待二期
- 自动记账：挂在 `MockData` 上，待四期
- 隐私保护：挂在硬编码列表上，待三期

## 技术方案
分层结构：commonMain/db（SQLDelight 生成）→ commonMain/data（Models、Repositories、Seeder、Time）→ commonMain/di（AppContainer + LocalAppContainer）→ commonMain/ui/{home,entry,settings,nav,components,theme}（Compose UI + ViewModel）。平台入口（MainActivity / main.kt / MainViewController）各自构造 AppContainer 传给 App()。ViewModel 用 lifecycle-viewmodel-compose + StateFlow + viewModelScope。自动记账的追赶引擎放 commonMain/data/AutoRuleScheduler.kt，App 初始化时 LaunchedEffect 调一次。

## 文件清单
- `composeApp/build.gradle.kts`
- `gradle/libs.versions.toml`
- `composeApp/src/commonMain/sqldelight/com/chaos/bin/mt/db/*.sq`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/App.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Models.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/DomainTypes.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Repositories.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/DefaultDataSeeder.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Time.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/di/AppContainer.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/theme/AppTheme.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/home/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/home/HomeViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/entry/EntryScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/entry/EntryViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/CategoriesScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/AutomationScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/PrivacyScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/SettingsHomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/nav/AppScaffold.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/components/LineIcons.kt`
- `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/components/CommonComponents.kt`
- `composeApp/src/androidMain/kotlin/com/chaos/bin/mt/MainActivity.kt`
- `composeApp/src/androidMain/kotlin/com/chaos/bin/mt/db/DatabaseFactory.android.kt`
- `composeApp/src/iosMain/kotlin/com/chaos/bin/mt/MainViewController.kt`
- `composeApp/src/iosMain/kotlin/com/chaos/bin/mt/db/DatabaseFactory.ios.kt`
- `composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/main.kt`
- `composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/db/DatabaseFactory.jvm.kt`

## 风险和依赖
1) 自动记账追赶引擎要设上限（比如最多追 90 天或 N 条），否则新装用户首次打开会插一堆历史记录；2) 用户手动删除过的自动记录不补回，靠只推进 lastFiredAt 保证不重复；3) Desktop 端 emoji 字体渲染可能回退成单色（Skia 渲染在 macOS 下正常，Linux/Windows 可能需要手动嵌入 emoji 字体）；4) kotlinx-datetime 0.7.1 把 Instant/Clock 迁到 kotlin.time，目前有一堆 deprecation warning 需要后续清理；5) 应用锁和截屏遮蔽在三平台的 actual 实现差异较大，Desktop 端可能直接跳过这两项

## 验收标准
- [x] 一期：App 能启动，首页空状态正常显示，记一笔能选分类/账户/键盘输入金额/保存，保存后首页能看到新记录（编译+启动通过，端到端闭环待用户手动验证）
- [ ] 二期：在设置里能新建/编辑/删除大类和小类、能拖拽排序、能独立管理账户；首页点任意记录能进详情并删除；能切换到任意月份
- [ ] 三期：设置里的隐私开关能控制首页遮蔽、能切换浅色/深色/跟随系统主题；Android/iOS 上能开启应用锁（生物识别）和截屏遮蔽
- [ ] 四期：能在设置里新建自动记账规则（每周/每月某天/每月第 N 个周 M）；关掉 App 隔几天打开能看到自动生成的对应记录；超过追赶上限时不会塞爆
- [ ] 五期：字体嵌入后中英文数字视觉贴近原型；空状态有设计感；能导出/导入 CSV；能备份和恢复；关键 VM/Repo 有单元测试覆盖

## 参考资料
- /tmp/moneytracker-handoff/untitled/project/app.html 设计原型入口
- /tmp/moneytracker-handoff/untitled/README.md Claude Design handoff 说明
- /Users/jiangjiwei/Code/Kmp/main/BleDemo/composeApp/build.gradle.kts 依赖组合参考
- https://cashapp.github.io/sqldelight/ SQLDelight 文档
- https://voyager.adriel.cafe/ Voyager 导航文档
