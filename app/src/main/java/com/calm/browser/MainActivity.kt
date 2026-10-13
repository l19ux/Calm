package com.calm.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    private val bg = Color.parseColor("#FAF9F5")   // claude.ai-style warm cream
    private val ink = Color.parseColor("#3D3929")
    private val gray = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    private lateinit var web: WebView
    private lateinit var bar: EditText
    private lateinit var progress: ProgressBar
    private lateinit var root: FrameLayout
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var savedUi = 0
    private var lastSafe = ""

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags

        bar = EditText(this).apply {
            setSingleLine()
            hint = "Search or type address"
            setBackgroundColor(bg)
            setTextColor(ink)
            setHintTextColor(Color.parseColor("#83827D"))
            textSize = 15f
            setPadding(dp(16), dp(12), dp(16), dp(12))
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_GO) {
                    go(text.toString()); clearFocus()
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(windowToken, 0)
                    true
                } else false
            }
        }

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(ink)
            visibility = View.INVISIBLE
        }

        web = WebView(this).apply {
            setBackgroundColor(bg)
            setLayerType(View.LAYER_TYPE_HARDWARE, gray)   // <- everything rendered in B&W
            isVerticalScrollBarEnabled = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = true     // no autoplay
                setSupportMultipleWindows(false)            // no popups
                javaScriptCanOpenWindowsAutomatically = false
                allowFileAccess = false
                allowContentAccess = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                    val u = r.url
                    if (u.scheme != "http" && u.scheme != "https") return true
                    if (SiteFilter.blocked(u)) { showBlocked(); return true }
                    val safe = SiteFilter.safeSearch(u)
                    if (safe != null && safe.toString() != lastSafe) {
                        lastSafe = safe.toString(); v.loadUrl(lastSafe); return true
                    }
                    return false
                }
                override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
                    if (SiteFilter.blocked(r.url) || SiteFilter.isAd(r.url))
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    return null
                }
                override fun onPageStarted(v: WebView, url: String, f: Bitmap?) {
                    if (!bar.hasFocus()) bar.setText(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(v: WebView, p: Int) {
                    this@MainActivity.progress.progress = p
                    this@MainActivity.progress.visibility = if (p in 1..99) View.VISIBLE else View.INVISIBLE
                }
                override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                    customView = view; customCb = cb
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, gray)
                    view.setBackgroundColor(Color.BLACK)
                    root.addView(view, FrameLayout.LayoutParams(-1, -1))
                    savedUi = window.decorView.systemUiVisibility
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }
                override fun onHideCustomView() { hideCustom() }
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            addView(bar, LinearLayout.LayoutParams(-1, -2))
            addView(progress, LinearLayout.LayoutParams(-1, dp(2)))
            addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        root = FrameLayout(this).apply {
            setBackgroundColor(bg)
            addView(column, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(root)

        intent?.data?.let { open(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { open(it) }
    }

    private fun go(input: String) {
        val t = input.trim()
        if (t.isEmpty()) return
        val uri = if (!t.contains(" ") && t.contains("."))
            Uri.parse(if (t.startsWith("http")) t else "https://$t")
        else Uri.parse("https://duckduckgo.com/?kp=1&q=" + Uri.encode(t))
        open(uri)
    }

    private fun open(u: Uri) {
        if (SiteFilter.blocked(u)) showBlocked()
        else web.loadUrl((SiteFilter.safeSearch(u) ?: u).toString())
    }

    private fun showBlocked() {
        val html = "<html><body style='background:#FAF9F5;color:#3D3929;font-family:sans-serif;" +
            "display:flex;align-items:center;justify-content:center;height:90vh;text-align:center'>" +
            "<div><h2>Blocked</h2><p>This page isn't available here.</p></div></body></html>"
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun hideCustom() {
        customView?.let { root.removeView(it) }
        customView = null
        customCb?.onCustomViewHidden(); customCb = null
        window.decorView.systemUiVisibility = savedUi
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> hideCustom()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }
}
