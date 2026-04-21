package com.chaos.bin.mt.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * 对应 icons.jsx 中的线条图标。统一 24x24 viewport、1.6 描边、round cap/join。
 * 填充路径（如 grip）走 fill，描边路径走 stroke。
 */
object LineIcons {

    private val Stroke = 1.6f
    private val None = Color(0x00000000)

    private fun build(
        name: String,
        block: PathBuilder.() -> Unit,
        fill: Boolean = false,
        strokeWidth: Float = Stroke,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathData(block),
            fill = if (fill) SolidColor(Color.Black) else null,
            stroke = if (fill) null else SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        )
    }.build()

    private fun multi(
        name: String,
        paths: List<MultiPath>,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        for (p in paths) {
            addPath(
                pathData = androidx.compose.ui.graphics.vector.PathData(p.block),
                fill = if (p.fill) SolidColor(Color.Black) else null,
                stroke = if (p.fill) null else SolidColor(Color.Black),
                strokeLineWidth = p.strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            )
        }
    }.build()

    data class MultiPath(
        val fill: Boolean = false,
        val strokeWidth: Float = 1.6f,
        val block: PathBuilder.() -> Unit,
    )

    val Home: ImageVector = build("Home", {
        // M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z
        moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
        verticalLineTo(20f); arcTo(1f, 1f, 0f, false, true, 20f, 21f)
        horizontalLineTo(15f); verticalLineTo(14f)
        horizontalLineTo(9f); verticalLineTo(21f)
        horizontalLineTo(4f); arcTo(1f, 1f, 0f, false, true, 3f, 20f)
        close()
    })

    val Plus: ImageVector = multi("Plus", listOf(
        MultiPath { moveTo(12f, 5f); verticalLineTo(19f) },
        MultiPath { moveTo(5f, 12f); horizontalLineTo(19f) },
    ))

    /** 设置齿轮 —— 贴合 icons.jsx 的 path（简化为贴近原型的近似）。 */
    val Cog: ImageVector = multi("Cog", listOf(
        MultiPath {
            // circle cx=12 cy=12 r=3
            moveTo(15f, 12f)
            arcTo(3f, 3f, 0f, true, true, 9f, 12f)
            arcTo(3f, 3f, 0f, true, true, 15f, 12f)
            close()
        },
        MultiPath {
            // 原 svg 的齿轮外环路径较长，用等价闭合轮廓
            moveTo(19.4f, 15f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 0.3f, 1.8f)
            lineToRelative(0.1f, 0.1f)
            arcToRelative(2f, 2f, 0f, true, true, -2.8f, 2.8f)
            lineToRelative(-0.1f, -0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1.8f, -0.3f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1f, 1.5f)
            verticalLineTo(21f)
            arcToRelative(2f, 2f, 0f, false, true, -4f, 0f)
            verticalLineToRelative(-0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1f, -1.5f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1.8f, 0.3f)
            lineToRelative(-0.1f, 0.1f)
            arcToRelative(2f, 2f, 0f, true, true, -2.8f, -2.8f)
            lineToRelative(0.1f, -0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 0.3f, -1.8f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1.5f, -1f)
            horizontalLineTo(3f)
            arcToRelative(2f, 2f, 0f, false, true, 0f, -4f)
            horizontalLineToRelative(0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1.5f, -1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -0.3f, -1.8f)
            lineToRelative(-0.1f, -0.1f)
            arcToRelative(2f, 2f, 0f, true, true, 2.8f, -2.8f)
            lineToRelative(0.1f, 0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1.8f, 0.3f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1f, -1.5f)
            verticalLineTo(3f)
            arcToRelative(2f, 2f, 0f, false, true, 4f, 0f)
            verticalLineToRelative(0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1f, 1.5f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1.8f, -0.3f)
            lineToRelative(0.1f, -0.1f)
            arcToRelative(2f, 2f, 0f, true, true, 2.8f, 2.8f)
            lineToRelative(-0.1f, 0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -0.3f, 1.8f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, 1.5f, 1f)
            horizontalLineTo(21f)
            arcToRelative(2f, 2f, 0f, false, true, 0f, 4f)
            horizontalLineToRelative(-0.1f)
            arcToRelative(1.7f, 1.7f, 0f, false, false, -1.5f, 1f)
            close()
        },
    ))

    val ChevL: ImageVector = build("ChevL", {
        moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
    })

    val ChevR: ImageVector = build("ChevR", {
        moveTo(9f, 18f); lineTo(15f, 12f); lineTo(9f, 6f)
    })

    val ChevD: ImageVector = build("ChevD", {
        moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
    })

    val Lock: ImageVector = multi("Lock", listOf(
        MultiPath {
            // rect x=4 y=11 w=16 h=10 rx=2
            moveTo(6f, 11f); horizontalLineTo(18f)
            arcTo(2f, 2f, 0f, false, true, 20f, 13f)
            verticalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 18f, 21f)
            horizontalLineTo(6f)
            arcTo(2f, 2f, 0f, false, true, 4f, 19f)
            verticalLineTo(13f)
            arcTo(2f, 2f, 0f, false, true, 6f, 11f)
            close()
        },
        MultiPath {
            // M8 11V7a4 4 0 1 1 8 0v4
            moveTo(8f, 11f)
            verticalLineTo(7f)
            arcTo(4f, 4f, 0f, true, true, 16f, 7f)
            verticalLineTo(11f)
        },
    ))

    val Clock: ImageVector = multi("Clock", listOf(
        MultiPath {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, true, true, 3f, 12f)
            arcTo(9f, 9f, 0f, true, true, 21f, 12f)
            close()
        },
        MultiPath { moveTo(12f, 7f); verticalLineTo(12f); lineTo(15f, 14f) },
    ))

    val Cal: ImageVector = multi("Cal", listOf(
        MultiPath {
            // rect x=3 y=5 w=18 h=16 rx=2
            moveTo(5f, 5f); horizontalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 21f, 7f)
            verticalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 19f, 21f)
            horizontalLineTo(5f)
            arcTo(2f, 2f, 0f, false, true, 3f, 19f)
            verticalLineTo(7f)
            arcTo(2f, 2f, 0f, false, true, 5f, 5f)
            close()
        },
        MultiPath { moveTo(3f, 10f); horizontalLineTo(21f) },
        MultiPath { moveTo(8f, 3f); verticalLineTo(7f) },
        MultiPath { moveTo(16f, 3f); verticalLineTo(7f) },
    ))

    val Wallet: ImageVector = multi("Wallet", listOf(
        MultiPath {
            // M3 7a2 2 0 0 1 2-2h13v4
            moveTo(3f, 7f)
            arcTo(2f, 2f, 0f, false, true, 5f, 5f)
            horizontalLineTo(18f)
            verticalLineTo(9f)
        },
        MultiPath {
            // rect x=3 y=7 w=18 h=13 rx=2
            moveTo(5f, 7f); horizontalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 21f, 9f)
            verticalLineTo(18f)
            arcTo(2f, 2f, 0f, false, true, 19f, 20f)
            horizontalLineTo(5f)
            arcTo(2f, 2f, 0f, false, true, 3f, 18f)
            verticalLineTo(9f)
            arcTo(2f, 2f, 0f, false, true, 5f, 7f)
            close()
        },
        MultiPath(fill = true) {
            moveTo(18.2f, 13.5f)
            arcTo(1.2f, 1.2f, 0f, true, true, 15.8f, 13.5f)
            arcTo(1.2f, 1.2f, 0f, true, true, 18.2f, 13.5f)
            close()
        },
    ))

    val Repeat: ImageVector = multi("Repeat", listOf(
        MultiPath { moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f) },
        MultiPath {
            moveTo(3f, 11f); verticalLineTo(10f)
            arcTo(4f, 4f, 0f, false, true, 7f, 6f)
            horizontalLineTo(21f)
        },
        MultiPath { moveTo(7f, 22f); lineTo(3f, 18f); lineTo(7f, 14f) },
        MultiPath {
            moveTo(21f, 13f); verticalLineTo(14f)
            arcTo(4f, 4f, 0f, false, true, 17f, 18f)
            horizontalLineTo(3f)
        },
    ))

    val Check: ImageVector = build(
        "Check",
        { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) },
        strokeWidth = 2f,
    )

    val Close: ImageVector = multi("Close", listOf(
        MultiPath { moveTo(6f, 6f); lineTo(18f, 18f) },
        MultiPath { moveTo(18f, 6f); lineTo(6f, 18f) },
    ))

    val Grip: ImageVector = multi("Grip", listOf(
        MultiPath(fill = true) { moveTo(10.3f, 6f); arcTo(1.3f, 1.3f, 0f, true, true, 7.7f, 6f); arcTo(1.3f, 1.3f, 0f, true, true, 10.3f, 6f); close() },
        MultiPath(fill = true) { moveTo(16.3f, 6f); arcTo(1.3f, 1.3f, 0f, true, true, 13.7f, 6f); arcTo(1.3f, 1.3f, 0f, true, true, 16.3f, 6f); close() },
        MultiPath(fill = true) { moveTo(10.3f, 12f); arcTo(1.3f, 1.3f, 0f, true, true, 7.7f, 12f); arcTo(1.3f, 1.3f, 0f, true, true, 10.3f, 12f); close() },
        MultiPath(fill = true) { moveTo(16.3f, 12f); arcTo(1.3f, 1.3f, 0f, true, true, 13.7f, 12f); arcTo(1.3f, 1.3f, 0f, true, true, 16.3f, 12f); close() },
        MultiPath(fill = true) { moveTo(10.3f, 18f); arcTo(1.3f, 1.3f, 0f, true, true, 7.7f, 18f); arcTo(1.3f, 1.3f, 0f, true, true, 10.3f, 18f); close() },
        MultiPath(fill = true) { moveTo(16.3f, 18f); arcTo(1.3f, 1.3f, 0f, true, true, 13.7f, 18f); arcTo(1.3f, 1.3f, 0f, true, true, 16.3f, 18f); close() },
    ))

    val Edit: ImageVector = multi("Edit", listOf(
        MultiPath { moveTo(4f, 20f); horizontalLineTo(8f); lineTo(18f, 10f); lineTo(14f, 6f); lineTo(4f, 16f); close() },
        MultiPath { moveTo(14f, 6f); lineTo(18f, 10f) },
    ))

    val Back: ImageVector = build("Back", {
        moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
    })

    /** Del / 键盘退格键 */
    val Del: ImageVector = multi("Del", listOf(
        MultiPath {
            moveTo(21f, 5f); horizontalLineTo(8f); lineTo(3f, 12f); lineTo(8f, 19f); horizontalLineTo(21f)
            arcTo(1f, 1f, 0f, false, false, 22f, 18f)
            verticalLineTo(6f)
            arcTo(1f, 1f, 0f, false, false, 21f, 5f)
            close()
        },
        MultiPath { moveTo(12f, 9f); lineTo(17f, 15f) },
        MultiPath { moveTo(17f, 9f); lineTo(12f, 15f) },
    ))
}
