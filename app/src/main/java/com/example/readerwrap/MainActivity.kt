package com.example.readerwrap

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Minimal WebView wrapper around Mozilla's Readability.js (the same library
 * Firefox itself uses for Reader View). See assets/READABILITY_LICENSE.md.
 *
 * Flow: load the page normally -> tap the FAB -> Readability.js parses the
 * current DOM -> the page body is replaced with the cleaned article.
 * Tap the FAB again (or press back) to return to the original page.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fab: FloatingActionButton

    private var originalUrl: String? = null
    private var isReaderMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        fab = findViewById(R.id.fab_toggle)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // A fresh page loaded (including reloading the original) -> not in reader mode.
                isReaderMode = false
            }
        }

        fab.setOnClickListener {
            if (isReaderMode) {
                originalUrl?.let { webView.loadUrl(it) }
            } else {
                applyReaderMode()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val url: String? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::extractUrl)
            else -> null
        }
        if (url != null) {
            originalUrl = url
            isReaderMode = false
            webView.loadUrl(url)
        }
    }

    private fun extractUrl(text: String): String? =
        Regex("https?://\\S+").find(text)?.value

    private fun applyReaderMode() {
        val readabilitySrc = readAsset("readability.js")
        val css = readAsset("reader.css")
        val cssLiteral = JSONObject.quote(css) // safely escapes into a JS string literal

        // Step 1: define the global Readability() function in the page's JS context.
        webView.evaluateJavascript(readabilitySrc, null)

        // Step 2: run it against a clone of the current DOM and swap in the result.
        val script = """
            (function() {
              try {
                if (typeof Readability === 'undefined') { return 'NO_READABILITY'; }
                var clone = document.cloneNode(true);
                var article = new Readability(clone).parse();
                if (!article) { return 'NO_ARTICLE'; }

                var style = document.getElementById('reader-style');
                if (!style) {
                  style = document.createElement('style');
                  style.id = 'reader-style';
                  document.head.appendChild(style);
                }
                style.textContent = $cssLiteral;

                document.body.innerHTML =
                  '<div id="reader-wrap">' +
                  '<h1>' + (article.title || '') + '</h1>' +
                  (article.byline ? '<p class="byline">' + article.byline + '</p>' : '') +
                  '<div id="reader-content">' + article.content + '</div>' +
                  '</div>';

                return 'OK';
              } catch (e) {
                return 'ERROR:' + (e && e.message ? e.message : e);
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            if (result != null && result.contains("OK")) {
                isReaderMode = true
                fab.setImageResource(android.R.drawable.ic_menu_revert)
            } else {
                Toast.makeText(
                    this,
                    "Couldn't extract article content: $result",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun readAsset(name: String): String {
        assets.open(name).use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                return reader.readText()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            isReaderMode && originalUrl != null -> {
                webView.loadUrl(originalUrl!!)
                fab.setImageResource(android.R.drawable.ic_menu_view)
            }
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }
}
