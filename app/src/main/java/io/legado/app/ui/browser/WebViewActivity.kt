package io.legado.app.ui.browser

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.databinding.ActivityWebViewBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieStore
import io.legado.app.help.source.SourceVerificationHelp
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.model.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.wenwen.EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE
import io.legado.app.ui.wenwen.EXTRA_WENWEN_BROWSER_MODE_ENABLED
import io.legado.app.ui.wenwen.EXTRA_WENWEN_BROWSER_SEARCH_QUERY
import io.legado.app.ui.wenwen.EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON
import io.legado.app.ui.wenwen.WenwenBrowserArticleExtractor
import io.legado.app.ui.wenwen.WenwenBrowserOptimizeTrigger
import io.legado.app.ui.wenwen.WenwenBrowserReadabilityBridge
import io.legado.app.ui.wenwen.WenwenBrowserReadabilityPayload
import io.legado.app.ui.wenwen.WenwenBrowserReadabilityPayloadDecoder
import io.legado.app.ui.wenwen.WenwenBrowserReaderActivity
import io.legado.app.ui.wenwen.WenwenBrowserTocEntry
import io.legado.app.utils.ACache
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.keepScreenOn
import io.legado.app.utils.longSnackbar
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setDarkeningAllowed
import io.legado.app.utils.startActivity
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import io.legado.app.help.http.CookieManager as AppCookieManager

class WebViewActivity : VMBaseActivity<ActivityWebViewBinding, WebViewModel>() {

    override val binding by viewBinding(ActivityWebViewBinding::inflate)
    override val viewModel by viewModels<WebViewModel>()
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null
    private var isCloudflareChallenge = false
    private var isFullScreen = false
    private var wenwenBrowserModeEnabled = false
    private var wenwenAutoOptimizeReading = false
    private var wenwenShowManualOptimizeButton = false
    private var wenwenBrowserSearchQuery: String? = null
    private var wenwenOptimizationInProgress = false
    private var lastOptimizedUrl: String? = null
    private val saveImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(webPic, uri.toString())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("title") ?: getString(R.string.loading)
        binding.titleBar.subtitle = intent.getStringExtra("sourceName")
        setupWenwenBrowserMode()
        viewModel.initData(intent) {
            val url = viewModel.baseUrl
            val headerMap = viewModel.headerMap
            initWebView(url, headerMap)
            val html = viewModel.html
            if (html.isNullOrEmpty()) {
                binding.webView.loadUrl(url, headerMap)
            } else {
                binding.webView.loadDataWithBaseURL(url, html, "text/html", "utf-8", url)
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (binding.customWebView.size > 0) {
                customWebViewCallback?.onCustomViewHidden()
                return@addCallback
            } else if (binding.webView.canGoBack()
                && binding.webView.copyBackForwardList().size > 1
            ) {
                binding.webView.goBack()
                return@addCallback
            }
            if (isFullScreen) {
                toggleFullScreen()
                return@addCallback
            }
            finish()
        }
    }

