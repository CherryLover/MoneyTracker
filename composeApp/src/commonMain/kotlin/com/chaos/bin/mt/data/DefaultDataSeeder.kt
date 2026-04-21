package com.chaos.bin.mt.data

/** 首启时幂等地写入默认分类与账户（参照设计原型里的数据）。 */
class DefaultDataSeeder(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val preferenceRepository: PreferenceRepository,
) {
    private data class SubSeed(val id: String, val name: String, val privacy: Boolean = false)

    private data class CatSeed(
        val id: String,
        val name: String,
        val emoji: String,
        val privacy: Boolean = false,
        val subs: List<SubSeed> = emptyList(),
    )

    suspend fun seedIfEmpty() {
        if (categoryRepository.count() == 0L) seedCategories()
        if (accountRepository.count() == 0L) seedAccounts()
    }

    private suspend fun seedCategories() {
        val expense = listOf(
            CatSeed("food", "餐饮", "\uD83C\uDF5A", subs = listOf(
                SubSeed("food-breakfast", "早餐"),
                SubSeed("food-lunch", "午餐"),
                SubSeed("food-dinner", "晚餐"),
                SubSeed("food-snack", "零食"),
                SubSeed("food-drink", "饮品"),
            )),
            CatSeed("transport", "交通", "\uD83D\uDE87", subs = listOf(
                SubSeed("t-metro", "地铁"),
                SubSeed("t-taxi", "打车"),
                SubSeed("t-fuel", "加油"),
            )),
            CatSeed("shopping", "购物", "\uD83D\uDECD", subs = listOf(
                SubSeed("s-daily", "日用"),
                SubSeed("s-cloth", "服饰"),
                SubSeed("s-digital", "数码"),
            )),
            CatSeed("health", "医疗", "\uD83D\uDC8A", privacy = true, subs = listOf(
                SubSeed("h-medicine", "药品", privacy = true),
                SubSeed("h-hospital", "门诊", privacy = true),
            )),
            CatSeed("home", "居家", "\uD83C\uDFE0", subs = listOf(
                SubSeed("ho-rent", "房租"),
                SubSeed("ho-util", "水电煤"),
                SubSeed("ho-wifi", "宽带"),
            )),
            CatSeed("social", "人情", "\uD83C\uDF81", privacy = true, subs = listOf(
                SubSeed("so-gift", "送礼", privacy = true),
                SubSeed("so-redpacket", "红包", privacy = true),
            )),
            CatSeed("study", "学习", "\uD83D\uDCDA", subs = listOf(
                SubSeed("st-book", "书籍"),
                SubSeed("st-course", "课程"),
            )),
            CatSeed("fun", "娱乐", "\uD83C\uDFAC", subs = listOf(
                SubSeed("f-movie", "电影"),
                SubSeed("f-game", "游戏"),
            )),
        )
        val income = listOf(
            CatSeed("salary", "工资", "\uD83D\uDCBC", subs = listOf(
                SubSeed("sa-base", "月薪"),
                SubSeed("sa-bonus", "奖金"),
            )),
            CatSeed("side", "副业", "\uD83D\uDCA1", subs = listOf(
                SubSeed("si-freelance", "外包"),
            )),
            CatSeed("invest", "理财", "\uD83D\uDCC8", privacy = true, subs = listOf(
                SubSeed("iv-interest", "利息", privacy = true),
                SubSeed("iv-dividend", "分红", privacy = true),
            )),
            CatSeed("other-in", "其他", "\u2728", subs = listOf(
                SubSeed("oi-refund", "退款"),
            )),
        )
        insertTree(RecordKind.Expense, expense)
        insertTree(RecordKind.Income, income)
    }

    private suspend fun insertTree(kind: RecordKind, seeds: List<CatSeed>) {
        seeds.forEachIndexed { catIndex, cat ->
            categoryRepository.insertCategory(
                id = cat.id,
                name = cat.name,
                emoji = cat.emoji,
                kind = kind,
                privacy = cat.privacy,
                sortIndex = catIndex.toLong(),
            )
            cat.subs.forEachIndexed { subIndex, sub ->
                categoryRepository.insertSubCategory(
                    id = sub.id,
                    categoryId = cat.id,
                    name = sub.name,
                    privacy = sub.privacy,
                    sortIndex = subIndex.toLong(),
                )
            }
        }
    }

    private suspend fun seedAccounts() {
        val seeds = listOf(
            Account("alipay", "支付宝", "\uD83C\uDD70"),
            Account("wechat", "微信", "\uD83D\uDCAC"),
            Account("cash", "现金", "\uD83D\uDCB5"),
            Account("card-cmb", "招行储蓄", "\uD83C\uDFE6"),
        )
        seeds.forEachIndexed { i, a ->
            accountRepository.insert(a.id, a.name, a.emoji, i.toLong())
        }
        preferenceRepository.set("default_account_id", seeds.first().id)
    }
}
