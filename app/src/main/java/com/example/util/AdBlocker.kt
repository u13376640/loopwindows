package com.example.util

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object AdBlocker {

    private const val TAG = "LoopAdBlocker"

    // OkHttpClient with aggressive timeouts for DNS queries
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
    }

    // Thread-safe in-memory DNS resolution cache: host -> isBlocked
    private val dnsBlockedCache = ConcurrentHashMap<String, Boolean>()

    // Local high-speed pre-seeded ad domains (standard AdGuard & EasyList Top Ad Servers)
    private val KNOWN_AD_DOMAINS = hashSetOf(
        // Google / DoubleClick
        "doubleclick.net",
        "googlesyndication.com",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "admob.com",
        "google-analytics.com",
        "partner.googleadservices.com",
        "adservice.google.com",

        // High-frequency popunder & video redirect networks common on streaming/blogspot
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "propellerclick.com",
        "adsterra.com",
        "exoclick.com",
        "monetag.com",
        "adcash.com",
        "hilltopads.net",
        "clickadu.com",
        "trafficstars.com",
        "juicyads.com",
        "richpush.com",
        "rollerads.com",
        "adtrue.com",
        "onclicksuper.com",
        "onclickperformance.com",
        "admaven.com",
        "yllix.com",
        "a-ads.com",
        "effectivecpmgate.com",
        "highcpmgate.com",
        "highratecpm.com",
        "alwingulla.com",
        "creativecdn.com",
        "histats.com",
        "whos.amung.us",
        "vlitag.com",
        "serving-sys.com",
        "infolinks.com",
        "mgid.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "bidswitch.net",
        "yieldmo.com",
        "casalemedia.com",
        "teads.tv",
        "scorecardresearch.com",
        "quantserve.com",
        "zemanta.com",
        "media.net",
        "advertising.com",
        "yandexadexchange.net",
        "aniview.com",
        "primis.tech",
        "undertone.com",
        "connatix.com",
        "lijit.com",
        "sovrn.com",
        "zergnet.com",
        "adform.net",
        "smartadserver.com",
        "sharethrough.com",
        "revcontent.com"
    )

    private val AD_KEYWORDS = listOf(
        "/ads.js",
        "/ad-banner",
        "/popunder",
        "/popup.js",
        "googlesyndication",
        "doubleclick",
        "/pagead/",
        "ad_type=",
        "ad_system=",
        "banner_ad",
        "clicktag",
        "cpmgate",
        "onclickperformance",
        "effectivecpm"
    )

    init {
        // Pre-fill local cache with known ad domains
        for (domain in KNOWN_AD_DOMAINS) {
            dnsBlockedCache[domain] = true
        }
    }

    /**
     * Checks if a request is an advertisement using both local list and AdGuard DNS (DoH).
     */
    fun isAd(uri: Uri): Boolean {
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false

        // Never block video stream fragments or video CDNs
        val urlString = uri.toString().lowercase(Locale.ROOT)
        if (urlString.contains(".m3u8") || urlString.contains(".mp4") || urlString.contains(".ts") || urlString.contains(".mpd") || urlString.contains(".webm") || host.contains("googlevideo.com")) {
            return false
        }

        // Never block primary content or trusted domain roots
        if (host.contains("blogspot.com") || host.contains("blogger.com") || host.contains("google.com/recaptcha")) {
            // Check path keywords for blogger embedded ads
            return AD_KEYWORDS.any { urlString.contains(it) }
        }

        // 1. Check in-memory DNS cache
        dnsBlockedCache[host]?.let { return it }

        // 2. Check subdomain match against local known ad domains
        for (adDomain in KNOWN_AD_DOMAINS) {
            if (host == adDomain || host.endsWith(".$adDomain")) {
                dnsBlockedCache[host] = true
                return true
            }
        }

        // 3. Check path keywords
        for (keyword in AD_KEYWORDS) {
            if (urlString.contains(keyword)) {
                dnsBlockedCache[host] = true
                return true
            }
        }

        // 4. AdGuard Private DNS Query (DoH - DNS over HTTPS)
        // AdGuard DNS returns IP 0.0.0.0 for all ad/tracking domains.
        val isBlockedByAdguard = queryAdGuardDns(host)
        dnsBlockedCache[host] = isBlockedByAdguard
        return isBlockedByAdguard
    }

    /**
     * Queries AdGuard DNS-over-HTTPS (DoH) service.
     * AdGuard DNS responds with 0.0.0.0 (or NXDOMAIN status 3) for blocked ad domains.
     */
    private fun queryAdGuardDns(host: String): Boolean {
        return try {
            val url = "https://dns.adguard-dns.com/resolve?name=$host&type=A"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return false

            val body = response.body?.string() ?: return false
            val json = JSONObject(body)

            // Status 3 is NXDOMAIN (domain rejected/blocked)
            val status = json.optInt("Status", 0)
            if (status == 3) {
                return true
            }

            // Inspect answers for 0.0.0.0 or blocked indicators
            val answers = json.optJSONArray("Answer")
            if (answers != null) {
                for (i in 0 until answers.length()) {
                    val answerObj = answers.optJSONObject(i)
                    val data = answerObj?.optString("data", "") ?: ""
                    if (data == "0.0.0.0" || data == "127.0.0.1" || data == "::") {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            // On timeout or offline, default to safe false to avoid blocking legitimate requests
            false
        }
    }

    /**
     * Creates an empty WebResourceResponse to cleanly kill the blocked resource.
     */
    fun createEmptyResourceResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Aggressive cosmetic injection:
     * - Injects strict CSS to hide all ad divs, banners, popups, and transparent click overlays
     * - Neutralizes rogue clickjackers, window.open, and redirect listeners on video player embeds
     */
    val AD_HIDING_CSS = """
        (function() {
            // 1. Inject aggressive CSS rules
            var css = `
                .adsbygoogle,
                [id*="google_ads"],
                [id*="ad-container"],
                [id*="ad_wrapper"],
                [class*="ad-banner"],
                [class*="sponsored-post"],
                [class*="advertisement"],
                [class*="ad-box"],
                .widget.AdSense,
                div[id^="AdSense"],
                iframe[src*="doubleclick"],
                iframe[src*="adsterra"],
                iframe[src*="popads"],
                iframe[src*="exoclick"],
                iframe[src*="onclick"],
                iframe[src*="monetag"],
                div[id*="ad_unit"],
                div[class*="ad_unit"],
                div[id*="ad-slot"],
                div[class*="ad-slot"],
                div[class*="overlay-ad"],
                div[id*="pop-overlay"],
                a[target="_blank"][href*="cpm"],
                a[target="_blank"][href*="ads"] {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    width: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
            `;
            var style = document.getElementById('loop-adguard-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'loop-adguard-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                (document.head || document.documentElement).appendChild(style);
            }
            
            // 2. Continuous DOM cleaner to eliminate dynamically inserted overlay ad frames
            function cleanAdNodes() {
                var badSelectors = [
                    '.adsbygoogle',
                    '.widget.AdSense',
                    'iframe[src*="ads"]',
                    'iframe[src*="doubleclick"]',
                    'div[id*="ad-container"]',
                    'div[id*="ad_unit"]'
                ];
                badSelectors.forEach(function(sel) {
                    var els = document.querySelectorAll(sel);
                    for (var i = 0; i < els.length; i++) {
                        els[i].remove();
                    }
                });
            }
            cleanAdNodes();

            // 3. Neutralize rogue window.open and popup clickjackers
            if (!window._loopPopupNeutralized) {
                window._loopPopupNeutralized = true;
                window.open = function(url, target, features) {
                    return null;
                };
            }
        })();
    """.trimIndent()
}
