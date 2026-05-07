# 记账提醒（本地通知）功能实施计划

> 作者：jiangjiwei · Claude Code
> 日期：2026-05-07（v2，按评审反馈调整）
> 状态：代码已实现（2026-05-07），待真机验收

## 1. 功能摘要

在「设置」页新增「通知提醒」入口，允许用户配置每天**最多 3 条** 本地定时提醒，到点弹一条手机系统通知，文案在硬编码池里随机取，提示用户该记账了。

**硬约束**

- 最多 **3 条** 提醒
- 任意两条之间的时间差 **≥ 60 分钟**（按时间点的绝对差，不跨午夜 wrap）
- **没有总开关**：每条独立 enable/disable；列表为空就什么都不弹
- 仅本地通知，**不依赖任何远程推送 / 后台服务**
- 三端：Android（实做） / iOS（实做） / **Desktop 入口完全隐藏**

**权限触达时机**

| 场景 | 行为 |
|------|------|
| 首次进入「通知提醒」设置页 | 检查授权 → 未授权时弹解释 dialog → 用户同意后调系统授权弹窗 |
| 进入首页 (HomeScreen) | 后台检查授权状态；若**至少有 1 条 enabled 提醒**但**系统授权被关**，顶部出现一条 inline banner（"通知未开启，无法收到提醒，点此前往系统设置"） |
| 切到前台 | 同上，每次 onResume 重新检查（避免用户去系统关了再回来） |

---

## 2. UX 流程

整体风格 / 组件参考已实现的 **`AutomationScreen.kt`**（自动记账列表页），照抄其骨架：PageHeader 右侧文字按钮 + 顶部说明 + 空状态 EmptyState + 卡片列表。

### 2.1 设置首页

`SettingsHomeScreen` 的 NavGroup 增加一项：

```
LineIcons.Bell → "通知提醒" → "每日记账提醒，最多 3 条"
```

新增 `SettingsDest.Notifications`，路由进入 `NotificationsScreen`。

> 桌面端编译时通过 `expect val isReminderSupportedOnPlatform: Boolean` 判断，false 则**不渲染** 这一行 NavGroup item。

### 2.2 通知列表页 `NotificationsScreen`

完全套用 `AutomationScreen` 的结构：

```
┌──────────────────────────────────────┐
│ ← 通知提醒                    新增 │   PageHeader（右侧 "新增" 文字按钮）
├──────────────────────────────────────┤
│ 到点提醒你记账，最多 3 条，相邻不少于   │   说明文字（c.text2 / 14sp）
│ 1 小时。                              │
│                                      │
│ ┌──────────────────────────────┐    │
│ │ 🕗 08:00              ●━━○ │    │   卡片：时间 + 单条 Switch
│ │ ─────────────────────────── │    │
│ │ 早间提醒                    │    │   当前文案不可配，留作未来扩展
│ └──────────────────────────────┘    │
│ ┌──────────────────────────────┐    │
│ │ 🕘 21:30              ●━━○ │    │
│ │ ─────────────────────────── │    │
│ │ 晚间提醒                    │    │
│ └──────────────────────────────┘    │
└──────────────────────────────────────┘
```

**空状态**（无任何提醒时）—— 完全照搬 `AutomationScreen` 的 EmptyState 用法：

```kotlin
EmptyState(
    icon = LineIcons.Bell,
    title = "还没有提醒",
    description = "设置每日定时提醒，到点轻轻提醒你记账",
    actionLabel = "新增提醒",
    onAction = { showEditDialog(null) },
)
```

**已有 1-3 条**：

- PageHeader 右侧的「新增」按钮 —— 已经 3 条时该文本变灰、`enabled = false`
- 列表卡片样式参考 `AutomationScreen.RuleCard`：圆角 + hairline 边框 + 透明度（disabled 项 alpha = 0.55）
- 每行点击进入编辑 Dialog；右侧 `ThemedSwitch` 切换 enabled
- 删除走编辑 Dialog 内的"删除"按钮（参考 AutomationEditScreen 的"删除此规则"按钮）

