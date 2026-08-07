package com.starborn.mantracounter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starborn.mantracounter.data.Japa
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

val DefaultBeadStep: Dp = 78.dp

private val BeadRadius = 26.dp
private val GuruRadius = 34.dp

/**
 * The mala itself: a vertical strand of beads that moves with the finger.
 *
 * Bead `count` sits at the centre line. Positions are computed relative to [count] rather than
 * from an absolute index, so the maths stays exact no matter how large a lifetime count grows —
 * a Float absolute position would lose the fractional drag offset entirely past a few million.
 *
 * The last bead of each mala is drawn as a guru bead with a tassel hanging off it, so the end of
 * the round is visible coming down the strand before you reach it.
 */
@Composable
fun MalaStrand(
    count: Long,
    malaSize: Int,
    dragOffset: Float,
    accent: Color,
    onDark: Boolean,
    modifier: Modifier = Modifier,
    beadStep: Dp = DefaultBeadStep,
) {
    val threadColor = if (onDark) Color.White.copy(alpha = 0.42f) else Color(0x66000000)
    val upcomingColor = if (onDark) Color.White.copy(alpha = 0.28f) else Color(0x33000000)
    val guruColor = if (onDark) Color(0xFFFFD79A) else accent
    val markerColor = if (onDark) Color.White.copy(alpha = 0.55f) else accent.copy(alpha = 0.55f)

    val measurer = rememberTextMeasurer()
    val omOnCounted = measurer.measure(
        AnnotatedString(OM),
        TextStyle(fontSize = 22.sp, color = Color.White.copy(alpha = 0.92f)),
    )
    val omOnUpcoming = measurer.measure(
        AnnotatedString(OM),
        TextStyle(fontSize = 22.sp, color = threadColor),
    )
    val omOnGuru = measurer.measure(
        AnnotatedString(OM),
        TextStyle(fontSize = 30.sp, color = if (onDark) Color(0xFF4A2A0C) else Color.White),
    )

    Canvas(modifier) {
        val step = beadStep.toPx()
        val beadR = BeadRadius.toPx()
        val guruR = GuruRadius.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f

        // How many beads either side of centre reach beyond the edge of the canvas.
        val span = ceil((size.height / 2f) / step).toInt() + 2

        fun yFor(k: Int): Float = cy + (dragOffset - k) * step
        fun indexAt(k: Int): Long = count + k
        fun isGuru(index: Long): Boolean = Japa.closesMala(index, malaSize)

        // Thread first, so beads sit on top of it.
        for (k in -span..span) {
            val index = indexAt(k)
            if (index < 0) continue
            drawLine(
                color = threadColor,
                start = Offset(cx, yFor(k)),
                end = Offset(cx, yFor(k + 1)),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Centre marker — the point the beads pass through, like fingers holding the strand.
        drawLine(
            color = markerColor,
            start = Offset(cx - guruR - 16.dp.toPx(), cy),
            end = Offset(cx - guruR - 4.dp.toPx(), cy),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = markerColor,
            start = Offset(cx + guruR + 4.dp.toPx(), cy),
            end = Offset(cx + guruR + 16.dp.toPx(), cy),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        for (k in -span..span) {
            val index = indexAt(k)
            if (index < 0) continue
            val y = yFor(k)
            val counted = index <= count
            val guru = isGuru(index)

            if (guru) {
                drawGuruStar(cx, y, guruR, guruColor, omOnGuru)
            } else {
                drawBead(
                    cx = cx,
                    cy = y,
                    radius = beadR,
                    color = if (counted) accent else upcomingColor,
                    om = if (counted) omOnCounted else omOnUpcoming,
                )
            }

            // The bead currently under the marker gets a ring, so the count is unambiguous even
            // mid-drag.
            if (abs(dragOffset - k) < 0.5f) {
                drawCircle(
                    color = markerColor,
                    radius = (if (guru) guruR * 1.25f else beadR) + 7.dp.toPx(),
                    center = Offset(cx, y),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

private const val OM = "\u0950"

private fun DrawScope.drawBead(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    om: TextLayoutResult,
) {
    drawCircle(color = color, radius = radius, center = Offset(cx, cy))
    drawText(
        textLayoutResult = om,
        topLeft = Offset(cx - om.size.width / 2f, cy - om.size.height / 2f),
    )
}

/**
 * The bead that closes a mala — the one the bell rings on — is a star rather than a circle, so
 * it is unmistakable coming down the strand.
 */
private fun DrawScope.drawGuruStar(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    om: TextLayoutResult,
) {
    val points = 8
    val outer = radius * 1.25f
    val inner = radius * 0.72f
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val angle = (PI * i / points) - PI / 2
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
    drawText(
        textLayoutResult = om,
        topLeft = Offset(cx - om.size.width / 2f, cy - om.size.height / 2f),
    )
}


