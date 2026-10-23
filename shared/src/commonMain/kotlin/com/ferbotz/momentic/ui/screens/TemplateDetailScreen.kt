package com.ferbotz.momentic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.template.TemplateDetailUiState
import com.ferbotz.momentic.ui.components.BackButton
import com.ferbotz.momentic.ui.components.ErrorState
import com.ferbotz.momentic.ui.components.Overline
import com.ferbotz.momentic.ui.components.TagChip
import com.ferbotz.momentic.ui.components.TemplateArt

/**
 * One template: its art, what it asks for, how long it can stay live, and the two ways forward —
 * Preview (the shopfront page with sample data) and Use this template (the editor).
 */
@Composable
fun TemplateDetailScreen(
    state: TemplateDetailUiState,
    onBack: () -> Unit,
    onPreview: (previewCode: String) -> Unit,
    onUseTemplate: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val template = state.template
        when {
            template != null -> TemplateDetailContent(template, onPreview, onUseTemplate)
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> ErrorState(state.error ?: "Something went wrong.", onRetry)
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(12.dp))
    }
}

@Composable
private fun TemplateDetailContent(
    template: TemplateDetail,
    onPreview: (String) -> Unit,
    onUseTemplate: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Hero(template)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (template.pricing.label.isNotBlank()) {
                        Overline(template.pricing.label)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (template.displayPerDay.isNotBlank()) {
                        Text(
                            text = template.displayPerDay,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(template.name, style = MaterialTheme.typography.headlineMedium)
                if (template.tagline.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = template.tagline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (template.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(template.description, style = MaterialTheme.typography.bodyLarge)
                }
                if (template.tags.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Tags(template.tags)
                }
                if (!template.canBeFilledIn) {
                    Spacer(Modifier.height(20.dp))
                    NeedsUpdateNote()
                }
                Spacer(Modifier.height(28.dp))
                Overline("What you'll add")
                Spacer(Modifier.height(8.dp))
                template.fields.forEach { field -> FieldSummaryRow(field) }
                if (template.supportsRsvp) {
                    Spacer(Modifier.height(20.dp))
                    Overline("Guests can reply")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Guests can RSVP right from the page, and you can read every reply in the app.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (template.pricing.bundles.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Overline("How long it stays live")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = durationCopy(template, canRenew = LocalRemoteConfig.current.features.renewal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Actions(template, onPreview, onUseTemplate)
    }
}

@Composable
private fun Hero(template: TemplateDetail) {
    val images = template.previewImageUrls.ifEmpty { listOfNotNull(template.thumbnailUrl) }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .aspectRatio(4f / 5f),
    ) {
        if (images.size > 1) {
            val pager = rememberPagerState { images.size }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                TemplateArt(
                    url = images[page],
                    categoryId = template.categoryId,
                    contentDescription = "${template.name}, picture ${page + 1} of ${images.size}",
                    modifier = Modifier.fillMaxSize(),
                    emojiSize = MaterialTheme.typography.displayLarge.fontSize,
                )
            }
            PagerDots(
                count = images.size,
                current = pager.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
        } else {
            TemplateArt(
                url = images.firstOrNull(),
                categoryId = template.categoryId,
                contentDescription = template.name,
                modifier = Modifier.fillMaxSize(),
                emojiSize = MaterialTheme.typography.displayLarge.fontSize,
            )
        }
        // A soft shade under the status bar so the back button and clock read over any art.
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent))),
        )
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (index == current) 0.95f else 0.55f)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { TagChip(it) }
    }
}

@Composable
private fun FieldSummaryRow(field: FieldDefinition) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(fieldGlyph(field.type), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (field.required) field.label else "${field.label} · optional",
                style = MaterialTheme.typography.titleSmall,
            )
            fieldSummary(field)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NeedsUpdateNote() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = "This template asks for something this version of Momentic can't collect yet. Update the app to use it.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun Actions(template: TemplateDetail, onPreview: (String) -> Unit, onUseTemplate: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { onPreview(template.previewCode) },
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Preview") }
            Button(
                onClick = onUseTemplate,
                enabled = template.canBeFilledIn,
                modifier = Modifier.weight(1.6f).height(52.dp),
            ) { Text(if (template.canBeFilledIn) "Use this template" else "Update to use") }
        }
    }
}

private fun fieldGlyph(type: FieldType): String = when (type) {
    FieldType.TEXT -> "✏️"
    FieldType.LONG_TEXT -> "📝"
    FieldType.NUMBER -> "🔢"
    FieldType.BOOLEAN -> "✅"
    FieldType.DATE -> "📅"
    FieldType.DATE_TIME -> "🕰️"
    FieldType.COLOR -> "🎨"
    FieldType.CHOICE -> "🔘"
    FieldType.LOCATION -> "📍"
    FieldType.IMAGE, FieldType.IMAGE_LIST -> "🖼️"
    FieldType.GROUP_LIST -> "📚"
    FieldType.UNKNOWN -> "✨"
}

/** One line on what a field wants, from its contract. */
private fun fieldSummary(field: FieldDefinition): String? = when (field.type) {
    FieldType.TEXT, FieldType.LONG_TEXT -> field.maxLength?.let { "Up to $it characters" }
    FieldType.NUMBER -> when {
        field.min != null && field.max != null -> "A number from ${field.min.clean()} to ${field.max.clean()}"
        else -> "A number"
    }
    FieldType.BOOLEAN -> "On or off"
    FieldType.DATE -> "A date"
    FieldType.DATE_TIME -> "A date and time"
    FieldType.COLOR -> "A colour"
    FieldType.CHOICE -> field.options.joinToString(" · ") { it.label }.ifBlank { null }
    FieldType.LOCATION -> "A place"
    FieldType.IMAGE -> "One photo"
    FieldType.IMAGE_LIST -> countText(field.minItems, field.maxItems, "photo", "photos")
    FieldType.GROUP_LIST -> listOfNotNull(
        countText(field.minItems, field.maxItems, "entry", "entries"),
        field.fields.joinToString(", ") { it.label.lowercase() }.ifBlank { null }?.let { "each with $it" },
    ).joinToString(", ")
    FieldType.UNKNOWN -> "Needs a newer version of Momentic"
}

private fun countText(min: Int?, max: Int?, one: String, many: String): String = when {
    min != null && max != null && min == max -> "$min ${if (min == 1) one else many}"
    min != null && min > 0 && max != null -> "$min to $max $many"
    max != null -> "Up to $max ${if (max == 1) one else many}"
    min != null && min > 0 -> "At least $min ${if (min == 1) one else many}"
    else -> "Any number of $many"
}

private fun durationCopy(template: TemplateDetail, canRenew: Boolean): String {
    val days = template.pricing.bundles.map { it.days }
    val list = when (days.size) {
        1 -> "${days[0]}"
        else -> days.dropLast(1).joinToString(", ") + " or " + days.last()
    }
    val renew = if (canRenew) " You can extend it any time, and the link never changes." else ""
    return "Choose $list days when you publish.$renew"
}

private fun Double.clean(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
