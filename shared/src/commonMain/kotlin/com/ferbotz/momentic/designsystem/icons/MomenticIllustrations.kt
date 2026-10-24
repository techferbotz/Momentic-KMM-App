package com.ferbotz.momentic.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.theme.MomenticColors

/**
 * The soft illustrations for empty states and full-screen messages — a blush disc with a line drawing in
 * rose-deep, from the canvas. Draw them with `Image(…)`, not `Icon`: they carry their own colours.
 */
object MomenticIllustrations {
    /** My Moments → Drafts, empty: a card being written. */
    val Drafts by illustration {
        disc(60f, 60f, 56f)
        rect(30f, 34f, 54f, 44f, 6f, fill = c.surface, stroke = c.roseDeep)
        line("M39 48h28M39 57h20M39 66h24", color = c.blush200, width = 3f)
        shape("M70 86 L92 64 L98 70 L76 92 L68 94 Z", fill = c.blush200, stroke = c.roseDeep)
        line("M88 68 L94 74", color = c.roseDeep)
        shape(sparkle(90f, 34f, 8f), fill = c.rose)
    }

    /** My Moments → Live, empty: a paper plane on its way. */
    val Sent by illustration {
        disc(60f, 60f, 56f)
        shape("M26 58 L94 30 L78 92 L60 72 Z", fill = c.surface, stroke = c.roseDeep)
        line("M60 72 L94 30 M60 72 L58 88 L68 80", color = c.roseDeep)
        dot(22f, 86f, 1.6f, c.rose)
        dot(31f, 82f, 1.6f, c.rose)
        dot(40f, 80f, 1.6f, c.rose)
    }

    /** My Moments → Ended, empty: a sealed envelope. */
    val Sealed by illustration {
        disc(60f, 60f, 56f)
        rect(28f, 40f, 64f, 44f, 6f, fill = c.surface, stroke = c.roseDeep)
        line("M29 43 L60 66 L91 43", color = c.roseDeep)
        dot(60f, 66f, 8f, c.roseDeep)
        shape(sparkle(90f, 31f, 7f), fill = c.rose)
    }

    /** RSVPs, none yet: a reply card waiting. */
    val Replies by illustration {
        disc(60f, 60f, 56f)
        rect(34f, 28f, 52f, 64f, 6f, fill = c.surface, stroke = c.roseDeep)
        line("M44 46h32M44 58h32M44 70h20", color = c.blush200, width = 3f)
        dot(86f, 86f, 13f, c.rose)
        line("M80 86h12M86 80v12", color = c.white, width = 2.5f)
    }

    /** Purchases, none yet: a receipt. */
    val Receipt by illustration {
        disc(60f, 60f, 56f)
        shape("M38 26 h44 v68 l-7 -5 -7 5 -8 -5 -7 5 -8 -5 -7 5 z", fill = c.surface, stroke = c.roseDeep)
        line("M48 42h24M48 54h24M48 66h14", color = c.blush200, width = 3f)
    }

    /** Offline: a cloud with a line through it. */
    val Offline by illustration {
        disc(60f, 60f, 56f)
        shape("M38 82 H84 A15 15 0 0 0 85 52 A21 21 0 0 0 45 48 A17 17 0 0 0 38 82 Z", fill = c.surface, stroke = c.roseDeep)
        line("M44 40 L82 92", color = c.rose, width = 2.5f)
    }

    /** Publishing couldn't finish: a link coming apart. */
    val BrokenLink by illustration {
        disc(60f, 60f, 56f)
        line("M52 66a10 10 0 0 0 14 0l9-9a10 10 0 0 0-14-14l-2 2", color = c.roseDeep, width = 2.5f)
        line("M68 54a10 10 0 0 0-14 0l-9 9a10 10 0 0 0 14 14l2-2", color = c.roseDeep, width = 2.5f)
        line("M78 80l8 8M84 74l6 2M74 84l2 6", color = c.rose, width = 2.5f)
    }

