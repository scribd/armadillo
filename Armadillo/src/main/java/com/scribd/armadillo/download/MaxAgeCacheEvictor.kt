package com.scribd.armadillo.download

import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.LinkedList
import java.util.Queue

/**
 * This cache evictor will:
 *  - preferentially remove content that is older then [contentMaxAgeMillis]
 *  - then function as an lru cache for valid content
 *  - if [Int.MAX_VALUE] is passed for [contentMaxAgeMillis], this cache will function as an LRU cache
 */
internal class MaxAgeCacheEvictor(
    private val maxBytes: Long,
    private val contentMaxAgeMillis: Int = CONTENT_MAX_AGE_MILLIS,
    private val clock: Clock = object : Clock {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()
    },
) : CacheEvictor {
    @VisibleForTesting
    internal interface Clock {
        fun currentTimeMillis(): Long
    }

    @VisibleForTesting
    internal data class CacheSpanExpiration(val key: Int, val expiration: Long)

    @VisibleForTesting
    internal val content = mutableMapOf<Int, CacheSpan>()

    /**
     * This structure will track how long a span has been cached for. This should only be modified when a span is added or removed. An
     * updated span should not trigger any changes here. The OS often moves around [CacheSpan]s internally to optimize storage, these
     * updates do not correspond to refreshes with the network and therefore should not modify the expiration times.
     */
    @VisibleForTesting
    internal val expirations : Queue<CacheSpanExpiration> = LinkedList()

    /**
     * This structure will handle lru ordering
     */
    @VisibleForTesting
    internal val lruArr = LinkedList<Int>()

    @VisibleForTesting
    internal var currentSize: Long = 0

    private companion object {
        const val CONTENT_MAX_AGE_MILLIS = 1000 * 60 * 60 * 12
    }

    override fun requiresCacheSpanTouches() = true

    override fun onCacheInitialized() {
        // Do nothing.
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(cache, length)
        }
    }


    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        val key = span.key()
        if (!content.containsKey(key)) {
            expirations.add(CacheSpanExpiration(key, expirationTimestamp()))
            currentSize += span.length
        }

        lruArr.remove(key)
        lruArr.add(key)
        content[key] = span
        evictCache(cache, 0)
    }

    /**
     * This triggered by calls in [evictCache] to [Cache.removeSpan]
     *
     * It is expected that this method will return because all the work is already done in [evictCache].
     * [expirations] is not handled here as it is already handled in [evictCache]. If the api changes and this
     * method gets called at other times, discrepancies will not cause issues.
     */
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        val key = span.key()
        content[key] ?: return
        content.remove(key)
        lruArr.remove(key)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {

        // remove expired content
        while (expirations.peek()?.expiration?.isExpired() == true) {

            val expiredWrapper = expirations.poll() ?: continue
            val key = expiredWrapper.key
            val expiredSpan = content[key] ?: continue

            lruArr.remove(key)
            content.remove(key)
            currentSize -= expiredSpan.length

            /**
             * Triggers [onSpanRemoved]
             */
            cache.removeSpan(expiredSpan)
        }

        // LRU behavior
        while (currentSize + requiredSpace > maxBytes && lruArr.peekLast() != null) {
            val key = lruArr.pollLast() ?: break
            val cacheSpan = content[key] ?: break
            currentSize -= cacheSpan.length

            /**
             * Triggers [onSpanRemoved]
             */
            cache.removeSpan(cacheSpan)
        }
    }

    private fun expirationTimestamp() = clock.currentTimeMillis() + contentMaxAgeMillis

    private fun Long.isExpired(): Boolean = clock.currentTimeMillis() > this
}

@VisibleForTesting
internal fun CacheSpan.key(): Int = this.toString().hashCode()
