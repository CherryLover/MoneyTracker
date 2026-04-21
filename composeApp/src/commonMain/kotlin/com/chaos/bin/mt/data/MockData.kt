package com.chaos.bin.mt.data

object MockData {
    val expenseCategories: List<Category> = listOf(
        Category(
            id = "food", name = "餐饮", emoji = "\uD83C\uDF5A",
            subs = listOf(
                SubCategory("food-breakfast", "早餐"),
                SubCategory("food-lunch", "午餐"),
                SubCategory("food-dinner", "晚餐"),
                SubCategory("food-snack", "零食"),
                SubCategory("food-drink", "饮品"),
            ),
        ),
        Category(
            id = "transport", name = "交通", emoji = "\uD83D\uDE87",
            subs = listOf(
                SubCategory("t-metro", "地铁"),
                SubCategory("t-taxi", "打车"),
                SubCategory("t-fuel", "加油"),
            ),
        ),
        Category(
            id = "shopping", name = "购物", emoji = "\uD83D\uDECD",
            subs = listOf(
                SubCategory("s-daily", "日用"),
                SubCategory("s-cloth", "服饰"),
                SubCategory("s-digital", "数码"),
            ),
        ),
        Category(
            id = "health", name = "医疗", emoji = "\uD83D\uDC8A", privacy = true,
            subs = listOf(
                SubCategory("h-medicine", "药品", privacy = true),
                SubCategory("h-hospital", "门诊", privacy = true),
            ),
        ),
        Category(
            id = "home", name = "居家", emoji = "\uD83C\uDFE0",
            subs = listOf(
                SubCategory("ho-rent", "房租"),
                SubCategory("ho-util", "水电煤"),
                SubCategory("ho-wifi", "宽带"),
            ),
        ),
        Category(
            id = "social", name = "人情", emoji = "\uD83C\uDF81", privacy = true,
            subs = listOf(
                SubCategory("so-gift", "送礼", privacy = true),
                SubCategory("so-redpacket", "红包", privacy = true),
            ),
        ),
        Category(
            id = "study", name = "学习", emoji = "\uD83D\uDCDA",
            subs = listOf(
                SubCategory("st-book", "书籍"),
                SubCategory("st-course", "课程"),
            ),
        ),
        Category(
            id = "fun", name = "娱乐", emoji = "\uD83C\uDFAC",
            subs = listOf(
                SubCategory("f-movie", "电影"),
                SubCategory("f-game", "游戏"),
            ),
        ),
    )

    val incomeCategories: List<Category> = listOf(
        Category(
            id = "salary", name = "工资", emoji = "\uD83D\uDCBC",
            subs = listOf(
                SubCategory("sa-base", "月薪"),
                SubCategory("sa-bonus", "奖金"),
            ),
        ),
        Category(
            id = "side", name = "副业", emoji = "\uD83D\uDCA1",
            subs = listOf(SubCategory("si-freelance", "外包")),
        ),
        Category(
            id = "invest", name = "理财", emoji = "\uD83D\uDCC8", privacy = true,
            subs = listOf(
                SubCategory("iv-interest", "利息", privacy = true),
                SubCategory("iv-dividend", "分红", privacy = true),
            ),
        ),
        Category(
            id = "other-in", name = "其他", emoji = "\u2728",
            subs = listOf(SubCategory("oi-refund", "退款")),
        ),
    )

    val accounts: List<Account> = listOf(
        Account("alipay", "支付宝", "\uD83C\uDD70"),
        Account("wechat", "微信", "\uD83D\uDCAC"),
        Account("cash", "现金", "\uD83D\uDCB5"),
        Account("card-cmb", "招行储蓄", "\uD83C\uDFE6"),
    )

