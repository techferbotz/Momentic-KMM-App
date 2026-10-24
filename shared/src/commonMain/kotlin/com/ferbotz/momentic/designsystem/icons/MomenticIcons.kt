package com.ferbotz.momentic.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's line icons — rounded, 1.5 px strokes on a 24 px grid — drawn from the same path data as the
 * design canvas. Tint them with `Icon(…, tint = …)`; the black here is only a mask.
 */
object MomenticIcons {
    // Tabs
    val Explore by icon { circle(12f, 12f, 9f); path("M15.5 8.5l-2 5-5 2 2-5z") }
    val Moments by icon { rect(7f, 3.5f, 13.5f, 13.5f, 3f); path("M4 7.5v9A3.5 3.5 0 0 0 7.5 20h9") }
    val Settings by icon { path("M4 7h9M17 7h3M4 17h3M11 17h9"); circle(15f, 7f, 2f); circle(9f, 17f, 2f) }

    // Navigation
    val Back by icon { path("M19 12H5M11 6l-6 6 6 6") }
    val Forward by icon { path("M5 12h14M13 6l6 6-6 6") }
    val Close by icon { path("M6 6l12 12M18 6L6 18") }
    val ChevronRight by icon { path("M9 5l7 7-7 7") }
    val ExternalLink by icon { path("M14 4h6v6M20 4l-9 9M18 14v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4") }

