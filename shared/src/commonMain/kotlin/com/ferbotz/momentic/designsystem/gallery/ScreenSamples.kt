package com.ferbotz.momentic.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.ferbotz.momentic.data.model.Attendance
import com.ferbotz.momentic.data.model.Category
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.data.model.DailyViews
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.OpenGraph
import com.ferbotz.momentic.data.model.Rsvp
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.data.repository.PendingPurchase
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.fields.ToggleRow
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.account.SignInContent
import com.ferbotz.momentic.feature.editor.EditorScreen
import com.ferbotz.momentic.feature.explore.CategoryScreen
import com.ferbotz.momentic.feature.explore.ExploreScreen
import com.ferbotz.momentic.feature.moments.MomentDetailScreen
import com.ferbotz.momentic.feature.moments.MomentsScreen
import com.ferbotz.momentic.feature.moments.MomentsTab
import com.ferbotz.momentic.feature.moments.RsvpsScreen
import com.ferbotz.momentic.feature.publish.ExtendContent
import com.ferbotz.momentic.feature.publish.PaywallScreen
import com.ferbotz.momentic.feature.publish.PublishedScreen
import com.ferbotz.momentic.feature.settings.DeleteAccountScreen
import com.ferbotz.momentic.feature.settings.PurchasesScreen
import com.ferbotz.momentic.feature.settings.SettingsScreen
import com.ferbotz.momentic.feature.template.TemplateScreen
import com.ferbotz.momentic.platform.FullScreenWindow
import com.ferbotz.momentic.presentation.account.AccountUiState
import com.ferbotz.momentic.presentation.account.SignInUiState
import com.ferbotz.momentic.presentation.editor.AutosaveState
import com.ferbotz.momentic.presentation.editor.EditorUiState
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.feed.FeedUiState
import com.ferbotz.momentic.presentation.moments.MomentDetailUiState
import com.ferbotz.momentic.presentation.moments.MomentsUiState
import com.ferbotz.momentic.presentation.moments.RsvpsUiState
import com.ferbotz.momentic.presentation.publish.DurationOption
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishUiState
import com.ferbotz.momentic.presentation.publish.perDayPrice
import com.ferbotz.momentic.presentation.template.TemplateDetailUiState
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.MomenticTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** A screen in a state worth seeing on a phone: [name] in the list, [content] full screen ([close] leaves it). */
internal class ScreenSample(val group: String, val name: String, val content: @Composable (close: () -> Unit) -> Unit)

