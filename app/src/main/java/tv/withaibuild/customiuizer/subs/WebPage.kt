package tv.withaibuild.customiuizer.subs

import android.os.Bundle
import android.webkit.DownloadListener
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.utils.Helpers

@Suppress("DEPRECATION")
class WebPage : SubFragment() {

    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        padded = false
        toolbarMenu = true
        activeMenus = "openinweb"
        pageUrl = requireArguments().getString("pageUrl")

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val webView = mWebView
                if (webView != null && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    webView?.destroy()
                    remove()
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val webView = requireView().findViewById<WebView>(R.id.mainWeb)
        mWebView = webView

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            defaultTextEncodingName = "utf-8"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                getActionBar()?.title = view.title
            }
        }

        webView.setDownloadListener(DownloadListener { url, _, _, _, _ ->
            Helpers.openURL(getValidContext(), url)
        })

        webView.loadUrl(pageUrl ?: "")
    }
}
