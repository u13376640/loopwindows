package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.LoopViewModel
import com.example.ui.viewmodel.WebViewAction
import com.example.util.AdBlocker
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainPageScreen(
    viewModel: LoopViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeUrl by viewModel.activeUrl.collectAsStateWithLifecycle()
    val isJsEnabled by viewModel.isJsEnabled.collectAsStateWithLifecycle()
    val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsStateWithLifecycle()

    val progress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }

    // Intercept hardware/gesture back press reliably
    BackHandler(enabled = true) {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            viewModel.updateNavigationState(webView.canGoBack())
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // React to ViewModel WebView actions (load, reload, back, etc.)
    LaunchedEffect(webViewRef) {
        viewModel.webViewActions.collectLatest { action ->
            val webView = webViewRef ?: return@collectLatest
            when (action) {
                is WebViewAction.GoBack -> {
                    if (webView.canGoBack()) webView.goBack()
                }
                is WebViewAction.Reload -> {
                    webView.reload()
                }
                is WebViewAction.LoadUrl -> {
                    webView.loadUrl(action.url)
                }
                is WebViewAction.ClearCache -> {
                    webView.clearCache(true)
                }
            }
        }
    }

    // When activeUrl changes from Settings, load it into WebView if different
    LaunchedEffect(activeUrl, webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        if (webView.url != activeUrl) {
            webView.loadUrl(activeUrl)
        }
    }

    // Update Desktop mode user agent dynamically
    LaunchedEffect(isDesktopMode, webViewRef, defaultUserAgent) {
        val webView = webViewRef ?: return@LaunchedEffect
        val originalUA = defaultUserAgent ?: webView.settings.userAgentString.also { defaultUserAgent = it }
        val desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        webView.settings.userAgentString = if (isDesktopMode) desktopUA else originalUA
    }

    // Update JS setting dynamically
    LaunchedEffect(isJsEnabled, webViewRef) {
        webViewRef?.settings?.javaScriptEnabled = isJsEnabled
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pull down gesture to refresh website
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.reloadCurrentPage() },
            modifier = Modifier.fillMaxSize()
        ) {
            // Pure Webview with integrated AdBlocker
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("webview_container"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        isNestedScrollingEnabled = true

                        settings.apply {
                            javaScriptEnabled = isJsEnabled
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportMultipleWindows(false)
                            mediaPlaybackRequiresUserGesture = false
                        }

                    defaultUserAgent = settings.userAgentString

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (isAdBlockEnabled && request?.url != null) {
                                if (AdBlocker.isAd(request.url)) {
                                    viewModel.onAdBlocked()
                                    return AdBlocker.createEmptyResourceResponse()
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false

                            // Block redirects to known ad networks
                            if (isAdBlockEnabled && request.url != null && AdBlocker.isAd(request.url)) {
                                viewModel.onAdBlocked()
                                return true
                            }

                            return if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                false
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            url?.let { viewModel.onPageStarted(it) }
                            view?.let {
                                viewModel.updateNavigationState(it.canGoBack())
                            }
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            view?.let {
                                viewModel.updateNavigationState(it.canGoBack())
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            url?.let { viewModel.onPageFinished(it) }
                            view?.let {
                                viewModel.updateNavigationState(it.canGoBack())
                            }
                            // Inject cosmetic CSS hiding for leftover ad frames and block rogue popup scripts
                            if (isAdBlockEnabled) {
                                view?.evaluateJavascript(AdBlocker.AD_HIDING_CSS, null)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                viewModel.onErrorReceived(
                                    error?.description?.toString() ?: "Failed to load page"
                                )
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun getDefaultVideoPoster(): Bitmap? {
                            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.BLACK)
                            return bitmap
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            viewModel.onProgressChanged(newProgress)
                            if (isAdBlockEnabled && newProgress in 40..90) {
                                view?.evaluateJavascript(AdBlocker.AD_HIDING_CSS, null)
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view != null && callback != null) {
                                viewModel.showCustomView(view, callback)
                            } else {
                                super.onShowCustomView(view, callback)
                            }
                        }

                        override fun onHideCustomView() {
                            viewModel.hideCustomView()
                            super.onHideCustomView()
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            if (isAdBlockEnabled) {
                                return false // Completely block rogue popup windows from opening
                            }
                            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                        }
                    }

                    webViewRef = this
                    loadUrl(activeUrl)
                }
            },
            update = { webView ->
                webViewRef = webView
            }
        )
    }

        // Subtle loading indicator at the very top edge
        AnimatedVisibility(
            visible = isLoading && progress < 100,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }

        // Error overlay with Retry
        if (hasError) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Unable to Load",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "Failed to connect to the configured URL.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = activeUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { viewModel.reloadCurrentPage() },
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Text("Retry")
                        }

                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("change_url_button")
                        ) {
                            Text("Configure URL in Settings")
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
