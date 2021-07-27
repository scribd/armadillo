package com.scribd.armadillo.download

import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class MaxAgeCacheEvictorTest {

    private lateinit var cacheEvictor: MaxAgeCacheEvictor
    private lateinit var cache: Cache
    private lateinit var clock: MaxAgeCacheEvictor.Clock

    private companion object {
        const val MAX_BYTES = 10L
        const val CONTENT_MAX_AGE_MILLIS = 10
    }

    private var currentTimeMillis = 1000L

    @Before
    fun setUp() {
        clock = object : MaxAgeCacheEvictor.Clock {
            override fun currentTimeMillis(): Long = currentTimeMillis
        }
        cache = mock()
        cacheEvictor = MaxAgeCacheEvictor(MAX_BYTES, CONTENT_MAX_AGE_MILLIS, clock)
    }

    @Test
    fun onSpanRemoved_cacheIsFull_removesSpanDecrementSize() {
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        cacheEvictor.content[key] = cacheSpan
        cacheEvictor.currentSize = MAX_BYTES

        cacheEvictor.onSpanRemoved(cache, cacheSpan)

        assertThat(cacheEvictor.content.containsKey(key)).isFalse
        assertThat(cacheEvictor.currentSize).isEqualTo(7)
    }

    @Test
    fun onSpanAdded_cacheMiss_addsKey() {
        // Initially it is all empty
        assertThat(cacheEvictor.content.isEmpty()).isTrue
        assertThat(cacheEvictor.expirations.isEmpty()).isTrue
        assertThat(cacheEvictor.currentSize).isEqualTo(0L)
        // Make the call
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        cacheEvictor.onSpanAdded(cache, cacheSpan)
        // Verify Item is added
        assertThat(cacheEvictor.content[key]).isEqualTo(cacheSpan)
        assertThat(cacheEvictor.expirations.poll()).isEqualTo(MaxAgeCacheEvictor.CacheSpanExpiration(key, currentTimeMillis + CONTENT_MAX_AGE_MILLIS))
        assertThat(cacheEvictor.currentSize).isEqualTo(3L)
    }

    @Test
    fun onSpanAdded_cacheHit_updatesValue() {
        // Set up contentAges with a piece of content
        val cacheSpan = CacheSpan("cool", 0, 3L)
        cacheEvictor.onSpanAdded(cache, cacheSpan)

        // Make a new span with the same position & length
        val updatedCacheSpan = CacheSpan("cool2", 0, 3L)

        // Make the call
        cacheEvictor.onSpanTouched(cache, cacheSpan, updatedCacheSpan)
        // Verify item is updated
        assertThat(cacheEvictor.content.size).isEqualTo(1)
        assertThat(cacheEvictor.content[updatedCacheSpan.key()]).isEqualTo(updatedCacheSpan)
        val expectedExpiration = MaxAgeCacheEvictor.CacheSpanExpiration(
            updatedCacheSpan.key(),
            currentTimeMillis + CONTENT_MAX_AGE_MILLIS)
        assertThat(cacheEvictor.expirations.poll()).isEqualTo(expectedExpiration)
        assertThat(cacheEvictor.currentSize).isEqualTo(3L)
    }

    @Test
    fun onSpanAdded_removesExpiredContentOnly() {
        // Setup content
        val spanToExpire1 = CacheSpan("expired1", 0, 3L)
        cacheEvictor.onSpanAdded(cache, spanToExpire1)

        val spanToExpire2 = CacheSpan("expired2", 3, 3L)
        cacheEvictor.onSpanAdded(cache, spanToExpire2)

        assertThat(cacheEvictor.expirations.size).isEqualTo(2)
        assertThat(cacheEvictor.content.size).isEqualTo(2)

        // Advance time so previous content is expired
        currentTimeMillis += CONTENT_MAX_AGE_MILLIS + 1

        // Make the call
        val validContent = CacheSpan("valid", 6, 3L)
        cacheEvictor.onSpanAdded(cache, validContent)

        assertThat(cacheEvictor.content.size).isEqualTo(1)
        assertThat(cacheEvictor.content[validContent.key()]).isEqualTo(validContent)
        assertThat(cacheEvictor.expirations.size).isEqualTo(1)
        assertThat(cacheEvictor.lruArr.size).isEqualTo(1)
    }
}