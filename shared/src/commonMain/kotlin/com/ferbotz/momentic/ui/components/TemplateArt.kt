package com.ferbotz.momentic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ferbotz.momentic.theme.occasionTint

/**
 * A template's art, over a category-tinted gradient with the category's emoji. The gradient is what
 * shows while the picture loads — and what stays when a template has no art yet, which is most of
 * them today (their thumbnails 404 until the web assets ship, contract BE-002).
 */
@Composable
fun TemplateArt(
    url: String?,
    categoryId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    emojiSize: TextUnit = 56.sp,
) {
    val tint = remember(categoryId) { occasionTint(categoryId) }
    Box(modifier.background(tint.brush), contentAlignment = Alignment.Center) {
        val glyph = emoji?.takeIf { it.isNotBlank() } ?: categoryEmoji(categoryId)
        Text(text = glyph, fontSize = emojiSize)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * The placeholder glyph for a category, for screens that only know its id. The feed passes the
 * backend's own emoji instead.
 */
fun categoryEmoji(categoryId: String): String = when (categoryId) {
    "birthday" -> "🎂"
    "wedding" -> "💍"
    "love" -> "💕"
    "anniversary" -> "💐"
    "friendship" -> "👫"
    "greeting" -> "💌"
    else -> "✨"
}
