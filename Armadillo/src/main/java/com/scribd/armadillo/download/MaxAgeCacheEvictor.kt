package com.scribd.armadillo.download

import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan

/**
 * This cache evictor ensures that cached content older then maxAgeMillis is not served.
 *
 * Future improvements of this evictor should:
 * - have a more performant oldestCacheSpan method
 * - also introduce LRU behavior
 */
internal class MaxAgeCacheEvictor(private val maxBytes: Long,
                                  private val contentMaxAgeMillis: Int = CONTENT_MAX_AGE_MILLIS,
                                  private val clock: Clock = object : Clock {
                                      override fun currentTimeMillis(): Long = System.currentTimeMillis()
                                  }) : CacheEvictor {
    @VisibleForTesting
    internal interface Clock {
        fun currentTimeMillis(): Long
    }

    @VisibleForTesting
    internal val contentAges = mutableMapOf<Int, CachePair>()

    @VisibleForTesting
    internal data class CachePair(var cacheSpan: CacheSpan, val expiration: Long)

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
        if (contentAges.containsKey(key)) {
            val previous = contentAges[key]!!
            contentAges[key] = previous.copy(cacheSpan = span)
        } else {
            contentAges[key] = CachePair(span, expirationTimestamp())
            currentSize += span.length
        }
        evictCache(cache, 0)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        contentAges.remove(span.key())
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {

        var oldestSpan = oldestCacheSpan()

        while (oldestSpan?.isExpired() == true) {
            cache.removeSpan(oldestSpan.cacheSpan)

            oldestSpan = oldestCacheSpan()
        }

        while (currentSize + requiredSpace > maxBytes && oldestSpan != null) {
            oldestSpan.cacheSpan.let {
                cache.removeSpan(it)
            }
            oldestSpan = oldestCacheSpan()
        }
    }

    @VisibleForTesting
    internal fun oldestCacheSpan(): CachePair? = contentAges.minByOrNull { it.value.expiration }?.value

    private fun expirationTimestamp() = clock.currentTimeMillis() + contentMaxAgeMillis

    private fun CachePair.isExpired(): Boolean = clock.currentTimeMillis() > expiration

}

@VisibleForTesting
internal fun CacheSpan.key(): Int = this.toString().hashCode()