### 2.3 编辑 Dialog `NotificationEditDialog`

> 因为表单极简（只有 hour + minute），用 `AlertDialog` 形态而不是另开一个全屏页 —— 节省一层导航。

复用现有 `AutomationEditScreen` 里的 `TimeStepper(hour, minute, ...)` 组件 —— 第一步把它从 `AutomationEditScreen.kt` 提取到 `ui/components/TimeStepper.kt`，AutomationEditScreen 改 import，行为不变。

Dialog 内容：
- 标题：「新增提醒」/「编辑提醒」
- 中部：TimeStepper（默认值：新增时 09:00；编辑时带入旧值）
- 冲突提示：检测到与已存项相距 < 60 分钟时显示一行红字 "与 21:30 太近，至少间隔 60 分钟"，并 disable 保存按钮
- 按钮：[取消] [删除（仅编辑态）] [保存]

### 2.4 首页未授权 banner

在 `HomeScreen` 顶部（标题下方、月份切换之前）插一个细 banner：

> ⚠️ 通知未开启，无法收到记账提醒。**前往设置 →**

显示条件（合并 AND）：
1. `reminderRepository.observe()` 流里**至少有 1 条 enabled = true**
2. `notificationPermission.isGranted()` 返回 false

点击调用 `notificationPermission.openAppSettings()` 跳到系统通知设置页。

---

## 3. 数据存储

### 3.1 不建表，存 Preference

按评审决定，不新建 SQLDelight 表，**复用现有 `PreferenceRepository` (KV 表)**，存一个 JSON 字符串：

| key | 类型 | 含义 |
|-----|------|------|
| `reminder.schedules` | JSON 数组字符串 | 全部提醒的有序列表（最多 3 条） |

JSON 形态（最小可用）：

```json
[
  { "id": 1, "hour": 8,  "minute": 0,  "enabled": true },
  { "id": 2, "hour": 21, "minute": 30, "enabled": true },
  { "id": 3, "hour": 23, "minute": 0,  "enabled": false }
]
```

- **id**：本地自增即可，仅用于：① UI key ② Android AlarmManager 的 `requestCode` ③ iOS notification identifier。简单做法：从 1 开始，新增时取 `(maxId + 1) coerce 至少 1`，删除不复用。最多到 3 后用户继续添加会拒绝。
- **未来可扩展**字段（先不写进当前 schema，预留）：custom title / weekday mask / 节假日跳过 flag。

### 3.2 引入 `kotlinx.serialization`

项目当前**没引** kotlinx-serialization，需要在 `gradle/libs.versions.toml` + `composeApp/build.gradle.kts` 加上：

