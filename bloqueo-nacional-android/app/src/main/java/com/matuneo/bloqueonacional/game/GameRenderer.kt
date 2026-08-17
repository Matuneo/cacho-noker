package com.matuneo.bloqueonacional.game

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.min
import kotlin.math.sin

private val Ink = Color(0xFF071522)
private val Cream = Color(0xFFFFF4D6)
private val Gold = Color(0xFFFFD54F)
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFE53935)
private val Blue = Color(0xFF244A7C)

@Composable
fun GameBoard(
    state: GameState,
    onCellTapped: (row: Int, column: Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .pointerInput(state.mode, state.selected) {
                detectTapGestures { point ->
                    val factor = min(size.width / Board.WIDTH, size.height / Board.HEIGHT)
                    val left = (size.width - Board.WIDTH * factor) / 2f
                    val top = (size.height - Board.HEIGHT * factor) / 2f
                    val logicalX = (point.x - left) / factor
                    val logicalY = (point.y - top) / factor
                    val column = ((logicalX - Board.START_X) / Board.CELL_WIDTH).toInt()
                    val row = ((logicalY - Board.TOP_Y) / Board.LANE_HEIGHT).toInt()
                    if (logicalX >= Board.START_X && logicalY >= Board.TOP_Y) {
                        onCellTapped(row, column)
                    }
                }
            }
    ) {
        val factor = min(size.width / Board.WIDTH, size.height / Board.HEIGHT)
        val left = (size.width - Board.WIDTH * factor) / 2f
        val top = (size.height - Board.HEIGHT * factor) / 2f
        withTransform({
            translate(left, top)
            scale(factor, factor, Offset.Zero)
        }) {
            drawWorld(state)
        }
    }
}

@Composable
fun MenuBackdrop(elapsed: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Ink)) {
        val factor = min(size.width / Board.WIDTH, size.height / Board.HEIGHT)
        val left = (size.width - Board.WIDTH * factor) / 2f
        val top = (size.height - Board.HEIGHT * factor) / 2f
        withTransform({
            translate(left, top)
            scale(factor, factor, Offset.Zero)
        }) {
            drawBackdrop(GameContent.levels.first(), elapsed, false)
            drawDefender(Defender(1, DefenderType.CHOLITA, 0, 0), 450f, 735f, 1.8f)
            drawDefender(Defender(2, DefenderType.DRUMMER, 0, 0), 710f, 760f, 1.65f)
            drawCapy(Enemy(3, EnemyType.SHIELD, 0, 1335f), 1335f, 755f, 1.75f)
            drawCapy(Enemy(4, EnemyType.COMMANDER, 0, 1580f), 1580f, 720f, 1.9f)
        }
    }
}

private fun DrawScope.drawWorld(state: GameState) {
    drawBackdrop(state.level, state.elapsed, true)

    state.defenders.sortedBy { it.row }.forEach { defender ->
        val x = Board.defenderX(defender.column)
        val y = Board.laneY(defender.row) + 18f
        drawDefender(defender, x, y, 1f)
        drawHealth(x - 58f, y - 91f, 116f, defender.health / defender.type.health, Color(0xFF4CAF50))
    }

    state.enemies.sortedBy { it.row }.forEach { enemy ->
        val y = Board.laneY(enemy.row) + 20f + sin((state.elapsed * 5f + enemy.id)) * 3f
        drawCapy(enemy, enemy.x, y, 1f)
        drawHealth(enemy.x - 56f, y - 93f, 112f, enemy.health / enemy.type.health, Color(0xFFE8564F))
        if (enemy.slowTimer > 0f) {
            drawCircle(Color(0x334DD0E1), 68f, Offset(enemy.x, y - 28f), style = Stroke(5f))
        }
    }

    state.projectiles.forEach { projectile ->
        val y = Board.laneY(projectile.row) - 10f
        when (projectile.kind) {
            ProjectileKind.PAPER -> rotate(10f, Offset(projectile.x, y)) {
                drawRoundRect(Color.White, Offset(projectile.x - 18f, y - 12f), Size(36f, 24f), CornerRadius(4f))
                drawLine(Red, Offset(projectile.x - 11f, y - 5f), Offset(projectile.x + 10f, y - 5f), 3f)
                drawLine(Green, Offset(projectile.x - 11f, y + 3f), Offset(projectile.x + 6f, y + 3f), 3f)
            }
            ProjectileKind.SOUND -> {
                drawCircle(Color(0x5539C6D7), 27f, Offset(projectile.x, y), style = Stroke(8f))
                drawCircle(Color(0xAA80DEEA), 11f, Offset(projectile.x, y), style = Stroke(5f))
            }
            ProjectileKind.SLOGAN -> {
                val path = Path().apply {
                    moveTo(projectile.x - 24f, y - 18f)
                    lineTo(projectile.x + 28f, y)
                    lineTo(projectile.x - 24f, y + 18f)
                    close()
                }
                drawPath(path, Color(0xFFE1BEE7))
                drawCircle(Gold, 6f, Offset(projectile.x + 16f, y))
            }
            ProjectileKind.CONFETTI -> {
                drawCircle(Gold, 10f, Offset(projectile.x, y))
                drawCircle(Red, 6f, Offset(projectile.x - 15f, y + 9f))
                drawCircle(Color(0xFF43A047), 7f, Offset(projectile.x + 13f, y - 12f))
            }
        }
    }

    drawTextHighRes(
        state.level.location.uppercase(),
        34f,
        208f,
        27f,
        Color(0xCCFFFFFF),
        NativePaint.Align.LEFT,
        Typeface.BOLD
    )
}