    /** Maintenance: a watering can tending a sprout. */
    val Tending by illustration(width = 200f, height = 182f) {
        disc(100f, 96f, 84f)
        shape("M56 164 a44 5 0 1 0 88 0 a44 5 0 1 0 -88 0 z", fill = c.roseDeep, alpha = 0.10f)
        line("M100 114 C100 100 100 90 100 78", color = Leaf)
        shape("M100 96 C88 96 79 88 79 77 C91 77 100 85 100 96 Z", fill = LeafFill, stroke = Leaf, width = 1.75f)
        shape("M100 86 C111 86 121 77 121 65 C109 65 100 74 100 86 Z", fill = LeafFill, stroke = Leaf, width = 1.75f)
        shape("M72 124 L128 124 L121 160 L79 160 Z", fill = c.blush200, stroke = c.roseDeep)
        rect(66f, 112f, 68f, 14f, 5f, fill = c.surface, stroke = c.roseDeep)
        line("M140 30 C140 17 162 17 162 30", color = c.roseDeep)
        rect(128f, 30f, 46f, 32f, 7f, fill = c.surface, stroke = c.roseDeep)
        line("M129 48 L108 60", color = c.roseDeep, width = 3f)
        dot(106f, 61f, 3.5f, c.roseDeep)
        dot(101f, 72f, 2.5f, c.rose, alpha = 0.55f)
        dot(95f, 82f, 2.5f, c.rose, alpha = 0.45f)
        dot(104f, 86f, 2f, c.rose, alpha = 0.35f)
    }

    /** Update required: a phone receiving the new version. */
    val NewVersion by illustration(width = 200f, height = 182f) {
        disc(100f, 92f, 84f)
        rect(66f, 22f, 68f, 128f, 14f, fill = c.surface, stroke = c.roseDeep)
        rect(74f, 36f, 52f, 96f, 6f, fill = c.blush100)
        line("M92 26 h16", color = c.roseDeep)
        dot(100f, 84f, 20f, c.rose)
        line("M100 74 v18 M92 85 l8 8 8-8", color = c.white, width = 2.5f)
        shape(sparkle(150f, 50f, 10f), fill = c.roseDeep, alpha = 0.55f)
        shape(sparkle(46f, 115f, 7f), fill = c.roseDeep, alpha = 0.45f)
        shape(sparkle(156f, 123f, 5f), fill = c.roseDeep, alpha = 0.35f)
    }

    /** Every illustration with its name — the gallery. */
    val all: List<Pair<String, ImageVector>>
        get() = listOf(
            "Drafts" to Drafts, "Sent" to Sent, "Sealed" to Sealed, "Replies" to Replies, "Receipt" to Receipt,
            "Offline" to Offline, "BrokenLink" to BrokenLink, "Tending" to Tending, "NewVersion" to NewVersion,
        )
}

private val c = MomenticColors()
private val Leaf = Color(0xFF21503D)
private val LeafFill = Color(0xFFC0DDCE)

private fun illustration(width: Float = 120f, height: Float = 120f, draw: Drawing.() -> Unit): Lazy<ImageVector> =
    lazy {
        val builder = ImageVector.Builder(
            defaultWidth = width.dp,
            defaultHeight = height.dp,
            viewportWidth = width,
            viewportHeight = height,
        )
        Drawing(builder).draw()
        builder.build()
    }

/** Paints shapes in order, back to front, like the SVG they come from. */
private class Drawing(private val builder: ImageVector.Builder) {

    /** The blush disc every illustration sits on. */
    fun disc(cx: Float, cy: Float, r: Float) = dot(cx, cy, r, c.blush100)

    fun shape(d: String, fill: Color? = null, stroke: Color? = null, width: Float = 2f, alpha: Float = 1f) {
        builder.addPath(
            pathData = addPathNodes(d),
            fill = fill?.let(::SolidColor),
            fillAlpha = alpha,
            stroke = stroke?.let(::SolidColor),
            strokeAlpha = alpha,
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    fun line(d: String, color: Color, width: Float = 2f) = shape(d, stroke = color, width = width)

    fun dot(cx: Float, cy: Float, r: Float, color: Color, alpha: Float = 1f) =
        shape("M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0 z", fill = color, alpha = alpha)

    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color? = null, stroke: Color? = null) = shape(
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r " +
            "h${-(w - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} z",
        fill = fill,
        stroke = stroke,
    )
}

/** A four-pointed sparkle centred on ([cx], [cy]) with arms of length [s]. */
private fun sparkle(cx: Float, cy: Float, s: Float): String =
    "M$cx,${cy - s} Q$cx,$cy ${cx + s},$cy Q$cx,$cy $cx,${cy + s} Q$cx,$cy ${cx - s},$cy Q$cx,$cy $cx,${cy - s} Z"
