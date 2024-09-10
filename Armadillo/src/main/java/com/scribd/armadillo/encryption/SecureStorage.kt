package com.scribd.armadillo.encryption

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.scribd.armadillo.Constants
import com.scribd.armadillo.models.DrmDownload
import com.scribd.armadillo.models.DrmType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal interface SecureStorage {
    fun downloadSecretKey(context: Context): ByteArray
    fun saveDrmDownload(context: Context, audioUrl: String, drmDownload: DrmDownload)
    fun getDrmDownload(context: Context, audioUrl: String, drmType: DrmType): DrmDownload?
    fun getAllDrmDownloads(context: Context): Map<String, DrmDownload>
    fun removeDrmDownload(context: Context, audioUrl: String, drmType: DrmType)
    fun removeDrmDownload(context: Context, key: String)
}

@Singleton
internal class ArmadilloSecureStorage @Inject constructor(
    @Named(Constants.DI.STANDARD_STORAGE) private val standardStorage: SharedPreferences,
    @Named(Constants.DI.DRM_DOWNLOAD_STORAGE) private val drmDownloadStorage: SharedPreferences,
    @Named(Constants.DI.DRM_SECURE_STORAGE) private val secureDrmStorage: SharedPreferences
) : SecureStorage {
    companion object {
        const val DOWNLOAD_KEY = "download_key"
        const val STRING_LENGTH = 20
        const val ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        const val DEFAULT = "82YEDKqPBEqA2qAb4bUU"
        const val TAG = "SecureStorage"
    }

    override fun downloadSecretKey(context: Context): ByteArray {
        return if (standardStorage.contains(DOWNLOAD_KEY)) {
            val storedKey = standardStorage.getString(DOWNLOAD_KEY, DEFAULT)!!
            if (storedKey == DEFAULT) {
                Log.e(TAG, "Storage Is Out of Alignment")
            }
            storedKey.toSecretByteArray
        } else {
            createRandomString().also {
                standardStorage.edit().putString(DOWNLOAD_KEY, it).apply()
            }.toSecretByteArray
        }
    }

    private fun createRandomString(): String {
        return (1..STRING_LENGTH)
            .map { ALLOWED_CHARS.random() }
            .joinToString("")
    }

    override fun saveDrmDownload(context: Context, audioUrl: String, drmDownload: DrmDownload) {
        val alias = getDrmDownloadAlias(audioUrl, drmDownload.drmType)
        val value = Base64.encodeToString(Json.encodeToString(drmDownload).toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        secureDrmStorage.edit().putString(alias, value).apply()
    }

    override fun getDrmDownload(context: Context, audioUrl: String, drmType: DrmType): DrmDownload? {
        val alias = getDrmDownloadAlias(audioUrl, drmType)
        var download = secureDrmStorage.getString(alias, null)?.decodeToDrmDownload()
        if (download == null && drmDownloadStorage.contains(alias)) {
            //migrate old storage to secure storage
            val downloadValue = drmDownloadStorage.getString(alias, null)
            download = downloadValue?.decodeToDrmDownload()
            secureDrmStorage.edit().putString(alias, downloadValue).apply()
            drmDownloadStorage.edit().remove(alias).apply()
        }
        return download
    }

    override fun getAllDrmDownloads(context: Context): Map<String, DrmDownload> =
        drmDownloadStorage.all.keys.mapNotNull { key ->
            drmDownloadStorage.getString(key, null)?.let { drmResult ->
                key to drmResult.decodeToDrmDownload()
            }
        }.toMap()

    override fun removeDrmDownload(context: Context, audioUrl: String, drmType: DrmType) {
        val alias = getDrmDownloadAlias(audioUrl, drmType)
        drmDownloadStorage.edit().remove(alias).apply()
        secureDrmStorage.edit().remove(alias).apply()
    }

    override fun removeDrmDownload(context: Context, key: String) {
        drmDownloadStorage.edit().remove(key).apply()
        secureDrmStorage.edit().remove(key).apply()
    }

    private val String.toSecretByteArray: ByteArray
        get() {
            val keyBytes = ByteArray(16)
            val md = MessageDigest.getInstance("SHA-256")
            md.update(this.toByteArray())
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.size)
            return keyBytes
        }

    private fun getDrmDownloadAlias(audioUrl: String, drmType: DrmType) =
        Base64.encodeToString(audioUrl.toSecretByteArray + drmType.name.toSecretByteArray, Base64.NO_WRAP)

    private fun String.decodeToDrmDownload(): DrmDownload =
        Base64.decode(this, Base64.NO_WRAP).let { resultByteArray ->
            Json.decodeFromString(String(resultByteArray, StandardCharsets.UTF_8))
        }
}