private fun DrawScope.drawBackdrop(level: LevelSpec, elapsed: Float, showGrid: Boolean) {
    drawRect(
        Brush.verticalGradient(listOf(Color(level.skyTop), Color(level.skyBottom))),
        size = Size(Board.WIDTH, Board.HEIGHT)
    )
    drawCircle(Color(0x99FFF8D6), 105f, Offset(1640f, 142f))

    val farMountains = Path().apply {
        moveTo(0f, 420f)
        lineTo(270f, 155f)
        lineTo(520f, 410f)
        lineTo(760f, 210f)
        lineTo(1050f, 420f)
        lineTo(1340f, 175f)
        lineTo(1710f, 430f)
        lineTo(Board.WIDTH, 260f)
        lineTo(Board.WIDTH, 550f)
        lineTo(0f, 550f)
        close()
    }
    drawPath(farMountains, Color(0x886B7C75))

    val nearMountains = Path().apply {
        moveTo(0f, 505f)
        lineTo(330f, 310f)
        lineTo(610f, 505f)
        lineTo(920f, 290f)
        lineTo(1260f, 510f)
        lineTo(1510f, 325f)
        lineTo(Board.WIDTH, 520f)
        lineTo(Board.WIDTH, 650f)
        lineTo(0f, 650f)
        close()
    }
    drawPath(nearMountains, Color(0xFF345C56))

    repeat(10) { index ->
        val buildingX = index * 205f - 30f
        val height = 90f + (index % 4) * 27f
        drawRect(Color(0xFF213D4C), Offset(buildingX, 520f - height), Size(170f, height + 90f))
        repeat(3) { window ->
            drawRect(Color(0x99FFD65A), Offset(buildingX + 25f + window * 47f, 550f - height), Size(22f, 31f))
        }
    }

    drawRect(Color(0xFF315C3C), Offset(0f, 570f), Size(Board.WIDTH, 510f))
    drawRect(Color(0xFF557A49), Offset(0f, Board.TOP_Y), Size(Board.WIDTH, Board.LANE_HEIGHT * Board.LANES))

    if (showGrid) {
        repeat(Board.LANES) { row ->
            val laneTop = Board.TOP_Y + row * Board.LANE_HEIGHT
            drawRect(
                if (row % 2 == 0) Color(0x1AFFFFFF) else Color(0x10000000),
                Offset(120f, laneTop),
                Size(Board.WIDTH - 175f, Board.LANE_HEIGHT)
            )
            drawLine(Color(0x445D8A57), Offset(120f, laneTop), Offset(Board.WIDTH - 55f, laneTop), 3f)
        }
        drawLine(Color(0x665D8A57), Offset(120f, Board.TOP_Y + Board.LANE_HEIGHT * Board.LANES), Offset(Board.WIDTH - 55f, Board.TOP_Y + Board.LANE_HEIGHT * Board.LANES), 3f)

        repeat(Board.COLUMNS) { column ->
            repeat(Board.LANES) { row ->
                drawRoundRect(
                    color = Color(0x18FFFFFF),
                    topLeft = Offset(Board.START_X + column * Board.CELL_WIDTH + 7f, Board.TOP_Y + row * Board.LANE_HEIGHT + 6f),
                    size = Size(Board.CELL_WIDTH - 14f, Board.LANE_HEIGHT - 12f),
                    cornerRadius = CornerRadius(18f),
                    style = Stroke(2f)
                )
            }
        }

        drawCamp(130f, 625f)
        drawPalace(1730f, 430f)
    } else {
        val flagWave = sin(elapsed * 1.8f) * 12f
        drawLine(Cream, Offset(270f, 400f), Offset(270f, 680f), 12f, StrokeCap.Round)
        val flag = Path().apply {
            moveTo(275f, 410f)
            quadraticBezierTo(390f, 390f + flagWave, 510f, 425f)
            lineTo(510f, 560f)
            quadraticBezierTo(390f, 530f + flagWave, 275f, 550f)
            close()
        }
        drawPath(flag, Brush.verticalGradient(listOf(Red, Gold, Color(0xFF43A047))))
    }

    drawRect(Color(0xC90A1827), Offset(0f, 0f), Size(Board.WIDTH, 190f))
    drawRect(Color(0xD9071522), Offset(0f, 950f), Size(Board.WIDTH, 130f))
}

