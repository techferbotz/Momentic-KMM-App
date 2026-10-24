package com.ferbotz.momentic

import com.ferbotz.momentic.data.model.Category
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Explore's offline state: only a first load that failed for want of a connection shows it. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val love = FeedSection(Category("love", "Love", "💕"), emptyList())

    /** Answers each `getFeed()` with the next of [results], then keeps repeating the last. */
    private class ScriptedCatalogue(vararg results: AppResult<List<FeedSection>>) : CatalogueRepository {
        private val queue = ArrayDeque(results.toList())
        override suspend fun getFeed(): AppResult<List<FeedSection>> = if (queue.size > 1) queue.removeFirst() else queue.first()
        override suspend fun getTemplate(templateId: String): AppResult<TemplateDetail> = error("not used")
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aFirstLoadWithoutAConnectionIsOffline_andARetryThatWorksClearsIt() {
        val vm = FeedViewModel(ScriptedCatalogue(AppResult.Failure(AppError.Network()), AppResult.Success(listOf(love))))
        assertTrue(vm.uiState.value.isOffline)
        assertNotNull(vm.uiState.value.error)

        vm.retry()
        assertFalse(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(love), vm.uiState.value.sections)
    }

    @Test
    fun aServerFailureIsAnErrorButNotOffline() {
        val vm = FeedViewModel(ScriptedCatalogue(AppResult.Failure(AppError.Api(503, ErrorCode.INTERNAL, null))))
        assertFalse(vm.uiState.value.isOffline)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun aFailedRefreshKeepsTheFeedAndOnlyLeavesANote() {
        val vm = FeedViewModel(ScriptedCatalogue(AppResult.Success(listOf(love)), AppResult.Failure(AppError.Network())))
        vm.refresh()
        val state = vm.uiState.value
        assertEquals(listOf(love), state.sections)
        assertFalse(state.isOffline)
        assertNull(state.error)
        assertNotNull(state.message)
    }
}
