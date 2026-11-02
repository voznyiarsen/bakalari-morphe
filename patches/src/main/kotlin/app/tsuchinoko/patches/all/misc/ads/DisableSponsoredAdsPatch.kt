/*
 * Copyright (C) 2026 tsuchinoko <https://github.com/voznyiarsen/bakalari-morphe>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.tsuchinoko.patches.all.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import app.tsuchinoko.patches.all.misc.ads.Constants.COMPATIBILITY_BAKALARI

/**
 * Disables sponsored ad pop-ups by blocking requests to the ad-serving CDN URLs.
 *
 * The patch hooks the Firebase Performance URL tracking method that processes every outgoing
 * request URL. After the URL's host is extracted via [okhttp3.HttpUrl.h], the patch checks it
 * against a blocklist of ad CDN domains. If a match is found, a [RuntimeException] is thrown,
 * which causes the connection setup to fail and prevents the ad from being loaded.
 *
 * The patch targets the CDN URLs directly (campaign.bakalari.cz, campaign-dev.bakalari.cz,
 * netglade.cz), making it app-version agnostic: these are stable backend infrastructure
 * endpoints that do not change between app releases.
 */
@Suppress("unused")
val disableSponsoredAdsPatch =
    bytecodePatch(
        name = "Disable sponsored ads",
        description = "Blocks sponsored ad pop-ups by targeting the ad CDN URLs (campaign.bakalari.cz, campaign-dev.bakalari.cz, netglade.cz). Version-agnostic."
    ) {
        dependsOn()
        compatibleWith(COMPATIBILITY_BAKALARI)

        execute {
            val method = AdUrlFingerprint.method

            // Find the instruction that extracts the host from the parsed URL.
            // At this point, p1 holds the host string (e.g. "campaign.bakalari.cz").
            val hostInsIndex = method.indexOfFirstInstructionOrThrow(
                fieldAccess(
                    definingClass = "Lokhttp3/HttpUrl;",
                    name = "h",
                    type = "Ljava/lang/String;",
                    opcode = Opcode.IGET_OBJECT
                )
            )

            method.addInstructionsWithLabels(
                hostInsIndex + 1,
                """
                    # p1 = host string at this injection point

                    # --- Block campaign.bakalari.cz ---
                    const-string v2, "campaign.bakalari.cz"
                    invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v2
                    if-eqz v2, :skip_campaign_bakalari_cz
                    new-instance v2, Ljava/lang/RuntimeException;
                    const-string v3, "Sponsored ad domain blocked: campaign.bakalari.cz"
                    invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
                    throw v2
                    :skip_campaign_bakalari_cz

                    # --- Block campaign-dev.bakalari.cz ---
                    const-string v2, "campaign-dev.bakalari.cz"
                    invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v2
                    if-eqz v2, :skip_campaign_dev_bakalari_cz
                    new-instance v2, Ljava/lang/RuntimeException;
                    const-string v3, "Sponsored ad domain blocked: campaign-dev.bakalari.cz"
                    invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
                    throw v2
                    :skip_campaign_dev_bakalari_cz

                    # --- Block netglade.cz ---
                    const-string v2, "netglade.cz"
                    invoke-virtual {p1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v2
                    if-eqz v2, :skip_netglade_cz
                    new-instance v2, Ljava/lang/RuntimeException;
                    const-string v3, "Sponsored ad domain blocked: netglade.cz"
                    invoke-direct {v2, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
                    throw v2
                    :skip_netglade_cz
                """.trimIndent()
            )
        }
    }
