package com.scribd.armadillo.encryption

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSink
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSink
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSource
import javax.inject.Inject
import javax.inject.Singleton

interface ExoplayerEncryption {
    fun dataSinkFactory(downloadCache: Cache): DataSink.Factory
    fun dataSourceFactory(upstream: DataSource.Factory): DataSource.Factory
}

/**
 * This class provides the plumbing for encrypting downloaded content & then reading this encrypted content.
 */
@Singleton
internal class ExoplayerEncryptionImpl @Inject constructor(applicationContext: Context) : ExoplayerEncryption {

    private val secureStorage: SecureStorage = ArmadilloSecureStorage()
    private val secret = secureStorage.downloadSecretKey(applicationContext)

    override fun dataSinkFactory(downloadCache: Cache) = DataSink.Factory {
        val scratch = ByteArray(3897) // size selected from exoplayer unit tests.
        return@Factory AesCipherDataSink(secret, CacheDataSink(downloadCache, Long.MAX_VALUE), scratch)
    }

    override fun dataSourceFactory(upstream: DataSource.Factory) =
            DataSource.Factory { AesCipherDataSource(secret, upstream.createDataSource()) }

}