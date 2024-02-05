package com.scribd.armadillo.encryption

import android.content.Context
import android.util.Base64
import android.util.Log
import com.scribd.armadillo.models.DrmDownload
import com.scribd.armadillo.models.DrmType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
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
internal class ArmadilloSecureStorage @Inject constructor() : SecureStorage {
    private companion object {
        const val DOWNLOAD_KEY = "download_key"
        const val STRING_LENGTH = 20
        const val ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        const val DEFAULT = "82YEDKqPBEqA2qAb4bUU"
        const val STANDARD_STORAGE_FILENAME = "armadillo.storage"
        const val DOWNLOAD_FILENAME = "armadillo.download"
        const val TAG = "SecureStorage"
    }

    override fun downloadSecretKey(context: Context): ByteArray {
        val sharedPreferences = context.getSharedPreferences(STANDARD_STORAGE_FILENAME, Context.MODE_PRIVATE)
        return if (sharedPreferences.contains(DOWNLOAD_KEY)) {
            val storedKey = sharedPreferences.getString(DOWNLOAD_KEY, DEFAULT)!!
            if (storedKey == DEFAULT) {
                Log.e(TAG, "Storage Is Out of Alignment")
            }
            storedKey.toSecretByteArray
        } else {
            createRandomString().also {
                sharedPreferences.edit().putString(DOWNLOAD_KEY, it).apply()
            }.toSecretByteArray
        }
    }

    private fun createRandomString(): String {
        return (1..STRING_LENGTH)
            .map { ALLOWED_CHARS.random() }
            .joinToString("")
    }

    override fun saveDrmDownload(context: Context, audioUrl: String, drmDownload: DrmDownload) {
        context.getSharedPreferences(DOWNLOAD_FILENAME, Context.MODE_PRIVATE).also { sharedPrefs ->
            val key = getDrmDownloadKey(audioUrl, drmDownload.drmType)
            val value = Base64.encodeToString(Json.encodeToString(drmDownload).toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            sharedPrefs.edit().putString(key, value).apply()
        }
    }

    override fun getDrmDownload(context: Context, audioUrl: String, drmType: DrmType): DrmDownload? =
        context.getSharedPreferences(DOWNLOAD_FILENAME, Context.MODE_PRIVATE).let { sharedPrefs ->
            sharedPrefs.getString(getDrmDownloadKey(audioUrl, drmType), null)?.decodeToDrmDownload()
        }

    override fun getAllDrmDownloads(context: Context): Map<String, DrmDownload> =
        context.getSharedPreferences(DOWNLOAD_FILENAME, Context.MODE_PRIVATE).let { sharedPrefs ->
            sharedPrefs.all.keys.mapNotNull { key ->
                sharedPrefs.getString(key, null)?.let { drmResult ->
                    key to drmResult.decodeToDrmDownload()
                }
            }.toMap()
        }

    override fun removeDrmDownload(context: Context, audioUrl: String, drmType: DrmType) {
        context.getSharedPreferences(DOWNLOAD_FILENAME, Context.MODE_PRIVATE).also { sharedPrefs ->
            sharedPrefs.edit().remove(getDrmDownloadKey(audioUrl, drmType)).apply()
        }
    }

    override fun removeDrmDownload(context: Context, key: String) {
        context.getSharedPreferences(DOWNLOAD_FILENAME, Context.MODE_PRIVATE).also { sharedPrefs ->
            sharedPrefs.edit().remove(key).apply()
        }
    }

    private val String.toSecretByteArray: ByteArray
        get() {
            val keyBytes = ByteArray(16)
            val md = MessageDigest.getInstance("SHA-256")
            md.update(this.toByteArray())
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.size)
            return keyBytes
        }

    private fun getDrmDownloadKey(audioUrl: String, drmType: DrmType) =
        Base64.encodeToString(audioUrl.toSecretByteArray + drmType.name.toSecretByteArray, Base64.NO_WRAP)

    private fun String.decodeToDrmDownload(): DrmDownload =
        Base64.decode(this, Base64.NO_WRAP).let { resultByteArray ->
            Json.decodeFromString(String(resultByteArray, StandardCharsets.UTF_8))
        }
}