private fun DrawScope.drawCamp(x: Float, y: Float) {
    drawRoundRect(Color(0xFF6D4C41), Offset(x - 55f, y + 25f), Size(110f, 25f), CornerRadius(10f))
    repeat(3) { i ->
        drawCircle(Color(0xFF202124), 27f, Offset(x - 36f + i * 36f, y + 18f), style = Stroke(12f))
    }
    drawLine(Cream, Offset(x - 65f, y - 85f), Offset(x - 65f, y + 10f), 8f)
    drawRoundRect(Red, Offset(x - 60f, y - 82f), Size(132f, 58f), CornerRadius(8f))
    drawTextHighRes("CAMPAMENTO", x + 6f, y - 46f, 17f, Color.White, NativePaint.Align.CENTER, Typeface.BOLD)
}

private fun DrawScope.drawPalace(x: Float, y: Float) {
    drawRect(Color(0xFFEEE4C7), Offset(x - 105f, y), Size(205f, 135f))
    drawPath(Path().apply {
        moveTo(x - 130f, y)
        lineTo(x, y - 75f)
        lineTo(x + 130f, y)
        close()
    }, Color(0xFFB23B34))
    repeat(4) { i ->
        drawRect(Color(0xFF234A6B), Offset(x - 82f + i * 48f, y + 31f), Size(25f, 46f))
    }
    drawRect(Color(0xFF6D4C41), Offset(x - 25f, y + 76f), Size(50f, 59f))
}

private fun DrawScope.drawDefender(defender: Defender, x: Float, y: Float, scale: Float) {
    withTransform({ scale(scale, scale, Offset(x, y)) }) {
        drawOval(Color(0x33000000), Offset(x - 66f, y + 43f), Size(132f, 27f))
        when (defender.type) {
            DefenderType.API -> drawApiVendor(x, y)
            DefenderType.BLOCKER -> drawBlocker(x, y)
            DefenderType.STUDENT -> drawStudent(x, y)
            DefenderType.DRUMMER -> drawDrummer(x, y)
            DefenderType.CHOLITA -> drawCholita(x, y)
            DefenderType.MEGAPHONE -> drawLeader(x, y)
        }
    }
}

private fun DrawScope.drawPersonBase(x: Float, y: Float, shirt: Color, skin: Color = Color(0xFFC68662)) {
    drawCircle(skin, 28f, Offset(x, y - 53f))
    drawRoundRect(shirt, Offset(x - 35f, y - 26f), Size(70f, 74f), CornerRadius(22f))
    drawLine(Ink, Offset(x - 17f, y + 42f), Offset(x - 24f, y + 65f), 12f, StrokeCap.Round)
    drawLine(Ink, Offset(x + 17f, y + 42f), Offset(x + 24f, y + 65f), 12f, StrokeCap.Round)
    drawCircle(Ink, 4f, Offset(x - 9f, y - 58f))
    drawCircle(Ink, 4f, Offset(x + 9f, y - 58f))
}

