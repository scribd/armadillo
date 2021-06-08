package com.scribd.armadillo.encryption

import android.content.Context
import android.util.Log
import java.security.MessageDigest

internal interface SecureStorage {
    fun downloadSecretKey(context: Context): ByteArray
}

internal class ArmadilloSecureStorage : SecureStorage {
    private companion object {
        const val DOWNLOAD_KEY = "download_key"
        const val STRING_LENGTH = 20
        const val ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        const val DEFAULT = "82YEDKqPBEqA2qAb4bUU"
        const val LOCATION = "armadillo.storage"
        const val TAG = "SecureStorage"
    }

    override fun downloadSecretKey(context: Context): ByteArray {
            val sharedPreferences = context.getSharedPreferences(LOCATION, Context.MODE_PRIVATE)
            return if (sharedPreferences.contains(DOWNLOAD_KEY)) {
                val storedKey = sharedPreferences.getString(DOWNLOAD_KEY, DEFAULT)!!
                if(storedKey == DEFAULT){
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

    private val String.toSecretByteArray: ByteArray
        get() {
            val keyBytes = ByteArray(16)
            val md = MessageDigest.getInstance("SHA-256")
            md.update(this.toByteArray())
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.size)
            return keyBytes
        }
}