```toml
[versions]
kotlinx-serialization = "1.9.0"  # 或与 kotlin 2.3.20 兼容的最新

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

```kotlin
// composeApp/build.gradle.kts
plugins {
    ...
    alias(libs.plugins.kotlinSerialization)
}
sourceSets {
    commonMain.dependencies {
        ...
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### 3.3 数据类（commonMain `data/Reminder.kt`）

```kotlin
@kotlinx.serialization.Serializable
data class ReminderSchedule(
    val id: Long,
    val hour: Int,        // 0..23
    val minute: Int,      // 0..59
    val enabled: Boolean = true,
)

object ReminderRules {
    const val MAX_COUNT = 3
    const val MIN_GAP_MINUTES = 60

    /** 返回与 [target] 时间冲突的已存项；返回 null 表示无冲突。 */
    fun findConflict(
        target: Pair<Int, Int>,
        existing: List<ReminderSchedule>,
        excludingId: Long? = null,
    ): ReminderSchedule? {
        val targetMinutes = target.first * 60 + target.second
        return existing
            .filter { it.id != excludingId }
            .firstOrNull { other ->
                val otherMinutes = other.hour * 60 + other.minute
                kotlin.math.abs(targetMinutes - otherMinutes) < MIN_GAP_MINUTES
            }
    }

    fun nextId(existing: List<ReminderSchedule>): Long =
        ((existing.maxOfOrNull { it.id } ?: 0L) + 1L).coerceAtLeast(1L)
}
```

### 3.4 Repository（commonMain `data/ReminderRepository.kt`）

薄封装 `PreferenceRepository`：

```kotlin
class ReminderRepository(private val prefs: PreferenceRepository) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = "reminder.schedules"

    fun observe(): Flow<List<ReminderSchedule>> = prefs.observe(key)
        .map { it.parse() }

    suspend fun list(): List<ReminderSchedule> = prefs.get(key).parse()

    suspend fun save(items: List<ReminderSchedule>) =
        prefs.set(key, json.encodeToString(items))

    suspend fun add(hour: Int, minute: Int, enabled: Boolean = true): ReminderSchedule {
        val curr = list()
        check(curr.size < ReminderRules.MAX_COUNT)
        val item = ReminderSchedule(ReminderRules.nextId(curr), hour, minute, enabled)
        save(curr + item)
        return item
    }

    suspend fun update(item: ReminderSchedule) {
        val curr = list().map { if (it.id == item.id) item else it }
        save(curr)
    }

    suspend fun delete(id: Long) = save(list().filter { it.id != id })

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val curr = list().map { if (it.id == id) it.copy(enabled = enabled) else it }
        save(curr)
    }

    private fun String?.parse(): List<ReminderSchedule> =
        if (this.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString<List<ReminderSchedule>>(this) }.getOrDefault(emptyList())
}
```

> 容错：解析异常 → 当成空列表（不让一行损坏 JSON 把整个 App 卡死）。

---

## 4. 文案池（硬编码，commonMain `data/ReminderMessages.kt`）

```kotlin
object ReminderMessages {
    const val title = "记一笔"

    val bodies = listOf(
        "今天的账，要不要顺手记一笔",
        "三秒钟就够了，把今天的小开销记下来",
        "钱包还好吗？过来看看",
        "记一笔，让花销心里有数",
        "今天花得怎么样？写下来才不会忘",
        "悄悄提醒：今天还没记账哦",
        "对自己好一点，记得记账",
        "你的小账本，在等你",
        "把今天的支出收拾一下吧",
        "顺手记一下，未来的你会感谢现在的你",
    )

    fun pickBody(seed: Int = -1): String {
        val idx = if (seed >= 0) seed % bodies.size
        else kotlin.random.Random.nextInt(bodies.size)
        return bodies[idx]
    }
}
```

> 触发时刻在 receiver / iOS NotificationContent 里 **当场** 取一条，不写进 JSON 配置。

---

## 5. 跨平台架构

参考项目里 `CsvFileAccess` 的 expect/actual 模式，但不用 `@Composable`（因为 scheduler 要在 App 启动 / ViewModel 里调用，不一定在 Composable 上下文）。

### 5.1 commonMain 抽象

```kotlin
// data/NotificationScheduler.kt
expect class NotificationScheduler {
    /** 注册 / 重排所有 enabled 的提醒。变更后必须调用。幂等。 */
    suspend fun rescheduleAll(schedules: List<ReminderSchedule>)

    /** 取消所有已注册的提醒。 */
    suspend fun cancelAll()
}

// data/NotificationPermission.kt
expect class NotificationPermission {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
    fun openAppSettings()
}

// data/ReminderSupport.kt
expect val isReminderSupportedOnPlatform: Boolean
```

`AppContainer` 持有这两个对象（构造时由平台入口注入）：

```kotlin
class AppContainer(
    val database: MtDatabase,
    val notificationScheduler: NotificationScheduler,
    val notificationPermission: NotificationPermission,
) {
    ...
    val reminderRepository = ReminderRepository(preferenceRepository)
}
```

### 5.2 Android 实现（`androidMain`）

**核心思路**：每条 enabled 提醒注册一个 `AlarmManager` exact alarm，PendingIntent 指向 `ReminderReceiver`。Receiver 触发时 → 弹通知 → 重新排下一天的同一时刻。

```
NotificationScheduler.android.kt
  - 构造参数: Context (applicationContext)
  - rescheduleAll(): 先 cancelAll，再为每条 enabled 调 scheduleOne(id, hour, minute)
  - cancelAll(): 取消可能已经存在的 PendingIntent。注册时 id 范围最多 1..N（N 累加），
    取消时按"最近一次实际持有的 id 列表"（存在 PreferenceRepository 一个旁路 key
    `reminder.scheduled_ids` 里）逐个 cancel。这样不需要遍历全部历史 id。
  - scheduleOne(id, hour, minute):
      val triggerAt = computeNextTriggerMillis(hour, minute, now)  // 复用 commonMain helper
      val intent = Intent(ctx, ReminderReceiver::class.java).putExtra("scheduleId", id)
      val pi = PendingIntent.getBroadcast(ctx, id.toInt(), intent, FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT)
      val am = ctx.getSystemService(AlarmManager::class.java)
      if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pi)
      else am.set(RTC_WAKEUP, triggerAt, pi)              // 降级，允许漂移

ReminderReceiver.kt
  - onReceive(ctx, intent):
      val id = intent.getLongExtra("scheduleId", -1)
      // 1. 弹通知（NotificationManagerCompat，channel "reminder"）
      // 2. 重排下一天同一时刻 alarm
      //    用 goAsync() 拿 PendingResult，跑一个短协程从 Preference 读出 schedules，
      //    找到对应 id 的 hour/minute，再 scheduleOne。9s 内完成。

BootReceiver.kt
  - 监听 BOOT_COMPLETED + MY_PACKAGE_REPLACED
  - onReceive: goAsync() 起一个临时 SQLDelight 连接 → 读 Preference reminder.schedules
    → 反序列化 → 调 NotificationScheduler.rescheduleAll
  - 不依赖 AppContainer 单例（避免冷启动没人构造它）
```

**Manifest 增量**

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<application ...>
    <receiver
        android:name=".notif.ReminderReceiver"
        android:exported="false" />
    <receiver
        android:name=".notif.BootReceiver"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
            <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        </intent-filter>
    </receiver>
</application>
```

**通知 Channel**

App 启动后（MainActivity.onCreate）创建一次：

```kotlin
val channel = NotificationChannel("reminder", "记账提醒", IMPORTANCE_DEFAULT).apply {
    description = "每日定时提醒记账"
}
NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
```

**权限实现 `NotificationPermission.android.kt`**

```kotlin
actual class NotificationPermission(private val ctx: Context) {
    // 由 MainActivity 注入运行时 launcher（与 CsvFileAccess 的 launcher 注册套路一致）
    @Volatile var pendingPermissionContinuation: ((Boolean) -> Unit)? = null
    var requestLauncher: (() -> Unit)? = null

    actual suspend fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        return ContextCompat.checkSelfPermission(ctx, POST_NOTIFICATIONS) == PERMISSION_GRANTED
    }

    actual suspend fun request(): Boolean = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT < 33) {
            cont.resume(NotificationManagerCompat.from(ctx).areNotificationsEnabled())
            return@suspendCancellableCoroutine
        }
        pendingPermissionContinuation = { granted -> cont.resume(granted) }
        requestLauncher?.invoke() ?: cont.resume(false)
    }

    actual fun openAppSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}
