/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.tsuchinoko.patches.all.misc.telemetry.Constants.COMPATIBILITY_BAKALARI

/**
 * Disables telemetry by blocking requests to telemetry-serving CDN URLs.
 *
 * The patch hooks the Firebase Performance URL tracking method at its entry point, where
 * [p1] holds the outgoing request URL string. The URL is checked against a blocklist of
 * telemetry domains; on match, a [RuntimeException] is thrown, causing the connection setup
 * to fail and preventing telemetry from being sent.
 *
 * The patch targets the CDN URLs directly, making it app-version agnostic: these are stable
 * backend infrastructure endpoints that do not change between app releases.
 *
 * Blocked domains:
 * - app-measurement.com (Google Analytics)
 * - firebase-settings.crashlytics.com (Crashlytics settings)
 * - seq.service.bakalari.cz (SEQ structured logging)
 * - bakalari-dec2b.firebasestorage.app (Firebase Storage)
 */
@Suppress("unused")
val disableTelemetryPatch =
    bytecodePatch(
        name = "Disable telemetry",
        description = "Blocks telemetry and analytics by targeting the CDN URLs (app-measurement.com, firebase-settings.crashlytics.com, seq.service.bakalari.cz, bakalari-dec2b.firebasestorage.app). Version-agnostic."
    ) {
        dependsOn()
        compatibleWith(COMPATIBILITY_BAKALARI)

        execute {
            val method = TelemetryUrlFingerprint.method

            // Insert at method entry: p1 holds the URL string parameter.
            method.addInstructions(
                0,
                """
const-string v2, "app-measurement.com"
invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_app_measurement_com
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: app-measurement.com"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_app_measurement_com

const-string v2, "firebase-settings.crashlytics.com"
invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_firebase_settings_crashlytics_com
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: firebase-settings.crashlytics.com"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_firebase_settings_crashlytics_com

const-string v2, "seq.service.bakalari.cz"
invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_seq_service_bakalari_cz
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: seq.service.bakalari.cz"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_seq_service_bakalari_cz

const-string v2, "bakalari-dec2b.firebasestorage.app"
invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_bakalari_dec2b_firebasestorage_app
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: bakalari-dec2b.firebasestorage.app"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_bakalari_dec2b_firebasestorage_app
""".trimIndent()
            )
        }
    }