    private fun setupWenwenBrowserMode() {
        wenwenBrowserModeEnabled = intent.getBooleanExtra(EXTRA_WENWEN_BROWSER_MODE_ENABLED, false)
        wenwenAutoOptimizeReading = intent.getBooleanExtra(EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE, false)
        wenwenShowManualOptimizeButton =
            intent.getBooleanExtra(EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON, false)
        wenwenBrowserSearchQuery = intent.getStringExtra(EXTRA_WENWEN_BROWSER_SEARCH_QUERY)
        binding.fabOptimizeReading.apply {
            if (wenwenBrowserModeEnabled && wenwenShowManualOptimizeButton) {
                visible()
            } else {
                gone()
            }
            setOnClickListener {
                optimizeCurrentPage(WenwenBrowserOptimizeTrigger.MANUAL)
            }
            setOnLongClickListener {
                optimizeCurrentPage(WenwenBrowserOptimizeTrigger.MANUAL)
                true
            }
        }
        updateOptimizeReadingButtonState()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_view, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (viewModel.sourceOrigin.isNotEmpty()) {
            menu.findItem(R.id.menu_disable_source)?.isVisible = true
            menu.findItem(R.id.menu_delete_source)?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_open_in_browser -> openUrl(viewModel.baseUrl)
            R.id.menu_copy_url -> sendToClip(viewModel.baseUrl)
            R.id.menu_ok -> {
                if (viewModel.sourceVerificationEnable) {
                    viewModel.saveVerificationResult(binding.webView) {
                        finish()
                    }
                } else {
                    finish()
                }
            }

            R.id.menu_full_screen -> toggleFullScreen()
            R.id.menu_disable_source -> {
                viewModel.disableSource {
                    finish()
                }
            }

            R.id.menu_delete_source -> {
                alert(R.string.draw) {
                    setMessage(getString(R.string.sure_del) + "\n" + viewModel.sourceName)
                    noButton()
                    yesButton {
                        viewModel.deleteSource {
                            finish()
                        }
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    //实现starBrowser调起页面全屏
    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen

        toggleSystemBar(!isFullScreen)

        if (isFullScreen) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(url: String, headerMap: HashMap<String, String>) {
        binding.progressBar.fontColor = accentColor
        binding.webView.webChromeClient = CustomWebChromeClient()
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            setDarkeningAllowed(AppConfig.isNightTheme)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            headerMap[AppConst.UA_NAME]?.let {
                userAgentString = it
            }
        }
        AppCookieManager.applyToWebView(url)
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                hitTestResult.extra?.let { webPic ->
                    selector(
                        arrayListOf(
                            SelectItem(getString(R.string.action_save), "save"),
                            SelectItem(getString(R.string.select_folder), "selectFolder")
                        )
                    ) { _, charSequence, _ ->
                        when (charSequence.value) {
                            "save" -> saveImage(webPic)
                            "selectFolder" -> selectSaveFolder()
                        }
                    }
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { downloadUrl, _, contentDisposition, _, _ ->
            var fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, null)
            fileName = URLDecoder.decode(fileName, "UTF-8")
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, downloadUrl, fileName)
            }
        }
    }

    private fun saveImage(webPic: String) {
        this.webPic = webPic
        val path = ACache.get().getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder()
        } else {
            viewModel.saveImage(webPic, path)
        }
    }

    private fun selectSaveFolder() {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        saveImage.launch {
            otherActions = default
        }
    }

    override fun finish() {
        SourceVerificationHelp.checkResult(viewModel.sourceOrigin)
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    private fun updateOptimizeReadingButtonState() {
        if (!wenwenBrowserModeEnabled || !wenwenShowManualOptimizeButton) {
            binding.fabOptimizeReading.gone()
            return
        }
        binding.fabOptimizeReading.visible()
        binding.fabOptimizeReading.isEnabled = !wenwenOptimizationInProgress
        binding.fabOptimizeReading.text =
            if (wenwenOptimizationInProgress) {
                "识别中..."
            } else {
                "优化阅读"
            }
    }

    private fun optimizeCurrentPage(trigger: WenwenBrowserOptimizeTrigger) {
        if (!wenwenBrowserModeEnabled || wenwenOptimizationInProgress) return
        val currentUrl = binding.webView.url ?: viewModel.baseUrl
        if (currentUrl.isBlank()) {
            if (trigger == WenwenBrowserOptimizeTrigger.MANUAL) {
                binding.root.longSnackbar("页面还没加载完成，稍后再试。")
            }
            return
        }
        if (trigger == WenwenBrowserOptimizeTrigger.AUTO &&
            WenwenBrowserArticleExtractor.shouldSkipOptimization(currentUrl)
        ) {
            return
        }
        wenwenOptimizationInProgress = true
        updateOptimizeReadingButtonState()
        ensureReadabilityBridge { bridgeReady ->
            if (!bridgeReady) {
                extractWithLegacyFallback(currentUrl, trigger)
                return@ensureReadabilityBridge
            }
            binding.webView.evaluateJavascript(WenwenBrowserReadabilityBridge.extractCallScript()) { raw ->
                val payload = WenwenBrowserReadabilityPayloadDecoder.decode(raw)
                val article = payload?.toBrowserArticle(currentUrl, trigger)
                if (article != null) {
                    completeOptimization(trigger, article)
                } else {
                    extractWithLegacyFallback(currentUrl, trigger)
                }
            }
        }
    }

    private fun ensureReadabilityBridge(onReady: (Boolean) -> Unit) {
        lifecycleScope.launch {
            val bootstrapScript =
                withContext(Dispatchers.IO) {
                    kotlin.runCatching {
                        WenwenBrowserReadabilityBridge.bootstrapScript()
                    }.getOrNull()
                }
            if (bootstrapScript == null) {
                onReady(false)
                return@launch
            }
            binding.webView.evaluateJavascript(bootstrapScript) { raw ->
                onReady(raw == "true")
            }
        }
    }

    private fun extractWithLegacyFallback(
        currentUrl: String,
        trigger: WenwenBrowserOptimizeTrigger,
    ) {
        binding.webView.evaluateJavascript(
            """
                (function() {
                  const title = document.title || '';
                  const content = document.body ? (document.body.innerText || '') : '';
                  const cover =
                    document.querySelector('meta[property="og:image"]')?.content ||
                    document.querySelector('meta[name="og:image"]')?.content ||
                    document.querySelector('meta[name="twitter:image"]')?.content ||
                    document.querySelector('img')?.src ||
                    '';
                  const nextLink = Array.from(document.querySelectorAll('a[href]')).find((anchor) => {
                    const text = (anchor.textContent || '').trim();
                    return /(下一章|下一页|下页|next)/i.test(text);
                  });
                  const nextUrl = nextLink ? new URL(nextLink.getAttribute('href'), location.href).href : '';
                  const tocEntries = Array.from(document.querySelectorAll('a[href]'))
                    .map((anchor) => {
                      const text = (anchor.textContent || '').replace(/\\s+/g, ' ').trim();
                      if (!text || text.length > 40 || !/(第.{1,16}(章|节|回|卷)|chapter\\s*\\d+|序章|楔子|尾声|番外)/i.test(text)) {
                        return null;
                      }
                      return {
                        title: text,
                        url: new URL(anchor.getAttribute('href'), location.href).href
                      };
                    })
                    .filter(Boolean)
                    .filter((item, index, array) => array.findIndex((candidate) => candidate.url === item.url) === index)
                    .slice(0, 200);
                  return JSON.stringify([title, content, cover, nextUrl, JSON.stringify(tocEntries)]);
                })();
            """.trimIndent()
        ) { raw ->
            val payload = decodeExtractPayload(raw)
            val article =
                payload?.let {
                    WenwenBrowserArticleExtractor.extract(
                        url = currentUrl,
                        title = it.title,
                        rawContent = it.content,
                        trigger = trigger,
                        coverUrl = it.coverUrl,
                        searchQuery = wenwenBrowserSearchQuery,
                        nextPageUrl = it.nextPageUrl,
                        tocEntries = it.tocEntries,
                    )
                }
            completeOptimization(trigger, article)
        }
    }

    private fun completeOptimization(
        trigger: WenwenBrowserOptimizeTrigger,
        article: io.legado.app.ui.wenwen.WenwenBrowserArticle?,
    ) {
        wenwenOptimizationInProgress = false
        updateOptimizeReadingButtonState()
        if (article == null) {
            if (trigger == WenwenBrowserOptimizeTrigger.MANUAL) {
                binding.root.longSnackbar("没有识别到适合阅读的正文内容。")
            }
            return
        }
        if (trigger == WenwenBrowserOptimizeTrigger.AUTO && lastOptimizedUrl == article.url) {
            return
        }
        lastOptimizedUrl = article.url
        WenwenBrowserReaderActivity.start(this, article)
    }

    private fun WenwenBrowserReadabilityPayload.toBrowserArticle(
        currentUrl: String,
        trigger: WenwenBrowserOptimizeTrigger,
    ): io.legado.app.ui.wenwen.WenwenBrowserArticle? {
        return WenwenBrowserArticleExtractor.extract(
            url = currentUrl,
            title = title.ifBlank { binding.webView.title ?: siteName.orEmpty() },
            rawContent = textContent,
            trigger = trigger,
            coverUrl = coverUrl,
            searchQuery = wenwenBrowserSearchQuery,
            nextPageUrl = nextPageUrl,
            tocEntries = tocEntries,
        )
    }

    private fun decodeExtractPayload(raw: String?): WenwenBrowserExtractPayload? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching {
            val escapedJson = JSONArray("[$raw]").optString(0)
            val array = JSONArray(escapedJson)
            WenwenBrowserExtractPayload(
                title = array.optString(0).orEmpty(),
                content = array.optString(1).orEmpty(),
                coverUrl = array.optString(2).orEmpty().ifBlank { null },
                nextPageUrl = array.optString(3).orEmpty().ifBlank { null },
                tocEntries =
                    array.optString(4).orEmpty().takeIf { it.isNotBlank() }?.let { tocJson ->
                        runCatching {
                            val tocArray = JSONArray(tocJson)
                            buildList {
                                for (index in 0 until tocArray.length()) {
                                    val item = tocArray.optJSONObject(index) ?: continue
                                    val title = item.optString("title").orEmpty().trim()
                                    val url = item.optString("url").orEmpty().trim()
                                    if (title.isBlank() || url.isBlank()) continue
                                    add(WenwenBrowserTocEntry(title = title, url = url, orderIndex = size))
                                }
                            }
                        }.getOrNull()
                    }.orEmpty(),
            )
        }.getOrNull()
    }

    private data class WenwenBrowserExtractPayload(
        val title: String,
        val content: String,
        val coverUrl: String?,
        val nextPageUrl: String?,
        val tocEntries: List<WenwenBrowserTocEntry> = emptyList(),
    )

    inner class CustomWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.setDurProgress(newProgress)
            binding.progressBar.gone(newProgress == 100)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            binding.llView.invisible()
            binding.fabOptimizeReading.gone()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
            keepScreenOn(true)
            toggleSystemBar(false)
        }

