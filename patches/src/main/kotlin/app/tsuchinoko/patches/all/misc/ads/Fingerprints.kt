/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for [android.webkit.WebViewClient.shouldOverrideUrlLoading].
 *
 * The [webview_flutter] plugin's WebViewClient ([va/e0]) handles URL navigation
 * for WebViews in the app. Sponsored ad pop-ups may be loaded in a WebView, making
 * this the ideal interception point for blocking ad domains at the WebView level.
 */
internal object WebViewClientFingerprint : Fingerprint(
    definingClass = "Lva/e0;",
    name = "shouldOverrideUrlLoading",
    returnType = "Z",
    parameters = listOf(
        "Landroid/webkit/WebView;",
        "Landroid/webkit/WebResourceRequest;"
    ),
    accessFlags = listOf(AccessFlags.PUBLIC)
)
