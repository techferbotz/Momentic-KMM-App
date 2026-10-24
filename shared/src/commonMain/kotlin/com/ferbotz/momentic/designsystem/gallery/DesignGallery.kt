package com.ferbotz.momentic.designsystem.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.DailyViews
import com.ferbotz.momentic.designsystem.ProvideMomenticImageLoader
import com.ferbotz.momentic.designsystem.components.ButtonStyle
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.DestructiveButton
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.FallingPetals
import com.ferbotz.momentic.designsystem.components.FeaturedTemplateCard
import com.ferbotz.momentic.designsystem.components.IconButtonStyle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MainTab
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomentRow
import com.ferbotz.momentic.designsystem.components.MomenticBottomBar
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.MomenticButton
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.MomenticSnackbarHost
import com.ferbotz.momentic.designsystem.components.MomenticSnackbarVisuals
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.OccasionChip
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.ProgressRing
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SaveIndicator
import com.ferbotz.momentic.designsystem.components.SaveStatus
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SegmentedControl
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.SkeletonGroup
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.components.StatusChip
import com.ferbotz.momentic.designsystem.components.TabHeader
import com.ferbotz.momentic.designsystem.components.TagLabel
import com.ferbotz.momentic.designsystem.components.TemplateTile
import com.ferbotz.momentic.designsystem.components.Tier
import com.ferbotz.momentic.designsystem.components.TierBadge
import com.ferbotz.momentic.designsystem.components.TopBarBadge
import com.ferbotz.momentic.designsystem.components.ViewsBarChart
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.feature.publish.QrSheet
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.Messenger
import com.ferbotz.momentic.presentation.moments.MomentStatus
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.MomenticTheme
import com.ferbotz.momentic.theme.occasionTint
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Every token and component on one scrolling screen — the app-side twin of the design canvas's
 * *Tokens & components* page, for checking them on a real phone. Debug builds only: Android adds a
 * "Momentic gallery" launcher icon; on iOS run the scheme with the `-designGallery` argument.
 */
