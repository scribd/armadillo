package com.scribd.armadillo.download

import android.content.Context
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An optimization for improving the performance of [SimpleCache] by caching indexes to improve downloading speeds.
 */
interface ArmadilloDatabaseProvider {
    val database: DatabaseProvider
}

@Singleton
class ArmadilloDatabaseProviderImpl @Inject constructor(context: Context) : ArmadilloDatabaseProvider {
    override val database: DatabaseProvider = ExoDatabaseProvider(context)
}