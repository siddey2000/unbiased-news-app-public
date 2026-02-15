package com.unbiased.news.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

// Primary color from theme (md_theme_light_primary)
private const val TOOLBAR_COLOR = 0xFF1A5F7A.toInt()

/**
 * Opens a URL in Chrome Custom Tabs with a fallback to regular browser.
 *
 * @param context Android context
 * @param url URL to open
 * @param showShareOption Whether to show share button (default: true)
 */
fun openInChromeTab(
    context: Context,
    url: String,
    showShareOption: Boolean = true
) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .setShareState(
                if (showShareOption) CustomTabsIntent.SHARE_STATE_ON
                else CustomTabsIntent.SHARE_STATE_OFF
            )
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(TOOLBAR_COLOR)
                    .build()
            )
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // Fallback to regular browser if Chrome Custom Tabs fails
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/**
 * Shares an article URL using the system share sheet.
 *
 * @param context Android context
 * @param url URL to share
 * @param title Optional title for the share message
 */
fun shareArticle(
    context: Context,
    url: String,
    title: String? = null
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooserIntent = Intent.createChooser(shareIntent, "Share article").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
}