private fun DrawScope.drawApiVendor(x: Float, y: Float) {
    drawPersonBase(x, y, Color(0xFFD84315))
    drawOval(Ink, Offset(x - 37f, y - 90f), Size(74f, 17f))
    drawRoundRect(Ink, Offset(x - 25f, y - 102f), Size(50f, 23f), CornerRadius(12f))
    drawRoundRect(Color(0xFF8D6E63), Offset(x + 31f, y - 18f), Size(57f, 66f), CornerRadius(12f))
    drawOval(Gold, Offset(x + 38f, y - 28f), Size(44f, 20f))
    drawTextHighRes("API", x + 60f, y + 20f, 16f, Cream, NativePaint.Align.CENTER, Typeface.BOLD)
}

private fun DrawScope.drawBlocker(x: Float, y: Float) {
    drawPersonBase(x - 6f, y - 2f, Color(0xFF455A64))
    repeat(3) { i ->
        drawCircle(Color(0xFF15181B), 35f, Offset(x - 46f + i * 46f, y + 35f), style = Stroke(15f))
    }
    drawRoundRect(Red, Offset(x - 77f, y + 15f), Size(154f, 24f), CornerRadius(8f))
    drawLine(Gold, Offset(x - 55f, y + 17f), Offset(x - 30f, y + 38f), 8f)
    drawLine(Gold, Offset(x + 8f, y + 17f), Offset(x + 33f, y + 38f), 8f)
}

private fun DrawScope.drawStudent(x: Float, y: Float) {
    drawPersonBase(x, y, Color(0xFF00838F))
    drawRect(Color.White, Offset(x + 20f, y - 88f), Size(75f, 53f))
    drawLine(Color(0xFF6D4C41), Offset(x + 31f, y - 37f), Offset(x + 12f, y + 35f), 7f)
    drawTextHighRes("UNIDAD", x + 57f, y - 55f, 13f, Red, NativePaint.Align.CENTER, Typeface.BOLD)
}

private fun DrawScope.drawDrummer(x: Float, y: Float) {
    drawPersonBase(x, y, Color(0xFF7B1FA2))
    drawCircle(Color(0xFFFFC107), 41f, Offset(x + 25f, y + 15f))
    drawCircle(Color(0xFF5D4037), 41f, Offset(x + 25f, y + 15f), style = Stroke(7f))
    drawLine(Cream, Offset(x - 18f, y - 12f), Offset(x + 36f, y + 8f), 7f, StrokeCap.Round)
}

private fun DrawScope.drawCholita(x: Float, y: Float) {
    val skin = Color(0xFFB97754)
    drawCircle(skin, 29f, Offset(x, y - 57f))
    val skirt = Path().apply {
        moveTo(x - 28f, y - 25f)
        lineTo(x - 58f, y + 50f)
        lineTo(x + 58f, y + 50f)
        lineTo(x + 28f, y - 25f)
        close()
    }
    drawPath(skirt, Color(0xFFC2185B))
    drawRoundRect(Color(0xFF3949AB), Offset(x - 33f, y - 30f), Size(66f, 48f), CornerRadius(18f))
    drawOval(Ink, Offset(x - 42f, y - 97f), Size(84f, 18f))
    drawRoundRect(Ink, Offset(x - 27f, y - 111f), Size(54f, 27f), CornerRadius(14f))
    drawLine(Color(0xFF5D4037), Offset(x - 18f, y - 35f), Offset(x - 42f, y + 38f), 8f)
    drawCircle(Ink, 4f, Offset(x - 9f, y - 61f))
    drawCircle(Ink, 4f, Offset(x + 9f, y - 61f))
}

private fun DrawScope.drawLeader(x: Float, y: Float) {
    drawPersonBase(x - 15f, y, Color(0xFFB71C1C))
    val megaphone = Path().apply {
        moveTo(x + 6f, y - 50f)
        lineTo(x + 91f, y - 78f)
        lineTo(x + 91f, y - 18f)
        close()
    }
    drawPath(megaphone, Color(0xFFF5F5F5))
    drawRoundRect(Red, Offset(x - 2f, y - 56f), Size(32f, 27f), CornerRadius(8f))
    drawCircle(Gold, 8f, Offset(x + 79f, y - 48f))
}

