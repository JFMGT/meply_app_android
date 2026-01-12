package de.meply.meply

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val webView = findViewById<WebView>(R.id.webView)

        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "Meply"

        toolbar.title = title
        toolbar.setNavigationOnClickListener {
            finish()
        }

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }
}