```

> `MainActivity` 在 `onCreate` 里 `registerForActivityResult(RequestPermission())` 拿到 launcher，注入到 `NotificationPermission`，回调时 invoke `pendingPermissionContinuation`。

### 5.3 iOS 实现（`iosMain`）

**核心思路**：用 `UNUserNotificationCenter` 注册 `UNCalendarNotificationTrigger`（DateComponents { hour, minute }, repeats=true），系统接管全部调度，App 完全不需要后台运行。

```kotlin
// NotificationScheduler.ios.kt
actual class NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    actual suspend fun rescheduleAll(schedules: List<ReminderSchedule>) {
        cancelAll()
        for (s in schedules.filter { it.enabled }) {
            val content = UNMutableNotificationContent().apply {
                setTitle(ReminderMessages.title)
                setBody(ReminderMessages.pickBody())
                setSound(UNNotificationSound.defaultSound())
            }
            val components = NSDateComponents().apply {
                hour = s.hour.toLong()
                minute = s.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "reminder_${s.id}",
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request) { /* error log */ }
        }
    }

    actual suspend fun cancelAll() {
        center.getPendingNotificationRequestsWithCompletionHandler { reqs ->
            val ids = reqs.orEmpty().filterIsInstance<UNNotificationRequest>()
                .filter { it.identifier.startsWith("reminder_") }
                .map { it.identifier }
            if (ids.isNotEmpty()) center.removePendingNotificationRequestsWithIdentifiers(ids)
        }
    }
}
```

> ⚠️ 文案随机：CalendarTrigger + repeats=true 注册一次后系统会**每天用同一份 content** 弹（content 在注册时定型）。所以"每次随机文案"近似实现：每次 App 启动 + 每次保存提醒时调一次 `rescheduleAll` 重新随机。**已确认接受这一妥协。**

**权限实现 `NotificationPermission.ios.kt`**

```kotlin
actual class NotificationPermission {
    actual suspend fun isGranted(): Boolean = suspendCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val granted = settings?.authorizationStatus in listOf(
                    UNAuthorizationStatusAuthorized,
                    UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral,
                )
                cont.resume(granted)
            }
    }

    actual suspend fun request(): Boolean = suspendCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ -> cont.resume(granted) }
    }

    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any>(), null)
    }
}
```

**iOS App 入口（Swift 改动）**

`iosApp/iosApp/iOSApp.swift` 加 AppDelegate，让前台也能弹通知：

```swift
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: ...) {
        completionHandler([.banner, .sound])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene { WindowGroup { ContentView() } }
}
```

`Info.plist` 不需要新增 key，本地通知不要求 Usage Description。

### 5.4 Desktop（`jvmMain`）

**入口完全隐藏**，但 actual 必须存在让编译通过：

```kotlin
actual class NotificationScheduler {
    actual suspend fun rescheduleAll(schedules: List<ReminderSchedule>) {}
    actual suspend fun cancelAll() {}
}
actual class NotificationPermission {
    actual suspend fun isGranted(): Boolean = false
    actual suspend fun request(): Boolean = false
    actual fun openAppSettings() {}
}
actual val isReminderSupportedOnPlatform: Boolean = false
```

UI 侧：
- `SettingsHomeScreen` NavGroup 构建时根据 `isReminderSupportedOnPlatform` 过滤 Notifications 项
- `HomeScreen` 顶部 banner 也用同一标志短路

---

## 6. 触发时间计算（commonMain 共享）

```kotlin
// data/ReminderTime.kt
fun computeNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long, zone: TimeZone = AppTimeZone): Long {
    val now = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone)
    val today = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, hour, minute, 0, 0)
        .toInstant(zone).toEpochMilliseconds()
    return if (today > nowMillis) today
    else today + 24 * 60 * 60 * 1000L  // 已过 → 明天同一时刻
}
```

可单元测试覆盖（参考 `AutoRuleSchedulerTest` 现有套路）。

---

## 7. 启动追赶 / 重排策略

`App.kt` 的 `LaunchedEffect(container)` 里追加：

```kotlin
LaunchedEffect(container) {
    container.seeder.seedIfEmpty()
    container.autoRuleScheduler.catchUp()
    if (isReminderSupportedOnPlatform) {
        val schedules = container.reminderRepository.list()
        container.notificationScheduler.rescheduleAll(schedules)
    }
}
```

**触发 reschedule 的所有时机**

1. App 启动（上面）
2. 用户在通知页改了任意一条 / 切换 enabled / 删除（ViewModel 内 save 后调）
3. Android `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`
4. iOS：不需要额外（系统接管，启动时重排已覆盖文案随机）

---

## 8. 校验逻辑（ViewModel 层）

`NotificationsViewModel` 在用户点保存时校验：

```kotlin
fun trySave(draft: Draft, editingId: Long?): SaveResult {
    val all = currentSchedulesSnapshot()
    if (editingId == null && all.size >= ReminderRules.MAX_COUNT) {
        return SaveResult.Error("最多只能配置 ${ReminderRules.MAX_COUNT} 条提醒")
    }
    val conflict = ReminderRules.findConflict(
        target = draft.hour to draft.minute,
        existing = all,
        excludingId = editingId,
    )
    if (conflict != null) {
        return SaveResult.Error("与 ${formatTime(conflict)} 太近（不少于 ${ReminderRules.MIN_GAP_MINUTES} 分钟）")
    }
    // commit
    ...
    return SaveResult.Ok
}
```

冲突也作为 UI 实时反馈（用户调 TimeStepper 时立刻显示红字），不只在保存时校验。

---

## 9. 已知坑 & 风险

### Android

| 坑 | 应对 |
|----|------|
| Android 12+ `SCHEDULE_EXACT_ALARM` 用户可在系统关掉 | 调度时 `canScheduleExactAlarms()` 检测，假就降级 `set()`（**已确认接受 ±15min 漂移**），UI 上不主动引导 |
| Android 13+ `POST_NOTIFICATIONS` 运行时权限 | 进入通知页时调 `request()` 走 ActivityResultContracts.RequestPermission |
| Android 14 `SCHEDULE_EXACT_ALARM` 默认拒绝 | 降级方案够用 |
| `setExactAndAllowWhileIdle` doze 冷却 9 分钟 | 一天最多 3 次提醒，不会触发 |
| BroadcastReceiver `onReceive` 必须 ≤10s | 用 `goAsync()` 拿 PendingResult，跑短协程 |
| PendingIntent 必须 `FLAG_IMMUTABLE` (Android 12+) | 直接加 |
| 系统时间被用户改 / 时区切换 | App 启动时 reschedule 兜底；不监听 `ACTION_TIMEZONE_CHANGED`（KISS） |
| 应用被强杀后 alarm 仍触发 | 触发后在 receiver 内重排下一次即可 |
| Boot 后 AppContainer 不存在 | BootReceiver 内自己起临时 SQLDelight 连接读 Preference 反序列化 → 直接调 NotificationScheduler 类（无状态，构造便宜） |

### iOS

| 坑 | 应对 |
|----|------|
| `UNCalendarNotificationTrigger` repeats=true 文案在注册时定型 | 接受现状：每次启动 reschedule 时重新随机一份；用户进 App 文案就刷新 |
| identifier 冲突 | 用 `"reminder_${id}"`；先 removePending 再 add |
| 用户撤销授权后 add 不报错但不显示 | 进入设置页 + 进入首页都做权限检查 |
| 前台时通知默认不弹 | AppDelegate 实现 `willPresent` 返回 `[.banner, .sound]` |
| 用户改设备时区 | 系统按本地 DateComponents 自动调整，无需我们处理 |

### KMP / 跨平台

| 坑 | 应对 |
|----|------|
| `expect class NotificationScheduler` 构造参数三端不同（Android 要 Context） | 平台入口注入：MainActivity 创建时传 `applicationContext`，iOS 直接 `NotificationScheduler()`，jvm 同 |
| AppContainer 现有签名只有 `database: MtDatabase` | 改签名（无外部使用者） |
| kotlinx-serialization 是新依赖 | gradle 同步一次，KMP 标配，风险低 |

---

## 10. 文件清单

### 新增

```
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Reminder.kt              ─┐  数据类 +
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderRepository.kt    ─┤  规则 +
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderMessages.kt      ─┤  文案 +
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderTime.kt          ─┘  时间换算
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.kt    (expect class)
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/NotificationPermission.kt   (expect class)
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderSupport.kt          (expect val)
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/components/TimeStepper.kt     (从 AutomationEditScreen 提取)
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/NotificationsScreen.kt
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/NotificationsViewModel.kt
composeApp/src/commonTest/kotlin/com/chaos/bin/mt/data/ReminderRulesTest.kt
composeApp/src/commonTest/kotlin/com/chaos/bin/mt/data/ReminderTimeTest.kt
composeApp/src/commonTest/kotlin/com/chaos/bin/mt/data/ReminderRepositoryTest.kt   (JSON 序列化兼容性)