/**
 * The app's screens drawn from made-up states — the ones the live backend can't show on demand
 * (offline, loading, a failure) as well as the everyday ones. Each opens full screen; Back closes it.
 * "Text at 200 %" opens them with the largest system text, without touching the phone's settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScreenSamples() {
    var open by remember { mutableStateOf<ScreenSample?>(null) }
    var largeText by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        ToggleRow(checked = largeText, onCheckedChange = { largeText = it }, label = "Text at 200 %")
        screenSamples.groupBy { it.group }.forEach { (group, samples) ->
            Text(group, style = Momentic.type.label, color = Momentic.colors.ink)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
            ) {
                samples.forEach { sample -> SecondaryButton(sample.name, onClick = { open = sample }, compact = true) }
            }
        }
    }
    open?.let { sample ->
        FullScreenWindow(onDismissRequest = { open = null }) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 2f else density.fontScale)) {
                Box(Modifier.fillMaxSize().background(Momentic.colors.background)) {
                    sample.content { open = null }
                }
            }
        }
    }
}

private val sampleFeed: List<FeedSection> by lazy {
    fun card(template: TemplateDetail) = TemplateCard(
        id = template.id,
        categoryId = template.categoryId,
        name = template.name,
        tagline = template.tagline,
        thumbnailUrl = template.thumbnailUrl,
        tierKey = template.tierKey,
        displayPerDay = template.displayPerDay,
        tags = template.tags,
        previewCode = template.previewCode,
    )
    listOf(
        FeedSection(Category("wedding", "Wedding", "💍"), listOf(card(GalleryTemplates.wedding))),
        FeedSection(Category("love", "Love", "💕"), listOf(card(GalleryTemplates.proposal))),
        FeedSection(Category("birthday", "Birthday", "🎂"), listOf(card(GalleryTemplates.giftReveal))),
    )
}

private val sampleLive: Creation by lazy {
    Creation(
        id = "c1",
        templateId = GalleryTemplates.proposal.id,
        templateName = GalleryTemplates.proposal.name,
        status = CreationStatus.PUBLISHED,
        data = JsonObject(emptyMap()),
        coverImageId = null,
        shareCode = "aK3x9Qm2",
        shareUrl = "https://momentic.ferbotz.com/aK3x9Qm2",
        publishedAt = Clock.System.now(),
        expiresAt = Clock.System.now() + 7.days,
        isExpired = false,
        viewCount = 0,
        createdAt = null,
        updatedAt = null,
    )
}

private val samplePaywall: PublishUiState by lazy {
    val prices = mapOf(1 to (149L to "₹149"), 3 to (299L to "₹299"), 7 to (599L to "₹599"), 30 to (1_999L to "₹1,999"), 90 to (4_999L to "₹4,999"))
    PublishUiState(
        isLoading = false,
        creation = sampleLive.copy(status = CreationStatus.DRAFT, shareCode = null, shareUrl = null, expiresAt = null),
        template = GalleryTemplates.proposal,
        tierLabel = "Premium",
        options = prices.map { (days, price) ->
            DurationOption(days, "publish_tierC_${days}d", price.second, perDayPrice(price.second, price.first * 1_000_000, days))
        },
        selectedProductId = "publish_tierC_7d",
        isSignedIn = true,
    )
}

private fun moment(
    id: String,
    template: TemplateDetail,
    data: String,
    status: CreationStatus,
    publishedDaysAgo: Int? = null,
    daysLeft: Int? = null,
    editedHoursAgo: Int = 30,
    views: Long = 0,
): Creation {
    val now = Clock.System.now()
    return Creation(
        id = id,
        templateId = template.id,
        templateName = template.name,
        status = status,
        data = Json.parseToJsonElement(data) as JsonObject,
        coverImageId = null,
        shareCode = if (status == CreationStatus.PUBLISHED) "Rk7pQ2x$id" else null,
        shareUrl = if (status == CreationStatus.PUBLISHED) "https://momentic.ferbotz.com/Rk7pQ2x$id" else null,
        publishedAt = publishedDaysAgo?.let { now - it.days },
        expiresAt = daysLeft?.let { now + it.days - 2.hours },
        isExpired = daysLeft != null && daysLeft <= 0,
        viewCount = views,
        createdAt = now - (editedHoursAgo + 2).hours,
        updatedAt = now - editedHoursAgo.hours,
    )
}

private val sampleMoments: MomentsUiState by lazy {
    val proposal = GalleryTemplates.proposal
    val wedding = GalleryTemplates.wedding
    val gift = GalleryTemplates.giftReveal
    MomentsUiState(
        isLoading = false,
        creations = listOf(
            moment("1", proposal, """{"partnerName":"Divya"}""", CreationStatus.PUBLISHED, publishedDaysAgo = 0, daysLeft = 7),
            moment("2", wedding, """{"partnerOne":"Ananya","partnerTwo":"Rohan"}""", CreationStatus.PUBLISHED, publishedDaysAgo = 29, daysLeft = 8, views = 342),
            moment("3", gift, """{"recipientName":"Aarav"}""", CreationStatus.PUBLISHED, publishedDaysAgo = 6, daysLeft = 1, views = 87),
            moment("4", gift, """{"recipientName":"Ma","message":"Thank you"}""", CreationStatus.DRAFT, editedHoursAgo = 26),
            moment("5", proposal, """{}""", CreationStatus.DRAFT, editedHoursAgo = 74),
            moment("6", gift, """{"recipientName":"Kabir"}""", CreationStatus.PUBLISHED, publishedDaysAgo = 30, daysLeft = -22, views = 214),
        ),
        templates = sampleFeed.flatMap { it.templates }.associateBy { it.id },
    )
}

private val sampleDetail: MomentDetailUiState by lazy {
    val creation = sampleMoments.creations[1]
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val views = listOf(2, 5, 3, 8, 12, 9, 14, 11, 7, 6, 18, 22, 15, 9, 8, 12, 16, 46, 31, 20, 14, 9, 7, 11, 13, 8, 6, 5, 9, 4)
    MomentDetailUiState(
        isLoading = false,
        creation = creation,
        template = GalleryTemplates.wedding,
        stats = CreationStats(
            totalViews = 342,
            daysRemaining = 8,
            expiresAt = creation.expiresAt,
            isExpired = false,
            dailyViews = views.mapIndexed { i, v -> DailyViews(today.minus(DatePeriod(days = views.size - 1 - i)), v.toLong()) },
            rsvp = RsvpSummary(yes = 24, no = 3, maybe = 5, guests = 61),
        ),
        showStats = true,
        canRenew = true,
        linkPreview = OpenGraph("Ananya weds Rohan", "You're invited", null),
    )
}

private val sampleRsvps: RsvpsUiState by lazy {
    fun rsvp(name: String, answer: Attendance, guests: Int, phone: String?, note: String?) =
        Rsvp(id = name, guestName = name, attending = answer, guestCount = guests, message = note, contactPhone = phone, createdAt = null)
    RsvpsUiState(
        isLoading = false,
        rsvps = listOf(
            rsvp("Meera Iyer", Attendance.YES, 2, "+91 98765 43210", "We wouldn't miss it! Save us a spot near the dance floor."),
            rsvp("Sana Khan", Attendance.MAYBE, 1, "+91 99300 44556", "Trying to move a work trip — I'll confirm by Sunday."),
            rsvp("Rahul Verma", Attendance.YES, 4, "+91 98200 11223", "Coming with the whole family. So happy for you both."),
            rsvp("Dev Patel", Attendance.NO, 0, null, "So sorry to miss it — sending love from Pune."),
        ),
        summary = RsvpSummary(yes = 24, no = 3, maybe = 5, guests = 61),
    )
}

private val screenSamples: List<ScreenSample> = listOf(
    ScreenSample("Explore", "Feed") { _ ->
        ExploreScreen(FeedUiState(sections = sampleFeed, isLoading = false), null, {}, {}, {}, {})
    },
    ScreenSample("Explore", "Loading") { _ ->
        ExploreScreen(FeedUiState(isLoading = true), null, {}, {}, {}, {})
    },
    ScreenSample("Explore", "Offline") { _ ->
        ExploreScreen(
            FeedUiState(isLoading = false, error = "You're offline. Check your connection and try again.", isOffline = true),
            null, {}, {}, {}, {},
        )
    },
    ScreenSample("Explore", "Failed") { _ ->
        ExploreScreen(FeedUiState(isLoading = false, error = "Our servers hiccuped. Please try again in a moment."), null, {}, {}, {}, {})
    },
    ScreenSample("Explore", "Category") { close ->
        CategoryScreen(sampleFeed[1], isLoading = false, error = null, onBack = close, onOpenTemplate = {}, onRetry = {})
    },
    ScreenSample("Explore", "Category loading") { close ->
        CategoryScreen(null, isLoading = true, error = null, onBack = close, onOpenTemplate = {}, onRetry = {})
    },
    ScreenSample("Template", "The Invitation") { close ->
        TemplateScreen(TemplateDetailUiState(GalleryTemplates.wedding, isLoading = false), onBack = close, onLiveExample = {}, onMakeItYours = {}, onRetry = {})
    },
    ScreenSample("Template", "Loading") { close ->
        TemplateScreen(TemplateDetailUiState(), onBack = close, onLiveExample = null, onMakeItYours = {}, onRetry = {})
    },
    ScreenSample("Create", "Personalise") { close ->
        val scope = rememberCoroutineScope()
        val form = remember { GalleryForm(GalleryTemplates.proposal, scope, onPick = { _, _ -> }) }
        EditorScreen(
            state = form.state.copy(autosave = AutosaveState.Saved),
            actions = form,
            onBack = close,
            onPreview = form::check,
            onRetry = {},
            onKeepEditing = {},
            onLeaveWithoutSaving = close,
        )
    },
    ScreenSample("Create", "Personalise · leaving") { close ->
        val scope = rememberCoroutineScope()
        val form = remember { GalleryForm(GalleryTemplates.giftReveal, scope, onPick = { _, _ -> }) }
        var asking by remember { mutableStateOf(true) }
        EditorScreen(
            state = form.state.copy(
                autosave = AutosaveState.NotSaved,
                confirmLeave = asking,
                errors = mapOf(FieldPath("recipientName") to "Keep it to 40 characters"),
            ),
            actions = form,
            onBack = { asking = true },
            onPreview = {},
            onRetry = {},
            onKeepEditing = { asking = false },
            onLeaveWithoutSaving = close,
        )
    },
    ScreenSample("Create", "Personalise loading") { close ->
        EditorScreen(EditorUiState(), GalleryForm(GalleryTemplates.proposal, rememberCoroutineScope(), { _, _ -> }), close, {}, {}, {}, close)
    },
    ScreenSample("Create", "Sign in") { close ->
        MomenticBottomSheet(onDismissRequest = close) {
            SignInContent(SignInUiState(), googleAvailable = false, onGoogle = {}, onTerms = {}, onPrivacy = {}, onClose = dismiss)
        }
    },
    ScreenSample("Create", "Sign in · ready") { close ->
        MomenticBottomSheet(onDismissRequest = close) {
            SignInContent(
                SignInUiState(error = "Google couldn't confirm it's you. Please try again."),
                googleAvailable = true, onGoogle = {}, onTerms = {}, onPrivacy = {}, onClose = dismiss,
            )
        }
    },
    ScreenSample("Publish", "Paywall") { close ->
        var picked by remember { mutableStateOf("publish_tierC_7d") }
        PaywallScreen(samplePaywall.copy(selectedProductId = picked), close, { picked = it }, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Publish", "Paywall · this build") { close ->
        PaywallScreen(
            samplePaywall.copy(
                options = samplePaywall.options.map { it.copy(price = null, perDay = null) },
                isAvailable = false,
                unavailableReason = "Purchases aren't available in this version of the app yet.",
            ),
            close, {}, {}, {}, {}, {}, {}, {},
        )
    },
    ScreenSample("Publish", "Paywall · to finish") { close ->
        PaywallScreen(samplePaywall.copy(missingFields = listOf("Your moments", "The question")), close, {}, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Publish", "Publishing") { close ->
        PaywallScreen(samplePaywall.copy(stage = PublishStage.CONFIRMING), close, {}, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Publish", "Couldn't finish") { close ->
        PaywallScreen(
            samplePaywall.copy(
                pendingPurchase = PendingPurchase(PublishKind.PUBLISH, "c1", "tx1", "publish_tierC_7d", 0),
                confirmError = "The connection dropped while publishing",
            ),
            close, {}, {}, {}, {}, {}, {}, {},
        )
    },
    ScreenSample("Publish", "Live") { close ->
        PublishedScreen(
            creation = sampleLive,
            linkPreview = OpenGraph("A question for Divya", "Tap to open", null),
            categoryId = "love",
            isLoading = false,
            error = null,
            onDone = close,
            onCopy = {},
            onShareTo = {},
            onShowQr = {},
            onRetry = {},
        )
    },
    ScreenSample("Publish", "Add more time") { close ->
        var picked by remember { mutableStateOf("publish_tierC_7d") }
        MomenticBottomSheet(onDismissRequest = close) {
            ExtendContent(
                state = samplePaywall.copy(kind = PublishKind.RENEW, creation = sampleLive, selectedProductId = picked),
                momentTitle = "A question for Divya",
                onSelect = { picked = it },
                onAdd = {},
                onTerms = {},
                onPrivacy = {},
                onClose = dismiss,
            )
        }
    },
    ScreenSample("Moments", "Moments list") { _ ->
        var tab by remember { mutableStateOf(MomentsTab.Live) }
        MomentsScreen(sampleMoments.copy(pendingPurchases = 1), tab, 20, { tab = it }, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Moments", "Moments empty") { _ ->
        var tab by remember { mutableStateOf(MomentsTab.Drafts) }
        MomentsScreen(MomentsUiState(isLoading = false), tab, 20, { tab = it }, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Moments", "Moments loading") { _ ->
        MomentsScreen(MomentsUiState(), MomentsTab.Drafts, 20, {}, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Moments", "Moment · live") { close ->
        MomentDetailScreen(sampleDetail, close, {}, {}, {}, {}, {}, {}, {}, {})
    },
    ScreenSample("Moments", "Moment · ended") { close ->
        MomentDetailScreen(
            MomentDetailUiState(isLoading = false, creation = sampleMoments.creations[5], template = GalleryTemplates.giftReveal, showStats = true, canRenew = true),
            close, {}, {}, {}, {}, {}, {}, {}, {},
        )
    },
    ScreenSample("Moments", "Moment · draft") { close ->
        MomentDetailScreen(
            MomentDetailUiState(isLoading = false, creation = sampleMoments.creations[3], template = GalleryTemplates.giftReveal, canPublish = true),
            close, {}, {}, {}, {}, {}, {}, {}, {},
        )
    },
    ScreenSample("Moments", "Guest replies") { close ->
        var filter by remember { mutableStateOf<Attendance?>(null) }
        RsvpsScreen(sampleRsvps, "Ananya weds Rohan", filter, { filter = it }, close, {}, {}, {})
    },
    ScreenSample("Settings", "Settings · signed in") { _ ->
        SettingsScreen(
            state = AccountUiState(
                session = SessionState.SignedIn(User("u1", "arjun.m@example.com", "Arjun Mehta", null, null)),
                appVersion = "1.0 (1)",
            ),
            signIn = SignInUiState(),
            googleAvailable = false,
            onGoogle = {}, onPurchases = {}, onPrivacy = {}, onTerms = {}, onSupport = {}, onSignOut = {}, onDeleteAccount = {},
        )
    },
    ScreenSample("Settings", "Settings · signed out") { _ ->
        SettingsScreen(
            state = AccountUiState(appVersion = "1.0 (1)"),
            signIn = SignInUiState(),
            googleAvailable = true,
            onGoogle = {}, onPurchases = {}, onPrivacy = {}, onTerms = {}, onSupport = {}, onSignOut = {}, onDeleteAccount = {},
        )
    },
    ScreenSample("Settings", "Purchases") { close -> PurchasesScreen(onBack = close, onSupport = {}) },
    ScreenSample("Settings", "Delete account") { close ->
        var confirming by remember { mutableStateOf(false) }
        DeleteAccountScreen("arjun.m@example.com", onBack = close, onMore = {}, onDelete = { confirming = true }, isDeleting = false)
        if (confirming) {
            ConfirmDialog(
                title = "Delete your account?",
                message = "Your moments go offline now, and everything is erased after 30 days.",
                confirmLabel = "Delete account",
                onConfirm = { confirming = false },
                dismissLabel = "Cancel",
                onDismiss = { confirming = false },
                icon = MomenticIcons.Trash,
            )
        }
    },
    ScreenSample("Template", "Failed") { close ->
        TemplateScreen(
            TemplateDetailUiState(isLoading = false, error = "We couldn't find that. It may have been removed."),
            onBack = close, onLiveExample = null, onMakeItYours = {}, onRetry = {},
        )
    },
)

// Android Studio previews of the main screens, from the same made-up states as the gallery.

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) = MomenticTheme { Box(Modifier.background(Momentic.colors.background)) { content() } }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ExplorePreview() = PreviewFrame { ExploreScreen(FeedUiState(sections = sampleFeed, isLoading = false), null, {}, {}, {}, {}) }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TemplatePreview() = PreviewFrame {
    TemplateScreen(TemplateDetailUiState(GalleryTemplates.wedding, isLoading = false), onBack = {}, onLiveExample = {}, onMakeItYours = {}, onRetry = {})
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PaywallPreview() = PreviewFrame { PaywallScreen(samplePaywall, {}, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LivePreview() = PreviewFrame {
    PublishedScreen(sampleLive, OpenGraph("A question for Divya", "Tap to open", null), "love", false, null, {}, {}, {}, {}, {})
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MomentsPreview() = PreviewFrame { MomentsScreen(sampleMoments, MomentsTab.Live, 20, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun MomentPreview() = PreviewFrame { MomentDetailScreen(sampleDetail, {}, {}, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RsvpsPreview() = PreviewFrame { RsvpsScreen(sampleRsvps, "Ananya weds Rohan", null, {}, {}, {}, {}, {}) }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() = PreviewFrame {
    SettingsScreen(
        state = AccountUiState(appVersion = "1.0 (1)"),
        signIn = SignInUiState(),
        googleAvailable = true,
        onGoogle = {}, onPurchases = {}, onPrivacy = {}, onTerms = {}, onSupport = {}, onSignOut = {}, onDeleteAccount = {},
    )
}