    val records: List<DayRecords> = listOf(
        DayRecords(21, "周二", listOf(
            MoneyRecord(1, RecordType.Expense, "餐饮", "午餐", "\uD83C\uDF5A", 38, "公司楼下沙县", "微信", "12:24"),
            MoneyRecord(2, RecordType.Expense, "交通", "地铁", "\uD83D\uDE87", 6, "", "支付宝", "09:12"),
            MoneyRecord(3, RecordType.Expense, "医疗", "药品", "\uD83D\uDC8A", 128, "感冒药", "微信", "19:40", privacy = true),
        )),
        DayRecords(20, "周一", listOf(
            MoneyRecord(4, RecordType.Income, "工资", "月薪", "\uD83D\uDCBC", 18500, "4 月工资", "招行储蓄", "10:00"),
            MoneyRecord(5, RecordType.Expense, "居家", "房租", "\uD83C\uDFE0", 4800, "4 月房租", "招行储蓄", "10:12"),
            MoneyRecord(6, RecordType.Expense, "餐饮", "晚餐", "\uD83C\uDF5A", 86, "和朋友吃饭", "微信", "20:30"),
        )),
        DayRecords(18, "周六", listOf(
            MoneyRecord(7, RecordType.Expense, "购物", "服饰", "\uD83D\uDECD", 429, "春装一件", "支付宝", "15:45"),
            MoneyRecord(8, RecordType.Expense, "娱乐", "电影", "\uD83C\uDFAC", 80, "两张票", "微信", "19:10"),
            MoneyRecord(9, RecordType.Expense, "人情", "红包", "\uD83C\uDF81", 520, "同事结婚", "微信", "12:00", privacy = true),
        )),
        DayRecords(15, "周三", listOf(
            MoneyRecord(10, RecordType.Expense, "学习", "课程", "\uD83D\uDCDA", 299, "设计课月费", "支付宝", "08:30"),
            MoneyRecord(11, RecordType.Expense, "餐饮", "饮品", "\uD83C\uDF5A", 22, "拿铁", "微信", "14:20"),
        )),
        DayRecords(12, "周日", listOf(
            MoneyRecord(12, RecordType.Income, "副业", "外包", "\uD83D\uDCA1", 3200, "一个小网站", "支付宝", "22:10"),
            MoneyRecord(13, RecordType.Expense, "餐饮", "晚餐", "\uD83C\uDF5A", 156, "家庭聚餐", "现金", "19:00"),
        )),
        DayRecords(8, "周三", listOf(
            MoneyRecord(14, RecordType.Expense, "居家", "水电煤", "\uD83C\uDFE0", 238, "", "招行储蓄", "09:00"),
            MoneyRecord(15, RecordType.Expense, "交通", "加油", "\uD83D\uDE87", 320, "", "招行储蓄", "18:40"),
        )),
        DayRecords(3, "周五", listOf(
            MoneyRecord(16, RecordType.Income, "理财", "利息", "\uD83D\uDCC8", 462, "货币基金", "招行储蓄", "00:00", privacy = true),
            MoneyRecord(17, RecordType.Expense, "购物", "日用", "\uD83D\uDECD", 78, "", "支付宝", "20:15"),
        )),
    )

    val autoRules: List<AutoRule> = listOf(
        AutoRule(1, "房租", "每月 1 号 09:00", 4800, RecordType.Expense, "居家 · 房租", "招行储蓄", enabled = true),
        AutoRule(2, "月薪", "每月 10 号 10:00", 18500, RecordType.Income, "工资 · 月薪", "招行储蓄", enabled = true),
        AutoRule(3, "通勤地铁", "每周一至周五 09:00", 6, RecordType.Expense, "交通 · 地铁", "微信", enabled = true),
        AutoRule(4, "设计课", "每月第 2 个周日 08:30", 299, RecordType.Expense, "学习 · 课程", "支付宝", enabled = false),
    )

    fun sumMonth(): Triple<Int, Int, Int> {
        var exp = 0
        var inc = 0
        for (day in records) for (it in day.items) {
            if (it.type == RecordType.Expense) exp += it.amount else inc += it.amount
        }
        return Triple(exp, inc, inc - exp)
    }
}
