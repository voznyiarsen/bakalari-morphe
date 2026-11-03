/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.tsuchinoko.patches.all.misc.ads.Constants.COMPATIBILITY_BAKALARI

/**
 * Disables sponsored ad pop-ups by blocking ad-serving CDN URLs at the WebView level.
 *
 * The Flutter app uses [webview_flutter] which includes a WebViewClient
 * ([va/e0]) that handles URL navigation. The
 * [shouldOverrideUrlLoading] method is called for every WebView navigation.
 * The patch checks the requested URL against a blocklist of ad CDN domains.
 * On match, it returns `true` (handled, not loaded), preventing the ad from
 * being displayed.
 *
 * The patch targets the CDN URLs directly, making it app-version agnostic:
 * these are stable backend infrastructure endpoints that do not change between
 * app releases.
 *
 * Blocked domains:
 * - campaign.bakalari.cz, campaign-dev.bakalari.cz (sponsored ad banners)
 * - netglade.cz (ad network)
 */
@Suppress("unused")
val disableSponsoredAdsPatch =
    bytecodePatch(
        name = "Disable sponsored ads",
        description = "Blocks sponsored ad pop-ups by targeting ad CDN URLs at the WebView level (campaign.bakalari.cz, campaign-dev.bakalari.cz, netglade.cz). Version-agnostic."
    ) {
        dependsOn()
        compatibleWith(COMPATIBILITY_BAKALARI)

        execute {
            val method = WebViewClientFingerprint.method

            // Insert at method entry: p2 holds the WebResourceRequest.
            // Extract the URL and check against blocked domains.
            method.addInstructions(
                0,
                """
invoke-interface {p2}, Landroid/webkit/WebResourceRequest;->getUrl()Landroid/net/Uri;
move-result-object v0
invoke-virtual {v0}, Landroid/net/Uri;->toString()Ljava/lang/String;
move-result-object v0

# --- Block campaign.bakalari.cz ---
const-string v1, "campaign.bakalari.cz"
invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v1
if-eqz v1, :skip_campaign_bakalari_cz
const/4 p1, 0x1
return p1
:skip_campaign_bakalari_cz

# --- Block campaign-dev.bakalari.cz ---
const-string v1, "campaign-dev.bakalari.cz"
invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v1
if-eqz v1, :skip_campaign_dev_bakalari_cz
const/4 p1, 0x1
return p1
:skip_campaign_dev_bakalari_cz

# --- Block netglade.cz ---
const-string v1, "netglade.cz"
invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
move-result v1
if-eqz v1, :skip_netglade_cz
const/4 p1, 0x1
return p1
:skip_netglade_cz

# Fall through to original shouldOverrideUrlLoading logic
""".trimIndent()
            )
        }
    }
