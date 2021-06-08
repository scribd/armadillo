package com.scribd.armadillo.download

import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class MaxAgeCacheEvictorTest {

    private lateinit var cacheEvictor: MaxAgeCacheEvictor
    private lateinit var cache: Cache
    private lateinit var clock: MaxAgeCacheEvictor.Clock

    private companion object {
        const val MAX_BYTES = 10L
        const val CONTENT_MAX_AGE_MILLIS = 10
        const val CURRENT_TIME_MILLIS = 1000L
    }

    @Before
    fun setUp() {
        clock = mock()
        whenever(clock.currentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS)
        cache = mock()
        cacheEvictor = MaxAgeCacheEvictor(MAX_BYTES, CONTENT_MAX_AGE_MILLIS, clock)
    }

    @Test
    fun onSpanRemoved_cacheIsFull_removesSpanDecrementSize() {
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        cacheEvictor.contentAges[key] = MaxAgeCacheEvictor.CachePair(cacheSpan, CURRENT_TIME_MILLIS)
        cacheEvictor.currentSize = MAX_BYTES

        cacheEvictor.onSpanRemoved(cache, cacheSpan)

        assertThat(cacheEvictor.contentAges.containsKey(key)).isFalse
        assertThat(cacheEvictor.currentSize).isEqualTo(7)
    }

    @Test
    fun onSpanAdded_cacheMiss_addsKey() {
        // Initially it is all empty
        assertThat(cacheEvictor.contentAges.isEmpty()).isTrue
        assertThat(cacheEvictor.currentSize).isEqualTo(0L)
        // Make the call
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        cacheEvictor.onSpanAdded(cache, cacheSpan)
        // Verify Item is added
        val expectedCachePair = MaxAgeCacheEvictor.CachePair(cacheSpan, CURRENT_TIME_MILLIS + CONTENT_MAX_AGE_MILLIS)
        assertThat(cacheEvictor.contentAges[key]).isEqualTo(expectedCachePair)
        assertThat(cacheEvictor.currentSize).isEqualTo(3L)
    }

    @Test
    fun onSpanAdded_cacheHit_updatesValue() {
        // Set up contentAges with a piece of content
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        val initialPair = MaxAgeCacheEvictor.CachePair(cacheSpan, CURRENT_TIME_MILLIS + CONTENT_MAX_AGE_MILLIS)
        cacheEvictor.contentAges[key] = initialPair
        cacheEvictor.currentSize = 3L
        // Make a new span with the same position & length
        val updatedCacheSpan = CacheSpan("cool2", 0, 3L)
        // Make the call
        cacheEvictor.onSpanAdded(cache, updatedCacheSpan)
        // Verify item is updated
        assertThat(cacheEvictor.contentAges.size).isEqualTo(1)
        assertThat(cacheEvictor.contentAges[updatedCacheSpan.key()]!!.cacheSpan).isEqualTo(updatedCacheSpan)
        assertThat(cacheEvictor.currentSize).isEqualTo(3L)
    }

    @Test
    fun evictCache_removesExpiredContentOnly() {
        // Setup content
        val expiredContentSpan1 = CacheSpan("expired1", 0, 3L)
        val expiredContent1 = MaxAgeCacheEvictor.CachePair(expiredContentSpan1, CURRENT_TIME_MILLIS - 1)

        val expiredContentSpan2 = CacheSpan("expired2", 3, 3L)
        val expiredContent2 = MaxAgeCacheEvictor.CachePair(expiredContentSpan2, CURRENT_TIME_MILLIS - 2)

        val notExpiredContentSpan = CacheSpan("valid", 6, 3L)
        val notExpiredContent = MaxAgeCacheEvictor.CachePair(notExpiredContentSpan, CURRENT_TIME_MILLIS + 1)

        cacheEvictor.contentAges[expiredContentSpan1.key()] = expiredContent1
        cacheEvictor.contentAges[expiredContentSpan2.key()] = expiredContent2
        cacheEvictor.contentAges[notExpiredContentSpan.key()] = notExpiredContent

        // Make the call & Assert
        assertThat(cacheEvictor.oldestCacheSpan()).isEqualTo(expiredContent2)
        cacheEvictor.contentAges.remove(expiredContentSpan2.key())

        assertThat(cacheEvictor.oldestCacheSpan()).isEqualTo(expiredContent1)
        cacheEvictor.contentAges.remove(expiredContentSpan1.key())

        assertThat(cacheEvictor.oldestCacheSpan()).isEqualTo(notExpiredContent)
    }
}