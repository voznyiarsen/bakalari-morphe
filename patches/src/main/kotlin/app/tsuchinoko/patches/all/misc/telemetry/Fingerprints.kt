/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.telemetry

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for [java.net.HttpURLConnection.getResponseCode].
 *
 * This method is called for every HTTP response read through HttpURLConnection on Android,
 * including requests made by Dart's [HttpClient] (which the Flutter app uses via Dio).
 * At this point, [HttpURLConnection.getURL] returns the request URL, making it the ideal
 * interception point for blocking ad and telemetry domains regardless of app version.
 */
internal object HttpUrlConnectionFingerprint : Fingerprint(
    definingClass = "Ljava/net/HttpURLConnection;",
    name = "getResponseCode",
    returnType = "I",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC)
)
