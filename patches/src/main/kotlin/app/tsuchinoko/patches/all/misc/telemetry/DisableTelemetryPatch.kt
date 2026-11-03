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
 * Disables sponsored ads and telemetry by blocking requests at the
 * [java.net.HttpURLConnection.getResponseCode] level.
 *
 * The Flutter app uses Dio → Dart [HttpClient] → java.net.HttpURLConnection on Android.
 * Every HTTP request calls [getResponseCode], at which point [HttpURLConnection.getURL]
 * returns the request URL. The patch checks this URL against a blocklist of ad and
 * telemetry domains. On match, a [RuntimeException] is thrown, preventing the response
 * from being read and the ad/telemetry from being loaded.
 *
 * The patch targets the destination domains directly, making it app-version agnostic:
 * these are stable backend infrastructure endpoints that do not change between app releases.
 *
 * Blocked domains:
 * - campaign.bakalari.cz, campaign-dev.bakalari.cz (sponsored ad banners)
 * - netglade.cz (ad network)
 * - app-measurement.com (Google Analytics)
 * - firebase-settings.crashlytics.com (Crashlytics)
 * - seq.service.bakalari.cz (SEQ structured logging)
 * - bakalari-dec2b.firebasestorage.app (Firebase Storage)
 */
@Suppress("unused")
val disableTelemetryPatch =
    bytecodePatch(
        name = "Disable telemetry",
        description = "Blocks sponsored ads and telemetry by targeting destination CDN URLs at the HttpURLConnection level. Version-agnostic."
    ) {
        dependsOn()
        compatibleWith(COMPATIBILITY_BAKALARI)

        execute {
            val method = HttpUrlConnectionFingerprint.method

            // Insert at method entry: p0 = this (HttpURLConnection).
            // Call getURL() to get the request URL, then check against blocked domains.
            method.addInstructions(
                0,
                """
invoke-virtual {p0}, Ljava/net/HttpURLConnection;->getURL()Ljava/net/URL;
move-result-object v1
invoke-virtual {v1}, Ljava/net/URL;->toString()Ljava/lang/String;
move-result-object v1

# --- Block campaign.bakalari.cz ---
const-string v2, "campaign.bakalari.cz"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_campaign_bakalari_cz
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Ad domain blocked: campaign.bakalari.cz"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_campaign_bakalari_cz

# --- Block campaign-dev.bakalari.cz ---
const-string v2, "campaign-dev.bakalari.cz"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_campaign_dev_bakalari_cz
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Ad domain blocked: campaign-dev.bakalari.cz"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_campaign_dev_bakalari_cz

# --- Block netglade.cz ---
const-string v2, "netglade.cz"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_netglade_cz
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Ad domain blocked: netglade.cz"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_netglade_cz

# --- Block app-measurement.com (Google Analytics) ---
const-string v2, "app-measurement.com"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_app_measurement_com
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: app-measurement.com"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_app_measurement_com

# --- Block firebase-settings.crashlytics.com (Crashlytics) ---
const-string v2, "firebase-settings.crashlytics.com"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_firebase_settings_crashlytics_com
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: firebase-settings.crashlytics.com"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_firebase_settings_crashlytics_com

# --- Block seq.service.bakalari.cz (SEQ logging) ---
const-string v2, "seq.service.bakalari.cz"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v2
if-eqz v2, :skip_seq_service_bakalari_cz
new-instance v2, Ljava/lang/RuntimeException;
const-string v3, "Telemetry domain blocked: seq.service.bakalari.cz"
invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
throw v2
:skip_seq_service_bakalari_cz

# --- Block bakalari-dec2b.firebasestorage.app (Firebase Storage) ---
const-string v2, "bakalari-dec2b.firebasestorage.app"
invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
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
