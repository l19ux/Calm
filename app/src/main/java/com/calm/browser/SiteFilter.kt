package com.calm.browser

import android.net.Uri

/** Best-effort adult-content + ad blocking. Pair with a family DNS (see README). */
object SiteFilter {
    private val hostBits = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "onlyfans",
        "hentai", "brazzers", "chaturbate", "stripchat", "bongacams", "livejasmin",
        "rule34", "spankbang", "motherless", "fapello", "camsoda", "myfreecams"
    )
    private val words = Regex(
        "\\b(porn\\w*|xxx|nsfw|hentai|nudes?|erotic\\w*|onlyfans|blowjob|milf|camgirls?|sex ?(video|cam|chat|tape)s?)\\b",
        RegexOption.IGNORE_CASE
    )
    private val ads = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "taboola.com",
        "outbrain.com", "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "exoclick.com", "juicyads.com", "trafficjunky.com"
    )
    private val googleHost = Regex("^(www\\.)?google\\.[a-z.]+$")

    fun blocked(u: Uri): Boolean {
        val host = (u.host ?: "").lowercase()
        if (hostBits.any { host.contains(it) }) return true
        val rest = ((u.path ?: "") + " " + (u.query ?: "")).replace(Regex("[-_+/.]"), " ")
        return words.containsMatchIn(rest)
    }

    fun isAd(u: Uri): Boolean {
        val host = (u.host ?: "").lowercase()
        return ads.any { host == it || host.endsWith(".$it") }
    }

    /** Forces strict SafeSearch on Google / Bing / DuckDuckGo. Returns null if nothing to change. */
    fun safeSearch(u: Uri): Uri? {
        val host = (u.host ?: return null).lowercase()
        val (key, value) = when {
            googleHost.matches(host) && u.path == "/search" -> "safe" to "active"
            host.endsWith("bing.com") && u.path == "/search" -> "adlt" to "strict"
            host.endsWith("duckduckgo.com") -> "kp" to "1"
            else -> return null
        }
        if (u.getQueryParameter(key) == value) return null
        val b = u.buildUpon().clearQuery()
        u.queryParameterNames.filter { it != key }.forEach { n ->
            u.getQueryParameters(n).forEach { b.appendQueryParameter(n, it) }
        }
        return b.appendQueryParameter(key, value).build()
    }
}
