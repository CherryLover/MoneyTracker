# 功能清单

记录项目已实现的功能、入口、流程和关键文件。

## 本地通知提醒
- **标签**: kmp, compose, notification, android, ios, desktop, reminder
- **描述**: 在设置页配置最多 3 条每日本地记账提醒，Android/iOS 使用系统本地通知，Desktop 隐藏入口。
- **入口**: 设置 → 通知提醒；首页在已启用提醒但通知未授权时显示提示 banner。
- **核心流程**:
  1. 提醒配置存入 PreferenceRepository 的 reminder.schedules JSON，不新增 SQLDelight 表。
  2. NotificationsScreen 复用 AutomationScreen 风格，支持新增、编辑、删除、单条启停，并实时校验最多 3 条与相邻至少 60 分钟。
  3. ViewModel 保存后调用 NotificationScheduler.rescheduleAll() 重新排期，App 启动时也会重新排期。
  4. Android 使用 AlarmManager + BroadcastReceiver 发通知，并在 BOOT_COMPLETED / MY_PACKAGE_REPLACED 后重排；Android 12+ 无 exact alarm 权限时降级 set()。
  5. iOS 使用 UNUserNotificationCenter 排每日 calendar trigger；Desktop actual 为 NoOp 且入口通过 isReminderSupportedOnPlatform 隐藏。
- **关键文件**:
  - `docs/notification-reminder-plan.md` - 本地通知提醒实施计划与验收清单
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/Reminder.kt` - 提醒数据模型、数量/间隔规则和时间格式化
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderRepository.kt` - 提醒配置 JSON over PreferenceRepository
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/ReminderTime.kt` - 下一次触发时间计算，按本地日期加一天规避 DST 24h 偏移
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.kt` - 跨平台通知排期 expect 抽象
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/data/NotificationPermission.kt` - 跨平台通知权限 expect 抽象
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/NotificationsScreen.kt` - 通知提醒列表、权限解释、编辑 dialog
  - `composeApp/src/commonMain/kotlin/com/chaos/bin/mt/ui/settings/NotificationsViewModel.kt` - 通知提醒 UI 状态、校验、保存后重排
  - `composeApp/src/androidMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.android.kt` - Android AlarmManager 本地通知排期
  - `composeApp/src/androidMain/kotlin/com/chaos/bin/mt/notif/ReminderReceiver.kt` - Android 到点发通知并安排下一次
  - `composeApp/src/androidMain/kotlin/com/chaos/bin/mt/notif/BootReceiver.kt` - Android 重启/升级后恢复提醒
  - `composeApp/src/iosMain/kotlin/com/chaos/bin/mt/data/NotificationScheduler.ios.kt` - iOS UNUserNotificationCenter 本地通知排期
  - `iosApp/iosApp/AppDelegate.swift` - iOS 前台通知展示代理
- **备注**: 已通过 jvmTest、Android lintDebug/assembleDebug、iOS KMP framework link 与 Xcode simulator build。真机到点通知、权限拒绝路径和 Android 重启恢复仍需手工验收。