@Composable
fun DesignGalleryApp() {
    MomenticTheme {
        ProvideMomenticImageLoader()
        DesignGallery()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignGallery() {
    val c = Momentic.colors
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember(scope) { Messenger(snackbars, scope) }
    var tab by remember { mutableStateOf(MainTab.Explore) }
    var segment by remember { mutableIntStateOf(1) }
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalMessenger provides messenger) {
    Scaffold(
        containerColor = c.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { MomenticSnackbarHost(snackbars) },
        bottomBar = { MomenticBottomBar(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            TabHeader(
                title = "Design gallery",
                action = { PrimaryButton("New moment", onClick = {}, icon = MomenticIcons.Plus, compact = true) },
            )
            Text(
                "Tokens, components and the field kit",
                style = Momentic.type.bodySmall,
                color = c.inkSoft,
                modifier = Modifier.padding(horizontal = MomenticSpacing.gutter),
            )

            Section("Field kit") { FieldKitDemo() }
            Section("Colour") { ColourSwatches() }
            Section("Occasion tints") { OccasionTints() }
            Section("Type") { TypeScale() }
            Section("Buttons") { ButtonMatrix() }
            Section("Icon buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    MomenticIconButton(MomenticIcons.Share, "Share", {}, style = IconButtonStyle.Plain)
                    MomenticIconButton(MomenticIcons.Share, "Share", {}, style = IconButtonStyle.Outlined)
                    Box(Modifier.size(96.dp, 64.dp).clip(MomenticShapes.input).background(occasionTint("love").brush), contentAlignment = Alignment.CenterStart) {
                        MomenticIconButton(MomenticIcons.Back, "Back", {}, style = IconButtonStyle.OnArt, modifier = Modifier.padding(start = 8.dp))
                    }
                    MomenticIconButton(MomenticIcons.Phone, "Call", {}, style = IconButtonStyle.Accent)
                    MomenticIconButton(MomenticIcons.Share, "Share", {}, style = IconButtonStyle.Outlined, enabled = false)
                }
            }
            Section("Chips and badges") { ChipsAndBadges(segment) { segment = it } }
            Section("Top bars") { TopBars() }
            Section("Cards") { CardSamples() }
            Section("Messages") {
                InlineBanner("You can keep up to 20 drafts. Delete one to start another.", MessageTone.Info)
                InlineBanner("You’ve added 60 photos — the most for now.", MessageTone.Neutral, icon = MomenticIcons.Photo)
                InlineBanner("Upload limit reached. Try again in 12 minutes.", MessageTone.Warning)
                InlineBanner("This photo expired — please add it again", MessageTone.Error, actionLabel = "Add", onAction = {})
                InlineBanner("We’ll keep trying.", MessageTone.Offline, lead = "You’re offline.")
                InlineBanner("A new version is available", MessageTone.Neutral, icon = MomenticIcons.Sparkle, actionLabel = "Update", onAction = {}, onDismiss = {})
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                    SecondaryButton("Link copied", onClick = { scope.launch { snackbars.showSnackbar(MomenticSnackbarVisuals("Link copied", MessageTone.Success)) } }, compact = true)
                    SecondaryButton("Upload failed", onClick = {
                        scope.launch { snackbars.showSnackbar(MomenticSnackbarVisuals("Couldn’t upload", MessageTone.Error, actionLabel = "Try again")) }
                    }, compact = true)
                    SecondaryButton("Offline", onClick = {
                        scope.launch { snackbars.showSnackbar(MomenticSnackbarVisuals("You’re offline", MessageTone.Offline, actionLabel = "Retry")) }
                    }, compact = true)
                }
            }
            Section("Sheet and dialog") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                    SecondaryButton("Open a sheet", onClick = { showSheet = true }, compact = true)
                    SecondaryButton("Open a dialog", onClick = { showDialog = true }, compact = true)
                    SecondaryButton("Open the QR code", onClick = { showQr = true }, compact = true)
                }
            }
            Section("Empty state") {
                EmptyState(
                    illustration = MomenticIllustrations.Drafts,
                    title = "Your first moment is one tap away",
                    body = "Pick an experience, add a few words and photos. It saves here as you go.",
                    action = { PrimaryButton("New moment", onClick = {}, icon = MomenticIcons.Plus) },
                )
            }
            Section("Loading") { LoadingSamples() }
            Section("Views chart") {
                Column(
                    Modifier.fillMaxWidth().clip(MomenticShapes.card).background(c.surface).padding(MomenticSpacing.gutter),
                ) {
                    Text("Views", style = Momentic.type.label)
                    Text("342", style = Momentic.type.hero)
                    Text("Since 24 September · tap a day for its count", style = Momentic.type.caption, color = c.inkSoft)
                    Spacer(Modifier.height(MomenticSpacing.sm))
                    ViewsBarChart(SampleViews)
                }
            }
            Section("Petals") {
                Box(
                    Modifier.fillMaxWidth().height(180.dp).clip(MomenticShapes.card).background(occasionTint("greeting").brush),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    FallingPetals()
                    Text("Your moment is live", style = Momentic.type.h1, color = c.roseDeep, modifier = Modifier.padding(bottom = MomenticSpacing.md))
                }
            }
            Section("Icons") { IconGrid() }
            Section("Illustrations", divider = false) {
                GalleryGrid(MomenticIllustrations.all, columns = 3) { (name, art) ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(art, contentDescription = name, modifier = Modifier.size(96.dp))
                        Text(name, style = Momentic.type.caption, color = c.inkSoft)
                    }
                }
            }
            Spacer(Modifier.height(MomenticSpacing.xxl))
        }
    }

    if (showSheet) {
        MomenticBottomSheet(onDismissRequest = { showSheet = false }) {
            Text("Save your moment and get your link", style = Momentic.type.h1)
            Row(Modifier.padding(top = MomenticSpacing.sm), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(MomenticIcons.CheckCircle, null, tint = c.success, modifier = Modifier.size(20.dp))
                Text("Your draft comes with you", style = Momentic.type.body)
            }
            Spacer(Modifier.height(MomenticSpacing.lg))
            SecondaryButton("Continue with Google", onClick = dismiss, modifier = Modifier.fillMaxWidth())
            Text(
                "By continuing you agree to our Terms & Conditions and Privacy Policy",
                style = Momentic.type.caption,
                color = c.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.md),
            )
        }
    }
    if (showDialog) {
        ConfirmDialog(
            title = "Delete this moment?",
            message = "Its link will stop working straight away.",
            confirmLabel = "Delete moment",
            onConfirm = { showDialog = false },
            dismissLabel = "Keep it",
            onDismiss = { showDialog = false },
            icon = MomenticIcons.Trash,
        )
    }
    if (showQr) {
        QrSheet(
            url = "https://momentic.ferbotz.com/aK3x9Qm2",
            momentTitle = "A question for Divya",
            shareLine = "I made something for you:",
            onDismiss = { showQr = false },
        )
    }
    }
}

@Composable
private fun Section(title: String, divider: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        Text(title.uppercase(), style = Momentic.type.eyebrow, color = Momentic.colors.rose)
        content()
    }
    if (divider) {
        HorizontalDivider(thickness = 1.dp, color = Momentic.colors.line, modifier = Modifier.padding(horizontal = MomenticSpacing.gutter))
    }
}

