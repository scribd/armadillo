package com.scribd.armadillo.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An optimization for improving the performance of [SimpleCache] by caching indexes to improve downloading speeds.
 */
interface ArmadilloDatabaseProvider {
    val database: DatabaseProvider
}

@UnstableApi
@Singleton
class ArmadilloDatabaseProviderImpl @Inject constructor(context: Context) : ArmadilloDatabaseProvider {
    override val database: DatabaseProvider = StandaloneDatabaseProvider(context)
}