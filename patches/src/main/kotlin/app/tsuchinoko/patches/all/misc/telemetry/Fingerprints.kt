/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.telemetry

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fingerprint for the Firebase Performance URL tracking method that receives every outgoing
 * request URL. The method parses the URL with [okhttp3.HttpUrl.Companion.d], making it the
 * ideal interception point for blocking telemetry CDN domains regardless of app version
 * (the CDN URLs are stable backend infrastructure).
 */
internal object TelemetryUrlFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        // URL parsing via OkHttp - unique to the URL tracking method.
        methodCall(
            definingClass = "Lokhttp3/HttpUrl\$Companion;",
            name = "d",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lokhttp3/HttpUrl;",
            opcode = Opcode.INVOKE_STATIC
        )
    )
)
