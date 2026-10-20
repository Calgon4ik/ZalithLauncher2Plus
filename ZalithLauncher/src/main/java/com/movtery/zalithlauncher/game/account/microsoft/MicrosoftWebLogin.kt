package com.movtery.zalithlauncher.game.account.microsoft

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Вход через Microsoft в стиле Pojav: страница входа в WebView,
 * по редиректу забираем code и отдаём его в onCode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MicrosoftWebLogin(
    modifier: Modifier = Modifier,
    onCode: (String) -> Unit,
    onError: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                CookieManager.getInstance().removeAllCookies(null)

                var handled = false
                fun handle(url: Uri): Boolean {
                    if (!url.toString().startsWith(POJAV_REDIRECT_URI)) return false
                    if (!handled) {
                        handled = true
                        val code = url.getQueryParameter("code")
                        if (code != null) {
                            onCode(code)
                        } else {
                            onError(
                                url.getQueryParameter("error_description")
                                    ?: url.getQueryParameter("error")
                                    ?: "unknown"
                            )
                        }
                    }
                    return true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        handle(request.url)

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        handle(Uri.parse(url))
                    }
                }
                loadUrl(POJAV_AUTH_URL)
            }
        },
        onRelease = { it.destroy() }
    )
}