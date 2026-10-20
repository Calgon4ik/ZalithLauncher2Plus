package com.movtery.zalithlauncher.game.account.microsoft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import java.lang.ref.WeakReference

/**
 * Мост между окном входа (WebView) и корутиной входа в аккаунт.
 * requestCode открывает окно и ждёт код авторизации Microsoft.
 * Возвращает null, если пользователь закрыл окно.
 */
object MicrosoftWebLoginBridge {
    @Volatile
    private var pending: CompletableDeferred<String?>? = null

    @Volatile
    private var activity: WeakReference<Activity>? = null

    suspend fun requestCode(context: Context): String? {
        val deferred = CompletableDeferred<String?>()
        pending?.complete(null)
        pending = deferred
        context.startActivity(
            Intent(context, MicrosoftLoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        try {
            return deferred.await()
        } finally {
            if (pending === deferred) pending = null
            //Если задачу отменили, а окно ещё открыто — закрываем его
            activity?.get()?.let { act -> act.runOnUiThread { act.finish() } }
        }
    }

    internal fun attach(act: Activity) {
        activity = WeakReference(act)
    }

    internal fun complete(code: String?) {
        pending?.complete(code)
    }

    internal fun fail(message: String) {
        pending?.completeExceptionally(Exception(message))
    }
}

/**
 * Окно входа через Microsoft в стиле Pojav:
 * страница входа в WebView, по редиректу забираем code.
 */
class MicrosoftLoginActivity : Activity() {
    private var webView: WebView? = null
    private var handled = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MicrosoftWebLoginBridge.attach(this)

        val view = WebView(this)
        webView = view
        setContentView(view)

        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        //Чистый вход каждый раз
        CookieManager.getInstance().removeAllCookies(null)

        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                return handle(request.url)
            }

            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                handle(Uri.parse(url))
            }
        }
        view.loadUrl(POJAV_AUTH_URL)
    }

    private fun handle(url: Uri): Boolean {
        if (!url.toString().startsWith(POJAV_REDIRECT_URI)) return false
        if (!handled) {
            handled = true
            val code = url.getQueryParameter("code")
            if (code != null) {
                MicrosoftWebLoginBridge.complete(code)
            } else {
                MicrosoftWebLoginBridge.fail(
                    url.getQueryParameter("error_description")
                        ?: url.getQueryParameter("error")
                        ?: "Microsoft login failed"
                )
            }
            finish()
        }
        return true
    }

    override fun onDestroy() {
        //Окно закрыто пользователем без результата
        if (!handled && isFinishing) {
            MicrosoftWebLoginBridge.complete(null)
        }
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}