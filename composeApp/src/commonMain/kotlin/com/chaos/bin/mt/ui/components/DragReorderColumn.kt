package com.chaos.bin.mt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex

/**
 * 简单的拖拽重排 Column。列表较短时用。
 *
 * 动态反馈：被拖项实时跟手指移动；其它项按被拖项当前投影位置平滑让位；
 * 松手时乐观地本地应用新顺序（不等 VM 回流），并通过 [onReorder] 把新顺序发出去。
 *
 * [handleModifier] 必须应用在 item 内部的"拖拽锚点"（比如 Grip 图标）上。
 */
@Composable
fun <T> DragReorderColumn(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    onReorder: (List<T>) -> Unit,
    item: @Composable (value: T, isDragging: Boolean, handleModifier: Modifier) -> Unit,
) {
    // 外部 items 变动时重建下面这些 state，重新对齐 props
    val positions = remember(items) { mutableStateMapOf<Any, Float>() }
    val heights = remember(items) { mutableStateMapOf<Any, Float>() }
    var pendingOrder by remember(items) { mutableStateOf<List<T>?>(null) }
    var dragKey by remember(items) { mutableStateOf<Any?>(null) }
    var dragOffsetY by remember(items) { mutableStateOf(0f) }

    // 本地视图用的顺序：刚松手时 pendingOrder 先生效，等 VM 回流到达会重建并回到 items
    val displayItems: List<T> = pendingOrder ?: items

    val originalIndex: Int = dragKey?.let { k -> displayItems.indexOfFirst { key(it) == k } } ?: -1
    val draggedHeight: Float = dragKey?.let { heights[it] } ?: 0f

    val projectedIndex: Int = run {
        if (originalIndex < 0) return@run -1
        val k = dragKey!!
        val origTop = positions[k] ?: 0f
        val myCenter = origTop + dragOffsetY + draggedHeight / 2f
        var idx = displayItems.lastIndex
        for (i in displayItems.indices) {
            if (i == originalIndex) continue
            val otherKey = key(displayItems[i])
            val top = positions[otherKey] ?: continue
            val h = heights[otherKey] ?: continue
            if (myCenter < top + h / 2f) {
                idx = if (i > originalIndex) i - 1 else i
                break
            }
        }
        idx.coerceIn(0, displayItems.lastIndex)
    }

    Column(modifier) {
        displayItems.forEachIndexed { i, value ->
            val k = key(value)
            val isDragging = k == dragKey
            val isIdle = dragKey == null

            val targetOffset: Float = when {
                isDragging -> dragOffsetY
                isIdle -> 0f
                i in (originalIndex + 1)..projectedIndex -> -draggedHeight
                i in projectedIndex until originalIndex -> draggedHeight
                else -> 0f
            }
            // 拖动时让位走 tween（丝滑），其它情况（被拖项跟手、空闲）用 snap 立即到位
            val animatedOffset by animateFloatAsState(
                targetValue = targetOffset,
                animationSpec = if (isDragging || isIdle) snap() else tween(durationMillis = 180),
                label = "reorder-offset",
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        if (isIdle) {
                            positions[k] = coords.positionInParent().y
                            heights[k] = coords.size.height.toFloat()
                        }
                    }
                    .graphicsLayer { translationY = animatedOffset }
                    .zIndex(if (isDragging) 1f else 0f),
            ) {
                val handle = Modifier.pointerInput(k, displayItems) {
                    detectDragGestures(
                        onDragStart = {
                            dragKey = k
                            dragOffsetY = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            dragOffsetY += drag.y
                        },
                        onDragEnd = {
                            val current = dragKey
                            val list = pendingOrder ?: items
                            val from = if (current == null) -1 else list.indexOfFirst { key(it) == current }
                            val to = if (current == null || from < 0) {
                                -1
                            } else {
                                val top = positions[current] ?: 0f
                                val h = heights[current] ?: 0f
                                val myCenter = top + dragOffsetY + h / 2f
                                var idx = list.lastIndex
                                for (i in list.indices) {
                                    if (i == from) continue
                                    val otherKey = key(list[i])
                                    val otherTop = positions[otherKey] ?: continue
                                    val otherH = heights[otherKey] ?: continue
                                    if (myCenter < otherTop + otherH / 2f) {
                                        idx = if (i > from) i - 1 else i
                                        break
                                    }
                                }
                                idx.coerceIn(0, list.lastIndex)
                            }
                            if (from >= 0 && to >= 0 && to != from) {
                                val newList = list.toMutableList()
                                val taken = newList.removeAt(from)
                                newList.add(to, taken)
                                pendingOrder = newList
                                onReorder(newList)
                            }
                            dragKey = null
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragKey = null
                            dragOffsetY = 0f
                        },
                    )
                }
                item(value, isDragging, handle)
            }
        }
    }
}
