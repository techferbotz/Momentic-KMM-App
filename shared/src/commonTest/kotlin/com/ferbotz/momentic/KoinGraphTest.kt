package com.ferbotz.momentic

import com.ferbotz.momentic.data.di.dataModules
import com.ferbotz.momentic.data.media.ImageProcessor
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.platform.AppPlatform
import com.ferbotz.momentic.presentation.account.AccountViewModel
import com.ferbotz.momentic.presentation.account.SignInViewModel
import com.ferbotz.momentic.presentation.config.RemoteConfigViewModel
import com.ferbotz.momentic.presentation.di.presentationModule
import com.ferbotz.momentic.presentation.editor.EditorViewModel
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.moments.MomentsViewModel
import com.ferbotz.momentic.presentation.moments.RsvpsViewModel
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import com.ferbotz.momentic.presentation.session.AppSessionViewModel
import com.ferbotz.momentic.presentation.template.TemplateDetailViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The whole Koin graph, as the app starts it, resolves every view model with its route parameters —
 * the wiring mistakes the compiler can't see. Only the platform pieces are swapped: storage, build
 * info, the image processor, and the network (every call answers 503, so nothing leaves the test).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun everyViewModelResolvesWithItsParameters() {
        val offline = HttpClient(MockEngine { respond("""{"success":false,"code":"INTERNAL"}""", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(appJson) }
            defaultRequest { url("https://test.local/") }
        }
        val koin = startKoin {
            allowOverride(true)
            modules(dataModules())
            modules(presentationModule)
            modules(
                module {
                    single<Settings> { InMemorySettings() }
                    single { AppBuildInfo(AppPlatform.ANDROID, "1.0", 1L) }
                    single<ImageProcessor> { FakeProcessor() }
                    single { offline }
                },
            )
        }.koin

        koin.get<RemoteConfigViewModel>()
        koin.get<AppSessionViewModel>()
        koin.get<FeedViewModel>()
        koin.get<TemplateDetailViewModel> { parametersOf("greeting_open_letter") }
        val newEditor = koin.get<EditorViewModel> { parametersOf("greeting_open_letter", null) }
        val draftEditor = koin.get<EditorViewModel> { parametersOf("greeting_open_letter", "c1") }
        koin.get<PublishViewModel> { parametersOf("c1") }
        koin.get<MomentsViewModel>()
        koin.get<MomentDetailViewModel> { parametersOf("c1") }
        koin.get<RsvpsViewModel> { parametersOf("c1") }
        koin.get<SignInViewModel>()
        koin.get<AccountViewModel>()

        assertEquals(null, newEditor.uiState.value.creationId)
        assertEquals("c1", draftEditor.uiState.value.creationId)
    }
}