/** [items] in rows of [columns] equal cells that fill the width; a short last row keeps the columns lined up. */
@Composable
private fun <T> GalleryGrid(items: List<T>, columns: Int, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                row.forEach { item -> Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { cell(item) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColourSwatches() {
    val c = Momentic.colors
    val swatches = listOf(
        "background" to c.background, "surface" to c.surface, "blush-100" to c.blush100, "blush-200" to c.blush200,
        "petal" to c.petal, "rose" to c.rose, "rose-deep" to c.roseDeep, "rose-soft" to c.roseSoft,
        "ink" to c.ink, "ink-soft" to c.inkSoft, "line" to c.line, "success" to c.success,
        "warning" to c.warning, "error" to c.error, "error-soft" to c.errorSoft, "ended" to c.endedFill,
    )
    GalleryGrid(swatches, columns = 4) { (name, colour) ->
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(48.dp).clip(MomenticShapes.input).background(colour).border(1.dp, c.line, MomenticShapes.input))
            Text(name, style = Momentic.type.caption, color = c.ink, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun OccasionTints() {
    GalleryGrid(SampleOccasions, columns = 3) { (id, emoji, name) ->
        val tint = occasionTint(id)
        Column(
            Modifier.fillMaxWidth().height(72.dp).clip(MomenticShapes.input).background(tint.brush).padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(emoji)
                Text("Aa", style = Momentic.type.cardTitle, color = tint.accent)
            }
            Text(name, style = Momentic.type.captionStrong, color = Momentic.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TypeScale() {
    val t = Momentic.type
    Text("Your moment is live", style = t.display)
    Text("Make it yours", style = t.h1)
    Text("What you’ll add", style = t.h2)
    Text("The Letter", style = t.cardTitle)
    Text("“We wouldn’t miss it! Save us a spot near the dance floor.”", style = t.quote)
    Text("Only you can see this preview. Your link stays the same.", style = t.body)
    Text("Guests reply yes, maybe or no", style = t.bodySmall)
    Text("Publish for 7 days · ₹599", style = t.button)
    Text("Their name", style = t.label)
    Text("Live until 30 October", style = t.caption)
    Text("FEATURED", style = t.eyebrow, color = Momentic.colors.rose)
    Text("342", style = t.hero)
}

@Composable
private fun ButtonMatrix() {
    ButtonStyle.entries.forEach { style ->
        val icon = if (style == ButtonStyle.Destructive || style == ButtonStyle.QuietDestructive) MomenticIcons.Trash else null
        val label = when (style) {
            ButtonStyle.Primary -> "Make it yours"
            ButtonStyle.Secondary -> "See a live example"
            ButtonStyle.Quiet -> "Show QR code"
            ButtonStyle.Destructive -> "Delete my account"
            ButtonStyle.QuietDestructive -> "Remove this moment"
        }
        Text(style.name, style = Momentic.type.captionStrong, color = Momentic.colors.inkSoft)
        MomenticButton(label, onClick = {}, style = style, icon = icon, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
            MomenticButton("Disabled", onClick = {}, style = style, icon = icon, enabled = false, modifier = Modifier.weight(1f))
            MomenticButton("Working…", onClick = {}, style = style, icon = icon, loading = true, modifier = Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        SecondaryButton("Edit", onClick = {}, icon = MomenticIcons.Pencil, modifier = Modifier.weight(1f))
        PrimaryButton("Publish", onClick = {}, modifier = Modifier.weight(1.4f))
    }
    QuietButton("Send a preview link (works for 30 minutes)", onClick = {}, icon = MomenticIcons.Link, modifier = Modifier.fillMaxWidth())
    DestructiveButton("Delete moment", onClick = {}, icon = MomenticIcons.Trash, compact = true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsAndBadges(segment: Int, onSegment: (Int) -> Unit) {
    var selectedOccasion by remember { mutableStateOf("love") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SampleOccasions.forEach { (id, emoji, name) ->
            OccasionChip(emoji, name, id, onClick = { selectedOccasion = id }, selected = id == selectedOccasion)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        Tier.entries.forEach { TierBadge(it) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        StatusChip(MomentStatus.Draft)
        StatusChip(MomentStatus.Live(daysLeft = 5))
        StatusChip(MomentStatus.Live(daysLeft = null))
        StatusChip(MomentStatus.EndingSoon(daysLeft = 1))
        StatusChip(MomentStatus.EndingSoon(daysLeft = 0))
        StatusChip(MomentStatus.Ended)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        TagLabel("Proposal")
        TagLabel("Love")
        TagLabel("Photo story")
    }
    SegmentedControl(listOf("Drafts · 2", "Live · 4", "Ended · 1"), segment, onSegment)
}

@Composable
private fun TopBars() {
    val c = Momentic.colors
    MomenticTopBar(title = "Guest replies", subtitle = "Ananya weds Rohan", onBack = {}, windowInsets = WindowInsets(0.dp)) {
        MomenticIconButton(MomenticIcons.Share, "Share", {})
    }
    MomenticTopBar(title = "The Question", onBack = {}, windowInsets = WindowInsets(0.dp)) {
        SaveIndicator(SaveStatus.Saved)
    }
    MomenticTopBar(
        title = "The Question",
        onBack = {},
        navigationIcon = MomenticIcons.Close,
        navigationLabel = "Close",
        background = c.surface,
        showDivider = true,
        windowInsets = WindowInsets(0.dp),
    ) {
        TopBarBadge("Example", Modifier.padding(end = MomenticSpacing.xs))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
        SaveIndicator(SaveStatus.Saving)
        SaveIndicator(SaveStatus.Saved)
    }
    SaveIndicator(SaveStatus.Offline)
}

@Composable
private fun CardSamples() {
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
        TemplateTile(
            name = "Birthday for Babies",
            tagline = "A soft, playful first-birthday page",
            priceLine = "from Rs.100/day",
            artUrl = null,
            categoryId = "birthday",
            tier = Tier.Signature,
            onClick = {},
            emoji = "🎂",
            modifier = Modifier.weight(1f),
        )
        TemplateTile(
            name = "Open the Surprise",
            tagline = "A gift box they tap to unwrap",
            priceLine = "from Rs.50/day",
            artUrl = null,
            categoryId = "greeting",
            tier = Tier.Simple,
            onClick = {},
            emoji = "💌",
            modifier = Modifier.weight(1f),
        )
    }
    FeaturedTemplateCard(
        name = "The Invitation",
        tagline = "An unfolding wedding invitation",
        priceLine = "from Rs.200/day",
        artUrl = null,
        categoryId = "wedding",
        tier = Tier.Premium,
        onClick = {},
        emoji = "💍",
    )
    MomentRow("Thank you, Ma", "The Letter · edited yesterday", null, "greeting", MomentStatus.Draft, views = null, onClick = {})
    MomentRow("A letter for Divya", "The Letter", null, "love", MomentStatus.Live(5), views = 128, onClick = {})
    MomentRow("A question for Divya", "The Question · published today", null, "love", MomentStatus.Live(7), views = 0, onClick = {})
    MomentRow("Aarav turns one", "Birthday for Babies", null, "birthday", MomentStatus.EndingSoon(1), views = 87, onClick = {})
    MomentRow("Happy 30th, Kabir", "Open the Surprise · ended 2 October", null, "friendship", MomentStatus.Ended, views = 214, onClick = {})
}

@Composable
private fun LoadingSamples() {
    val c = Momentic.colors
    SkeletonGroup {
        Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            Row(
                Modifier.fillMaxWidth().clip(MomenticShapes.card).background(c.surface).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(Modifier.size(72.dp, 90.dp), MomenticShapes.input)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(Modifier.size(150.dp, 14.dp))
                    SkeletonBlock(Modifier.size(96.dp, 10.dp))
                    SkeletonBlock(Modifier.size(120.dp, 20.dp), MomenticShapes.pill)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
                SkeletonBlock(Modifier.size(120.dp, 150.dp), MomenticShapes.card)
                SkeletonBlock(Modifier.size(120.dp, 150.dp), MomenticShapes.card)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(110.dp, 138.dp).clip(MomenticShapes.input).background(occasionTint("love").brush), contentAlignment = Alignment.Center) {
            ProgressRing(progress = 0.62f)
        }
        ProgressRing(progress = null, size = 48.dp)
        Spinner(color = c.rose)
    }
}

@Composable
private fun IconGrid() {
    val c = Momentic.colors
    GalleryGrid(MomenticIcons.all, columns = 4) { (name, icon) ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = name, tint = c.ink)
            }
            Text(name, style = Momentic.type.caption, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val SampleOccasions = listOf(
    Triple("birthday", "🎂", "Birthday"),
    Triple("wedding", "💍", "Wedding"),
    Triple("love", "💕", "Love"),
    Triple("anniversary", "💐", "Anniversary"),
    Triple("friendship", "👫", "Friendship"),
    Triple("greeting", "💌", "Greeting"),
)

/** The canvas's 30 days for "Ananya weds Rohan", 24 September to today. */
private val SampleViews: List<DailyViews> = listOf(
    38, 46, 31, 22, 15, 12, 9, 8, 6, 7, 5, 4, 6, 5, 3, 4, 6, 9, 7, 5, 4, 6, 5, 4, 3, 5, 18, 22, 14, 13,
).mapIndexed { index, views -> DailyViews(LocalDate(2026, 9, 24).plus(index, DateTimeUnit.DAY), views.toLong()) }