        override fun onHideCustomView() {
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            updateOptimizeReadingButtonState()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            keepScreenOn(false)
            toggleSystemBar(true)
        }
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                return shouldOverrideUrlLoading(it.url)
            }
            return true
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let {
                return shouldOverrideUrlLoading(Uri.parse(it))
            }
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val cookieManager = CookieManager.getInstance()
            url?.let {
                CookieStore.setCookie(it, cookieManager.getCookie(it))
            }
            view?.title?.let { title ->
                if (title != url && title != view.url && title.isNotBlank()) {
                    binding.titleBar.title = title
                } else {
                    binding.titleBar.title = intent.getStringExtra("title")
                }
            } ?: run {
                binding.titleBar.title = intent.getStringExtra("title")
            }
            view?.evaluateJavascript("!!window._cf_chl_opt") {
                if (it == "true") {
                    isCloudflareChallenge = true
                } else if (isCloudflareChallenge && viewModel.sourceVerificationEnable) {
                    viewModel.saveVerificationResult(binding.webView) {
                        finish()
                    }
                }
            }
            if (wenwenBrowserModeEnabled &&
                wenwenAutoOptimizeReading &&
                !url.isNullOrBlank()
            ) {
                optimizeCurrentPage(WenwenBrowserOptimizeTrigger.AUTO)
            }
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            when (url.scheme) {
                "http", "https" -> {
                    return false
                }

                "wenwentome", "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> {
                        data = url
                    }
                    return true
                }

                else -> {
                    binding.root.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                        openUrl(url)
                    }
                    return true
                }
            }
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.proceed()
        }

    }

}
