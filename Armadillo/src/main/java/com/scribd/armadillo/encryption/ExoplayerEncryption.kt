package com.scribd.armadillo.encryption

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.AesCipherDataSink
import androidx.media3.datasource.AesCipherDataSource
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
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
@OptIn(UnstableApi::class)
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