package com.scribd.armadillo.download

import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class MaxAgeCacheEvictorTest {
    private companion object {
        const val MAX_BYTES = 10L
        const val CONTENT_MAX_AGE_MILLIS = 10
    }

    private lateinit var cacheEvictor: MaxAgeCacheEvictor
    private lateinit var cache: Cache
    private lateinit var fakeClock: MaxAgeCacheEvictor.Clock

    private var currentTimeMillis = 1000L

    @Before
    fun setUp() {
        currentTimeMillis = 1000L
        fakeClock = object : MaxAgeCacheEvictor.Clock {
            override fun currentTimeMillis(): Long = currentTimeMillis
        }
        cache = mock()
        cacheEvictor = MaxAgeCacheEvictor(MAX_BYTES, CONTENT_MAX_AGE_MILLIS, fakeClock)
    }

    @Test
    fun onSpanRemoved_hasItem_removesSpanDecrementSize() {
        val cacheSpan = CacheSpan("cool", 0, 3L)
        // add span
        cacheEvictor.onSpanAdded(cache, cacheSpan)

        // remove span
        cacheEvictor.onSpanRemoved(cache, cacheSpan)

        // Assert outcome
        assertThat(cacheEvictor.content.containsKey(cacheSpan.key())).isFalse
        assertThat(cacheEvictor.expirations.isEmpty()).isTrue
        assertThat(cacheEvictor.currentSize).isEqualTo(0)
    }

    @Test
    fun onSpanAdded_cacheMiss_addsKey() {
        // Add the span
        val cacheSpan = CacheSpan("cool", 0, 3L)
        val key = cacheSpan.key()
        cacheEvictor.onSpanAdded(cache, cacheSpan)

        // Verify item is added
        assertThat(cacheEvictor.content[key]).isEqualTo(cacheSpan)
        assertThat(cacheEvictor.expirations.pollLast()).isEqualTo(
            MaxAgeCacheEvictor.CacheSpanExpiration(key, currentTimeMillis + CONTENT_MAX_AGE_MILLIS))
        assertThat(cacheEvictor.currentSize).isEqualTo(3L)
    }

    @Test
    fun onSpanAdded_cacheHit_updatesValue() {
        // Set up contentAges with a piece of content
        val cacheSpan = CacheSpan("cool", 0, 3L)
        cacheEvictor.onSpanAdded(cache, cacheSpan)

        // Make a new span with the same position & length
        val updatedCacheSpan = CacheSpan("cool2", 0, 3L)

        // Make the call & advance time
        val initialTimeMillis = currentTimeMillis
        currentTimeMillis += 10
        cacheEvictor.onSpanTouched(cache, cacheSpan, updatedCacheSpan)

        // Verify expiration is not modified
        val expectedExpiration = MaxAgeCacheEvictor.CacheSpanExpiration(
            updatedCacheSpan.key(),
            initialTimeMillis + CONTENT_MAX_AGE_MILLIS)
        assertThat(cacheEvictor.expirations.pollLast()).isEqualTo(expectedExpiration)

        assertThat(cacheEvictor.content.size).isEqualTo(1)
        assertThat(cacheEvictor.content[updatedCacheSpan.key()]).isEqualTo(updatedCacheSpan)
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

        // verify expired content is removed
        assertThat(cacheEvictor.content.size).isEqualTo(1)
        assertThat(cacheEvictor.content[validContent.key()]).isEqualTo(validContent)
        assertThat(cacheEvictor.expirations.size).isEqualTo(1)
        assertThat(cacheEvictor.expirations.pollLast()!!.key).isEqualTo(validContent.key())
        assertThat(cacheEvictor.lruArr.size).isEqualTo(1)
    }

    @Test
    fun lruOrdering_itemsAdded_lruOrdering() {
        val spanA = CacheSpan("spanA", 0, 2L)
        val spanB = CacheSpan("spanB", 2, 2L)
        val spanC = CacheSpan("spanC", 4, 2L)
        val spans = listOf(spanA, spanB, spanC)

        // Add spans
        spans.forEach {
            cacheEvictor.onSpanAdded(cache, it)
        }

        // verify ordering
        cacheEvictor.lruArr.reversed().forEachIndexed { index, spanKey ->
            assertThat(spanKey).isEqualTo(spans[index].key())
        }
    }

    @Test
    fun lruOrdering_itemsUpdated_lruOrdering() {
        val spanA = CacheSpan("spanA", 0, 2L)
        val spanB = CacheSpan("spanB", 2, 2L)
        val spanC = CacheSpan("spanC", 4, 2L)
        val spans = listOf(spanA, spanB, spanC)

        // Add spans
        spans.forEach {
            cacheEvictor.onSpanAdded(cache, it)
        }

        // updates
        cacheEvictor.onSpanAdded(cache, spanB)

        // verify ordering
        val lruArr = cacheEvictor.lruArr
        assertThat(lruArr.pollLast()!!).isEqualTo(spanA.key())
        assertThat(lruArr.pollLast()!!).isEqualTo(spanC.key())
        assertThat(lruArr.pollLast()!!).isEqualTo(spanB.key())
    }
}