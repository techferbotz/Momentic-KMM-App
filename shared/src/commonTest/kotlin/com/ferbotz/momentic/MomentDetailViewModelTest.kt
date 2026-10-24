package com.ferbotz.momentic

import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.data.model.OpenGraph
import com.ferbotz.momentic.data.model.RenderMode
import com.ferbotz.momentic.data.model.RenderedPage
import com.ferbotz.momentic.data.repository.CreationEvents
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "How it will look in WhatsApp": a live moment's link preview comes from its public page. */
@OptIn(ExperimentalCoroutinesApi::class)
class MomentDetailViewModelTest {

    private val gift = TemplateFixtures.template(TemplateFixtures.giftReveal)
    private val og = OpenGraph("A surprise for Asha", "Tap to open it", "https://momentic.ferbotz.com/og/aK3x9Qm2.png")
    private val page = RenderedPage(gift.id, JsonObject(emptyMap()), RenderMode.PUBLISHED, null, true, og, false, "c1")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(status: CreationStatus, render: FakeRender): MomentDetailViewModel {
        val creations = FakeCreations().apply {
            stored = creation("c1", gift.id, JsonObject(emptyMap()), status).copy(
                shareCode = if (status == CreationStatus.PUBLISHED) "aK3x9Qm2" else null,
                shareUrl = if (status == CreationStatus.PUBLISHED) "https://momentic.ferbotz.com/aK3x9Qm2" else null,
            )
        }
        return MomentDetailViewModel("c1", creations, FakeCatalogue(gift), render, FakeRemoteConfig(), CreationEvents())
    }

    @Test
    fun aLiveMoment_previewsWithItsPagesTitleAndImage() {
        val vm = detail(CreationStatus.PUBLISHED, FakeRender(mapOf("aK3x9Qm2" to page)))
        assertEquals(og, vm.uiState.value.linkPreview)
    }

    @Test
    fun aDraft_hasNoLinkToPreview() {
        val render = FakeRender(mapOf("aK3x9Qm2" to page))
        val vm = detail(CreationStatus.DRAFT, render)
        assertNull(vm.uiState.value.linkPreview)
        assertTrue(render.rendered.isEmpty())
    }
}
