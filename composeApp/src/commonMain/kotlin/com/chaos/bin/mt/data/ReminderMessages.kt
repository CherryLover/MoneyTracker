package com.chaos.bin.mt.data

import kotlin.random.Random

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
        val idx = if (seed >= 0) seed % bodies.size else Random.nextInt(bodies.size)
        return bodies[idx]
    }
}