    // Actions
    val Plus by icon { path("M12 5v14M5 12h14") }
    val Minus by icon { path("M5 12h14") }
    val Check by icon { path("M5 12.5l4.5 4.5L19 7.5") }
    val Pencil by icon { path("M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4z"); path("M13.5 6.5l4 4") }
    val Trash by icon { path("M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4.5h6V7") }
    val Share by icon {
        circle(18f, 5.5f, 2.5f); circle(6f, 12f, 2.5f); circle(18f, 18.5f, 2.5f)
        path("M8.2 10.8l7.6-4.1M8.2 13.2l7.6 4.1")
    }
    val Copy by icon {
        rect(8.5f, 8.5f, 11.5f, 11.5f, 2.5f)
        path("M15.5 8.5V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2.5")
    }
    val Link by icon {
        path("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1")
        path("M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    }
    val LinkOff by icon {
        path("M10 14a4 4 0 0 0 5.7 0l1.5-1.5M18.6 9.4a4 4 0 0 0-5.7-5.7l-1 1")
        path("M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
        path("M4 4l16 16")
    }
    val Download by icon { path("M12 4v11M7 10.5l5 5 5-5M5 20h14") }
    val Retry by icon { path("M20 11a8 8 0 1 0-2.3 5.7"); path("M20 4.5V11h-6.5") }
    val Rotate by icon { path("M4 11a8 8 0 1 1 2.3 5.7"); path("M4 4.5V11h6.5") }
    val DragHandle by icon {
        dot(9f, 6f, 1.5f); dot(15f, 6f, 1.5f)
        dot(9f, 12f, 1.5f); dot(15f, 12f, 1.5f)
        dot(9f, 18f, 1.5f); dot(15f, 18f, 1.5f)
    }
    val More by icon { dot(6f, 12f, 1.6f); dot(12f, 12f, 1.6f); dot(18f, 12f, 1.6f) }
    val Search by icon { circle(11f, 11f, 6.5f); path("M16 16l4.5 4.5") }
    val Qr by icon {
        rect(4f, 4f, 6f, 6f, 1f); rect(14f, 4f, 6f, 6f, 1f); rect(4f, 14f, 6f, 6f, 1f)
        path("M14 14h2v2h-2zM18 18h2v2h-2zM14 18h1M18 14h2")
    }

    // Things
    val Photo by icon { rect(3.5f, 4.5f, 17f, 15f, 3f); circle(9f, 10f, 1.6f); path("M20.5 15.5l-5-5-9 9") }
    val PhotoOff by icon { rect(3.5f, 4.5f, 17f, 15f, 3f); path("M3 3l18 18") }
    val Frame by icon { rect(5f, 3f, 14f, 18f, 2f) }
    val Calendar by icon { rect(3.5f, 5f, 17f, 15.5f, 3f); path("M3.5 10h17M8 3v4M16 3v4") }
    val CalendarPlus by icon { rect(3.5f, 5f, 17f, 15.5f, 3f); path("M3.5 10h17M8 3v4M16 3v4M12 13v5M9.5 15.5h5") }
    val Clock by icon { circle(12f, 12f, 9f); path("M12 7v5l3 2") }
    val Eye by icon { path("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"); circle(12f, 12f, 3f) }
    val MapPin by icon { path("M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z"); circle(12f, 9.5f, 2.5f) }
    val Phone by icon { path("M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z") }
    val Person by icon { circle(12f, 8f, 3.5f); path("M5 20a7 7 0 0 1 14 0") }
    val Users by icon {
        circle(9f, 8f, 3.5f)
        path("M2.5 20a6.5 6.5 0 0 1 13 0")
        path("M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14a6.5 6.5 0 0 1 3.5 6")
    }
    val UserMinus by icon { circle(10f, 8f, 4f); path("M3 20a7 7 0 0 1 14 0M16 11h5") }
    val SignOut by icon { path("M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"); path("M10 16l-4-4 4-4M6 12h10") }
    val Receipt by icon { path("M6 3h12v18l-3-2-3 2-3-2-3 2z"); path("M9 8h6M9 12h6") }
    val Shield by icon { path("M12 3l7.5 3v5.5c0 4.6-3.2 8.3-7.5 9.5-4.3-1.2-7.5-4.9-7.5-9.5V6z") }
    val Document by icon {
        path("M7 3h7l5 5v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
        path("M14 3v5h5M9 13h6M9 17h6")
    }
    val Mail by icon { rect(3f, 5f, 18f, 14f, 3f); path("M3.5 7l8.5 6 8.5-6") }
    val Bubble by icon { path("M4 5.5h16v10H9l-5 4z") }
    val Message by icon { path("M4 5.5h16v10H9l-5 4z"); path("M8 9.5h8M8 12.5h5") }
    val ChatRound by icon { path("M4.5 19.5l1.2-3.6A8 8 0 1 1 8.6 18.6z") }
    val Camera by icon {
        rect(3.5f, 6f, 17f, 13f, 3f); circle(12f, 12.5f, 3.5f)
        path("M8.5 6l1.5-2.5h4L15.5 6")
    }

    // Status
    val CheckCircle by icon { circle(12f, 12f, 9f); path("M8 12.5l3 3 5-6") }
    val CloudCheck by icon {
        path("M7 18.5h10a4 4 0 0 0 .7-7.94A6 6 0 0 0 6.2 9.6 4.5 4.5 0 0 0 7 18.5z")
        path("M9.5 13.8l1.8 1.8 3.4-3.6")
    }
    val Info by icon { circle(12f, 12f, 9f); path("M12 11v5.5"); path("M12 7.8h.01") }
    val Alert by icon { circle(12f, 12f, 9f); path("M12 7.5v5.5"); path("M12 16.5h.01") }
    val Help by icon {
        circle(12f, 12f, 9f)
        path("M9.6 9.4a2.5 2.5 0 0 1 4.8 1c0 1.6-2.4 2-2.4 3.4")
        path("M12 17h.01")
    }
    val No by icon { path("M7 7l10 10M17 7L7 17") }
    val EndedMark by icon { circle(12f, 12f, 9f); path("M8.5 12h7") }
    val WifiOff by icon {
        path("M3 3l18 18")
        path("M8.5 16.5a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 4.2-2.3M12 8a14 14 0 0 1 10 4M2 8.8a14 14 0 0 1 3.6-2.3")
    }
    val Sparkle by icon { path("M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z") }
    val Star by icon { path("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z") }

    /** Every icon with its name — the gallery's icon grid. */
    val all: List<Pair<String, ImageVector>>
        get() = listOf(
            "Explore" to Explore, "Moments" to Moments, "Settings" to Settings, "Back" to Back,
            "Forward" to Forward, "Close" to Close, "ChevronRight" to ChevronRight, "ExternalLink" to ExternalLink,
            "Plus" to Plus, "Minus" to Minus, "Check" to Check, "Pencil" to Pencil, "Trash" to Trash,
            "Share" to Share, "Copy" to Copy, "Link" to Link, "LinkOff" to LinkOff, "Download" to Download,
            "Retry" to Retry, "Rotate" to Rotate, "DragHandle" to DragHandle, "More" to More, "Search" to Search,
            "Qr" to Qr, "Photo" to Photo, "PhotoOff" to PhotoOff, "Frame" to Frame, "Calendar" to Calendar,
            "CalendarPlus" to CalendarPlus, "Clock" to Clock, "Eye" to Eye, "MapPin" to MapPin, "Phone" to Phone,
            "Person" to Person, "Users" to Users, "UserMinus" to UserMinus, "SignOut" to SignOut,
            "Receipt" to Receipt, "Shield" to Shield, "Document" to Document, "Mail" to Mail, "Bubble" to Bubble,
            "Message" to Message, "ChatRound" to ChatRound, "Camera" to Camera, "CheckCircle" to CheckCircle,
            "CloudCheck" to CloudCheck, "Info" to Info, "Alert" to Alert, "Help" to Help, "No" to No,
            "EndedMark" to EndedMark, "WifiOff" to WifiOff, "Sparkle" to Sparkle, "Star" to Star,
        )
}

/** Builds each icon once, on first use. */
private fun icon(draw: LineIcon.() -> Unit): Lazy<ImageVector> = lazy {
    val shapes = LineIcon().apply(draw)
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            shapes.strokes.forEach { nodes ->
                addPath(
                    pathData = nodes,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            shapes.fills.forEach { nodes -> addPath(pathData = nodes, fill = SolidColor(Color.Black)) }
        }
        .build()
}

/** The shapes the canvas's SVG icons are made of, as Compose path nodes. */
private class LineIcon {
    val strokes = mutableListOf<List<PathNode>>()
    val fills = mutableListOf<List<PathNode>>()

    fun path(d: String) {
        strokes += addPathNodes(d)
    }

    fun circle(cx: Float, cy: Float, r: Float) {
        strokes += addPathNodes(circlePath(cx, cy, r))
    }

    /** A filled dot (drag handles, the "more" glyph). */
    fun dot(cx: Float, cy: Float, r: Float) {
        fills += addPathNodes(circlePath(cx, cy, r))
    }

    fun rect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val r = radius
        strokes += addPathNodes(
            "M${x + r},$y h${width - 2 * r} a$r,$r 0 0 1 $r,$r v${height - 2 * r} a$r,$r 0 0 1 ${-r},$r " +
                "h${-(width - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(height - 2 * r)} a$r,$r 0 0 1 $r,${-r} z",
        )
    }

    private fun circlePath(cx: Float, cy: Float, r: Float): String =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0 z"
}
