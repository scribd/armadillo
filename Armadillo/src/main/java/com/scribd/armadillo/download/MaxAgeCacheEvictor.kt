package com.scribd.armadillo.download

import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.Deque
import java.util.LinkedList

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
     * Tracks how long a span has been cached for. Only be modified when a span is added or removed. An
     * update does not trigger changes. The OS often moves around [CacheSpan]s internally to optimize storage, these
     * updates do not correspond to refreshes with the network and therefore do not modify the expiration times.
     */
    @VisibleForTesting
    internal val expirations: Deque<CacheSpanExpiration> = LinkedList()

    /**
     * Handles lru ordering.
     * New elements are enqueued to head and old are dequeued from tail
     */
    @VisibleForTesting
    internal val lruArr: Deque<Int> = LinkedList()

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
            expirations.addFirst(CacheSpanExpiration(key, expirationTimestamp()))
            currentSize += span.length
        }

        lruArr.remove(key)
        lruArr.addFirst(key)
        content[key] = span
        evictCache(cache, 0)
    }

    /**
     * This is triggered by calls in [evictCache] to [Cache.removeSpan]
     *
     * It is expected that this method will return because all the work is already done in [evictCache].
     */
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        val key = span.key()
        content[key] ?: return
        content.remove(key)
        lruArr.remove(key)
        removeExpiredContent(key)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {

        // remove expired content
        while (expirations.peekLast()?.isExpired() == true) {

            val expiredWrapper = expirations.pollLast() ?: continue
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

    private fun removeExpiredContent(key: Int) {
        val iterator = expirations.iterator()
        while (iterator.hasNext()) {
            val current = iterator.next()
            if (current.key == key) {
                expirations.remove(current)
                break
            }
        }
    }

    private fun CacheSpanExpiration.isExpired() = clock.currentTimeMillis() > expiration
    private fun expirationTimestamp() = clock.currentTimeMillis() + contentMaxAgeMillis
}

@VisibleForTesting
internal fun CacheSpan.key(): Int = this.toString().hashCode()
