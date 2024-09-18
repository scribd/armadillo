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
    fun saveDrmDownload(context: Context, id: String, drmDownload: DrmDownload)
    fun getDrmDownload(context: Context, id: String, drmType: DrmType): DrmDownload?
    fun getAllDrmDownloads(context: Context): Map<String, DrmDownload>
    fun removeDrmDownload(context: Context, id: String, drmType: DrmType)
    fun removeDrmDownload(context: Context, key: String)
}

@Singleton
internal class ArmadilloSecureStorage @Inject constructor(
    @Named(Constants.DI.STANDARD_STORAGE) private val legacyStandardStorage: SharedPreferences,
    @Named(Constants.DI.STANDARD_SECURE_STORAGE) private val secureStandardStorage: SharedPreferences,
    @Named(Constants.DI.DRM_DOWNLOAD_STORAGE) private val legacyDrmStorage: SharedPreferences,
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
        return if (secureStandardStorage.contains(DOWNLOAD_KEY)) {
            val storedKey = secureDrmStorage.getString(DOWNLOAD_KEY, DEFAULT) ?: DEFAULT
            if (storedKey == DEFAULT) {
                Log.e(TAG, "Storage Is Out of Alignment")
            }
            storedKey.toSecretByteArray
        } else if(legacyStandardStorage.contains(DOWNLOAD_KEY)) {
            //migrate to secured version
            val storedKey = legacyStandardStorage.getString(DOWNLOAD_KEY, DEFAULT) ?: DEFAULT
            if (storedKey == DEFAULT) {
                Log.e(TAG, "Storage Is Out of Alignment")
            }
            secureStandardStorage.edit().putString(DOWNLOAD_KEY, storedKey).apply()
            legacyStandardStorage.edit().remove(DOWNLOAD_KEY).apply()
            storedKey.toSecretByteArray
        } else {
            //no key exists anywhere yet
            createRandomString().also {
                secureStandardStorage.edit().putString(DOWNLOAD_KEY, it).apply()
            }.toSecretByteArray
        }
    }

    private fun createRandomString(): String {
        return (1..STRING_LENGTH)
            .map { ALLOWED_CHARS.random() }
            .joinToString("")
    }

    override fun saveDrmDownload(context: Context, id: String, drmDownload: DrmDownload) {
        val alias = getDrmDownloadAlias(id, drmDownload.drmType)
        val value = Base64.encodeToString(Json.encodeToString(drmDownload).toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        secureDrmStorage.edit().putString(alias, value).apply()
    }

    override fun getDrmDownload(context: Context, id: String, drmType: DrmType): DrmDownload? {
        val alias = getDrmDownloadAlias(id, drmType)
        var download = secureDrmStorage.getString(alias, null)?.decodeToDrmDownload()
        if (download == null && legacyDrmStorage.contains(alias)) {
            //migrate old storage to secure storage
            val downloadValue = legacyDrmStorage.getString(alias, null)
            download = downloadValue?.decodeToDrmDownload()
            secureDrmStorage.edit().putString(alias, downloadValue).apply()
            legacyDrmStorage.edit().remove(alias).apply()
        }
        return download
    }

    override fun getAllDrmDownloads(context: Context): Map<String, DrmDownload> {
        val drmDownloads = secureDrmStorage.all.keys.mapNotNull { alias ->
            secureDrmStorage.getString(alias, null)?.let { drmResult ->
                alias to drmResult.decodeToDrmDownload()
            }
        }.toMap()
        val legacyDownloads = legacyDrmStorage.all.keys.mapNotNull { alias ->
            legacyDrmStorage.getString(alias, null)?.let { drmResult ->
                alias to drmResult.decodeToDrmDownload()
            }
        }.toMap()

        return legacyDownloads.plus(drmDownloads)
    }

    override fun removeDrmDownload(context: Context, id: String, drmType: DrmType) {
        val alias = getDrmDownloadAlias(id, drmType)
        legacyDrmStorage.edit().remove(alias).apply()
        secureDrmStorage.edit().remove(alias).apply()
    }

    override fun removeDrmDownload(context: Context, key: String) {
        legacyDrmStorage.edit().remove(key).apply()
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

    private fun getDrmDownloadAlias(id: String, drmType: DrmType) =
        Base64.encodeToString(id.toSecretByteArray + drmType.name.toSecretByteArray, Base64.NO_WRAP)

    private fun String.decodeToDrmDownload(): DrmDownload =
        Base64.decode(this, Base64.NO_WRAP).let { resultByteArray ->
            Json.decodeFromString(String(resultByteArray, StandardCharsets.UTF_8))
        }
}