composeApp/src/androidMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.android.kt
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/data/NotificationPermission.android.kt
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/data/ReminderSupport.android.kt
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/notif/ReminderReceiver.kt
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/notif/BootReceiver.kt
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/notif/NotificationChannels.kt

composeApp/src/iosMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.ios.kt
composeApp/src/iosMain/kotlin/com/chaos/bin/mt/data/NotificationPermission.ios.kt
composeApp/src/iosMain/kotlin/com/chaos/bin/mt/data/ReminderSupport.ios.kt

composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.jvm.kt
composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/data/NotificationPermission.jvm.kt
composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/data/ReminderSupport.jvm.kt

iosApp/iosApp/AppDelegate.swift                                                    (新增)
```

### 修改

```
gradle/libs.versions.toml                                              新增 kotlinx-serialization
composeApp/build.gradle.kts                                            apply serialization plugin + 依赖
composeApp/src/androidMain/AndroidManifest.xml                         +3 个 uses-permission, +2 个 receiver
composeApp/src/androidMain/kotlin/com/chaos/bin/mt/MainActivity.kt     注册 RequestPermission launcher 注入到 NotificationPermission
composeApp/src/iosMain/kotlin/com/chaos/bin/mt/MainViewController.kt   构造注入 scheduler / permission
composeApp/src/jvmMain/kotlin/com/chaos/bin/mt/main.kt                 同上（NoOp 实例）
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/di/AppContainer.kt   构造签名变更 + 加 reminderRepository
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/App.kt               LaunchedEffect 增加 rescheduleAll
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/SettingsHomeScreen.kt    NavGroup 增加 Notifications 项（受 isReminderSupportedOnPlatform 过滤）
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/nav/AppScaffold.kt                添加 NotificationsScreenRoute
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/home/HomeScreen.kt                顶部添加未授权 banner（条件渲染）
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/home/HomeViewModel.kt             暴露 (hasEnabledReminder, permissionGranted) 流
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/AutomationEditScreen.kt  TimeStepper 改为 import 共享版本
composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/components/LineIcons.kt           新增 Bell 图标
iosApp/iosApp/iOSApp.swift                                             挂载 AppDelegate
```

---

## 11. 实施分期

按"每个 phase 独立提交"的粒度切：

| Phase | 内容 | 验证 |
|-------|------|------|
| **P1 — 数据层** | 引入 kotlinx-serialization；ReminderRepository (JSON over Preference) + 数据类 + 校验工具 + 文案池 + 时间换算 + 单元测试 | `./gradlew :composeApp:jvmTest`，校验 / 时间逻辑 / JSON 序列化测试通过 |
| **P2 — 调度抽象 + Desktop NoOp** | expect class（scheduler / permission / support flag）+ jvmMain NoOp 实现 + AppContainer 改造 | 桌面端 `:composeApp:run` 跑起来不崩，设置页**没有** "通知提醒" 入口 |
| **P3 — Android 调度** | AlarmManager + ReminderReceiver + Channel + Manifest 权限 + BootReceiver + NotificationPermission android | Android 13+ 真机：手动写一条 1 分钟后的 schedule 到 Preference → 调 rescheduleAll → 弹通知 |
| **P4 — iOS 调度** | UNUserNotificationCenter scheduler + permission + AppDelegate.swift | iOS 真机：同上路径 |
| **P5 — 设置页 UI** | NotificationsScreen + ViewModel + NotificationEditDialog + TimeStepper 提取 + SettingsHomeScreen 入口 | 三端：能 add / edit / delete / 切单条 enable；冲突给红字；3 条上限"新增"灰显 |
| **P6 — 首页 banner + 启动 reschedule** | HomeScreen banner（条件渲染）+ App.kt 启动追赶 | Android 关闭通知权限后首页显示 banner，点击跳系统设置；启动时 enabled 项重新注册 |
| **P7 — 收尾打磨** | 文案微调、Bell icon、README "已实现" 增加一行 | README 更新 |

每个 phase 一个 commit。

---

## 12. 验收标准

### 功能

- [ ] 通知设置页：增 / 删 / 改 / 启停单条 四种操作均可
- [ ] 已有 3 条时 PageHeader「新增」按钮 disabled
- [ ] 设两条 08:00 与 08:59 → 编辑 dialog 报红"太近"，保存按钮 disabled
- [ ] 删完所有项 → 列表回到 EmptyState（带"新增提醒"按钮）
- [ ] Android 13+ 拒绝通知权限 → 设置页弹解释 → 用户仍拒绝 → 提示去系统打开
- [ ] iOS 拒绝授权 → 同上路径
- [ ] 设置页关闭，杀掉 App，到点仍然弹通知
- [ ] **Android 重启手机后**，到点仍然弹通知
- [ ] 已配置启用项 + 系统通知关闭 → 首页顶部出现 banner，点击跳系统设置
- [ ] 通知文案是文案池 10 条之一，多次触发会变化（iOS 是每次启动 App 后变）
- [ ] **桌面端**：设置页**不出现**「通知提醒」入口，编译可过

### 代码质量

- [x] commonTest 覆盖：`ReminderRulesTest` / `ReminderTimeTest` / `ReminderRepositoryTest`（JSON 容错：空 / 损坏 / 未知字段）
- [x] Android `assembleDebug` 通过，无新增 lint warning
- [x] iOS Xcode build 通过
- [x] Desktop JVM 编译 / 测试通过（入口隐藏由 `isReminderSupportedOnPlatform = false` 覆盖）
- [ ] CI 通过

---

## 13. 后续可选扩展（不在本期）

- 节假日 / 工作日跳过（与 AutoRule 的中国法定节假日触发类型联动）
- 自定义文案 / 每条独立文案
- 通知点击直达「记一笔」页（Android Activity intent + iOS notification handler）
- 提醒 dismiss 后短期不再骚扰（snooze）
- 通知统计

---

## 14. 评审决议（已对齐，可开工）

- [x] **数据层**：复用 `PreferenceRepository`，存 JSON 字符串（key = `reminder.schedules`），不新建表
- [x] **冲突规则**：相邻 ≥ 60 分钟用绝对差值，不跨午夜 wrap
- [x] **iOS 文案随机**：接受"启动时重新随机"妥协
- [x] **Android exact alarm 降级**：用户关掉权限后悄悄降级到 inexact，不打扰
- [x] **桌面**：完全隐藏入口（actual 保留 NoOp 仅为编译通过）
- [x] **总开关**：去掉，只保留单条 enable/disable，列表为空 = 没启用任何提醒
- [x] **UX 风格**：完全照抄 `AutomationScreen`（PageHeader 右侧"新增" + EmptyState + 卡片列表）
- [x] **编辑形态**：用 AlertDialog 而非全屏 Screen（表单极简，省一层导航）
