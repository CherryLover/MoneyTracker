package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.chaos.bin.mt.theme.LocalAppColors
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * iOS 风格的垂直滚轮选择器。中心 item 最大最亮，离中心越远越弱。
 * 释放后 snap 到最近的 item。
 *
 * @param items 选项列表（通常是数字字符串）
 * @param selectedIndex 当前选中下标
 * @param onSelected 滚动停稳后回调
 * @param itemHeight 单个 item 高度
 * @param visibleCount 可见 item 数（必须是奇数，推荐 5）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 36.dp,
    visibleCount: Int = 5,
) {
    require(visibleCount % 2 == 1) { "visibleCount must be odd" }
    val c = LocalAppColors.current
    val padding = visibleCount / 2
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val snapBehavior = rememberSnapFlingBehavior(listState)

    // 外部 selectedIndex 变化时滚过去（编程式）
    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex || listState.firstVisibleItemScrollOffset != 0) {
            if (selectedIndex in items.indices) {
                listState.scrollToItem(selectedIndex)
            }
        }
    }

    // 连续浮点位置：用于 item 缩放/淡出计算
    val scrollPosition by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset / itemHeightPx
        }
    }

    // 停止滚动时上报当前中心下标
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val idx = kotlin.math.round(scrollPosition).toInt().coerceIn(0, items.size - 1)
                    if (idx != selectedIndex) onSelected(idx)
                }
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
    ) {
        // 中间高亮底色
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(c.subtle, RoundedCornerShape(8.dp)),
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * padding),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, item ->
                val distance = kotlin.math.abs(index - scrollPosition)
                val clamped = distance.coerceAtMost(padding.toFloat())
                val alpha = (1f - clamped * 0.32f).coerceAtLeast(0.15f)
                val fontSize = (18f - clamped * 3f).coerceAtLeast(12f).sp
                val weight = if (distance < 0.5f) FontWeight.SemiBold else FontWeight.Normal
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item,
                        color = c.text.copy(alpha = alpha),
                        fontSize = fontSize,
                        fontWeight = weight,
                    )
                }
            }
        }
    }
}
