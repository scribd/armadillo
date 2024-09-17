package com.scribd.armadillo.di

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.offline.DownloaderFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.scribd.armadillo.Constants
import com.scribd.armadillo.download.ArmadilloDatabaseProvider
import com.scribd.armadillo.download.ArmadilloDatabaseProviderImpl
import com.scribd.armadillo.download.ArmadilloDownloadManagerFactory
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.download.CacheManagerImpl
import com.scribd.armadillo.download.DefaultExoplayerDownloadService
import com.scribd.armadillo.download.DownloadEngine
import com.scribd.armadillo.download.DownloadManagerFactory
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.download.ExoplayerDownloadEngine
import com.scribd.armadillo.download.ExoplayerDownloadTracker
import com.scribd.armadillo.download.HeaderAwareDownloaderFactory
import com.scribd.armadillo.download.MaxAgeCacheEvictor
import com.scribd.armadillo.encryption.ArmadilloSecureStorage
import com.scribd.armadillo.encryption.ExoplayerEncryption
import com.scribd.armadillo.encryption.ExoplayerEncryptionImpl
import com.scribd.armadillo.encryption.SecureStorage
import com.scribd.armadillo.exoplayerExternalDirectory
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
internal class DownloadModule {
    @Singleton
    @Provides
    fun databaseProvider(databaseProvider: ArmadilloDatabaseProviderImpl): ArmadilloDatabaseProvider = databaseProvider

    @Singleton
    @Provides
    @Named(Constants.DI.DOWNLOAD_CACHE)
    fun downloadCache(@Named(Constants.DI.EXOPLAYER_DIRECTORY) exoplayerDirectory: File, databaseProvider: ArmadilloDatabaseProvider): Cache {
        val downloadContentDirectory = File(exoplayerDirectory, Constants.Exoplayer.EXOPLAYER_DOWNLOADS_DIRECTORY)
        return SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider.database)
    }

    @Singleton
    @Provides
    @Named(Constants.DI.PLAYBACK_CACHE)
    fun playbackCache(@Named(Constants.DI.EXOPLAYER_DIRECTORY) exoplayerDirectory: File,
                      databaseProvider: ArmadilloDatabaseProvider): Cache {
        val playbackCacheDir = File(exoplayerDirectory, Constants.Exoplayer.EXOPLAYER_PLAYBACK_CACHE_DIRECTORY)
        return SimpleCache(
            playbackCacheDir,
            MaxAgeCacheEvictor(Constants.Exoplayer.MAX_PLAYBACK_CACHE_SIZE),
            databaseProvider.database
        )
    }

    @Singleton
    @Provides
    @Named(Constants.DI.EXOPLAYER_DIRECTORY)
    fun exoplayerFile(context: Context): File = exoplayerExternalDirectory(context)

    @Singleton
    @Provides
    fun cacheManager(cacheManagerImpl: CacheManagerImpl): CacheManager = cacheManagerImpl

    @Singleton
    @Provides
    fun httpDataSourceFactory(context: Context): DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent(Constants.getUserAgent(context))
        .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
        .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
        .setAllowCrossProtocolRedirects(true)

    @Singleton
    @Provides
    fun downloadEngine(exoplayerDownloadEngine: ExoplayerDownloadEngine): DownloadEngine = exoplayerDownloadEngine

    @Singleton
    @Provides
    fun downloadService(): Class<out DownloadService> = DefaultExoplayerDownloadService::class.java

    @Singleton
    @Provides
    fun downloadTracker(exoplayerDownloadTracker: ExoplayerDownloadTracker): DownloadTracker = exoplayerDownloadTracker

    @Singleton
    @Provides
    fun exoplayerEncryption(exoplayerEncryption: ExoplayerEncryptionImpl): ExoplayerEncryption = exoplayerEncryption

    @Singleton
    @Provides
    fun secureStorage(secureStorage: ArmadilloSecureStorage): SecureStorage = secureStorage

    @Singleton
    @Provides
    @Named(Constants.DI.STANDARD_STORAGE)
    fun standardStorage(context: Context): SharedPreferences =
        context.getSharedPreferences("armadillo.storage", Context.MODE_PRIVATE)

    @Singleton
    @Provides
    @Named(Constants.DI.DRM_DOWNLOAD_STORAGE)
    fun drmDownloadStorage(context: Context): SharedPreferences =
        context.getSharedPreferences("armadillo.download.drm", Context.MODE_PRIVATE)

    @Singleton
    @Provides
    @Named(Constants.DI.STANDARD_SECURE_STORAGE)
    fun standardSecureStorage(context: Context): SharedPreferences? {
        return try {
            val keys = MasterKeys.getOrCreate(
                KeyGenParameterSpec.Builder("armadilloStandard", PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(BLOCK_MODE_GCM)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                    .build()
            )
            EncryptedSharedPreferences.create(
                "armadillo.standard.secure",
                keys,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (ex: Exception) {
            null
        }
    }

    @Singleton
    @Provides
    @Named(Constants.DI.DRM_SECURE_STORAGE)
    fun drmSecureStorage(context: Context): SharedPreferences? {
        return try {
            val keys = MasterKeys.getOrCreate(
                KeyGenParameterSpec.Builder("armadillo", PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(BLOCK_MODE_GCM)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                    .build()
            )
            EncryptedSharedPreferences.create(
                "armadillo.download.secure",
                keys,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        catch (ex: Exception) {
            null
        }
    }

    @Singleton
    @Provides
    fun downloadManagerFactory(downloadManagerFactory: ArmadilloDownloadManagerFactory): DownloadManagerFactory = downloadManagerFactory

    @Singleton
    @Provides
    fun downloadManager(context: Context,
                        databaseProvider: ArmadilloDatabaseProvider,
                        downloadManagerFactory: DownloadManagerFactory):
        DownloadManager = downloadManagerFactory.build(context, databaseProvider.database)

    @Singleton
    @Provides
    fun downloadFactory(headerAwareDownloaderFactory: HeaderAwareDownloaderFactory): DownloaderFactory = headerAwareDownloaderFactory

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    private annotation class PrivateModule
    // https://stackoverflow.com/questions/43272652/dagger-2-avoid-exporting-private-dependencies
}