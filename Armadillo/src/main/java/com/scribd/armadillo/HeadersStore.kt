package com.scribd.armadillo

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeadersStore @Inject constructor() {
    private val headersMap = mutableMapOf<String, Map<String, String>>().withDefault { emptyMap() }

    fun headersForKey(key: String): Map<String, String>? = headersMap[key]

    fun setHeaders(key: String, headers: Map<String, String>) {
        headersMap[key] = headers
    }

    fun keyForUrl(url: String): String? {
        // Matches a numeric segment of a URL. For example, "12345" in www.coolhost.com/audio/12345/master.m3u8
        // TODO [29]: Remove this once exoplayer is fixed
        return Regex("""/(\d+)/""").find(url)?.value
    }
}