private fun DrawScope.drawCapy(enemy: Enemy, x: Float, y: Float, scale: Float) {
    withTransform({ scale(scale, scale, Offset(x, y)) }) {
        drawOval(Color(0x33000000), Offset(x - 76f, y + 43f), Size(152f, 28f))
        drawRoundRect(Color(0xFF8D6E63), Offset(x - 55f, y - 52f), Size(112f, 105f), CornerRadius(44f))
        drawCircle(Color(0xFF9E7B6A), 43f, Offset(x + 43f, y - 55f))
        drawCircle(Color(0xFF6D4C41), 13f, Offset(x + 25f, y - 92f))
        drawCircle(Color(0xFF6D4C41), 13f, Offset(x + 58f, y - 91f))
        drawOval(Color(0xFFBCA395), Offset(x + 35f, y - 59f), Size(55f, 34f))
        drawCircle(Ink, 5f, Offset(x + 69f, y - 48f))
        drawCircle(Ink, 5f, Offset(x + 48f, y - 66f))
        drawRoundRect(Blue, Offset(x - 57f, y - 12f), Size(112f, 46f), CornerRadius(13f))
        drawLine(Ink, Offset(x - 30f, y + 45f), Offset(x - 38f, y + 66f), 15f, StrokeCap.Round)
        drawLine(Ink, Offset(x + 26f, y + 45f), Offset(x + 35f, y + 66f), 15f, StrokeCap.Round)

        when (enemy.type) {
            EnemyType.PATROL -> {
                drawRoundRect(Color(0xFF263238), Offset(x + 5f, y - 106f), Size(78f, 29f), CornerRadius(18f))
                drawCircle(Color(0xFF90A4AE), 9f, Offset(x + 44f, y - 92f))
            }
            EnemyType.SHIELD -> {
                drawRoundRect(Color(0xFF90A4AE), Offset(x - 82f, y - 73f), Size(56f, 126f), CornerRadius(18f))
                drawCircle(Color(0xFFCFD8DC), 17f, Offset(x - 54f, y - 11f), style = Stroke(6f))
                drawRoundRect(Color(0xFF263238), Offset(x + 5f, y - 106f), Size(78f, 29f), CornerRadius(18f))
            }
            EnemyType.MOTO -> {
                drawCircle(Ink, 31f, Offset(x - 45f, y + 48f), style = Stroke(12f))
                drawCircle(Ink, 31f, Offset(x + 58f, y + 48f), style = Stroke(12f))
                drawLine(Red, Offset(x - 45f, y + 25f), Offset(x + 53f, y + 25f), 14f)
                drawRoundRect(Color(0xFFE0E0E0), Offset(x + 2f, y - 107f), Size(82f, 31f), CornerRadius(17f))
            }
            EnemyType.WATER -> {
                drawRoundRect(Color(0xFF039BE5), Offset(x - 86f, y - 38f), Size(48f, 79f), CornerRadius(17f))
                drawLine(Color(0xFF81D4FA), Offset(x - 40f, y - 29f), Offset(x + 69f, y - 83f), 13f, StrokeCap.Round)
            }
            EnemyType.COMMANDER -> {
                drawRoundRect(Color(0xFF172C4B), Offset(x - 4f, y - 112f), Size(92f, 34f), CornerRadius(17f))
                val star = Path().apply {
                    moveTo(x + 43f, y - 109f)
                    lineTo(x + 50f, y - 95f)
                    lineTo(x + 66f, y - 93f)
                    lineTo(x + 54f, y - 82f)
                    lineTo(x + 57f, y - 67f)
                    lineTo(x + 43f, y - 75f)
                    lineTo(x + 29f, y - 67f)
                    lineTo(x + 32f, y - 82f)
                    lineTo(x + 20f, y - 93f)
                    lineTo(x + 36f, y - 95f)
                    close()
                }
                drawPath(star, Gold)
                drawLine(Gold, Offset(x - 42f, y - 1f), Offset(x + 43f, y - 1f), 7f)
            }
        }
    }
}

private fun DrawScope.drawHealth(x: Float, y: Float, width: Float, ratio: Float, color: Color) {
    drawRoundRect(Color(0xAA071522), Offset(x, y), Size(width, 11f), CornerRadius(6f))
    drawRoundRect(color, Offset(x + 2f, y + 2f), Size((width - 4f) * ratio.coerceIn(0f, 1f), 7f), CornerRadius(4f))
}

private fun DrawScope.drawTextHighRes(
    value: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    alignment: NativePaint.Align = NativePaint.Align.LEFT,
    style: Int = Typeface.NORMAL
) {
    drawContext.canvas.nativeCanvas.drawText(
        value,
        x,
        y,
        NativePaint().apply {
            isAntiAlias = true
            textSize = size
            this.color = color.toArgb()
            textAlign = alignment
            typeface = Typeface.create(Typeface.SANS_SERIF, style)
        }
    )
}

