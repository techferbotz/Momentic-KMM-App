package com.ferbotz.momentic.platform

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebPage(state: WebPageState, modifier: Modifier) {
    var view by remember { mutableStateOf<WebView?>(null) }
    LaunchedEffect(state.reloads) {
        if (state.reloads > 0) view?.reload()
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        state.isLoading = true
                        state.failed = false
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        state.isLoading = false
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) {
                            state.failed = true
                            state.isLoading = false
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (uri.scheme == "http" || uri.scheme == "https") return false
                        try {
                            view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: ActivityNotFoundException) {
                            // Nothing on the device handles it; stay on the page.
                        }
                        return true
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        state.progress = newProgress / 100f
                    }
                }
                loadUrl(state.url)
                view = this
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
            view = null
        },
    )
}
