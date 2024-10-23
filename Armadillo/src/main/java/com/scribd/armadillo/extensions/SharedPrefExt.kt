package com.scribd.armadillo.extensions

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.scribd.armadillo.Constants.DI.STANDARD_STORE_FILENAME
import com.scribd.armadillo.Constants.Keys.ANDROID_KEYSTORE_NAME
import java.io.File
import java.security.KeyStore

fun SharedPreferences.deleteSharedPreference(context: Context, filename: String, keystoreAlias: String) {
    val tag = "DeletingSharedPrefs"
    try {
        val sharedPrefsFile = File(
            (context.filesDir.getParent()?.plus("/shared_prefs/")) + filename + ".xml"
        )

        edit().clear().commit()

        if (sharedPrefsFile.exists()) {
            val deleted = sharedPrefsFile.delete()
            Log.d(tag, "resetStorage() Shared prefs file deleted: $deleted; path: ${sharedPrefsFile.absolutePath}")
        } else {
            Log.d(tag,"resetStorage() Shared prefs file non-existent; path: ${sharedPrefsFile.absolutePath}")
        }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_NAME)
        keyStore.load(null)
        keyStore.deleteEntry(keystoreAlias)
    } catch (e: Exception) {
        Log.e(tag, "Error occurred while trying to reset shared prefs", e)
    }
}

fun createEncryptedSharedPrefKeyStoreWithRetry(context: Context, fileName: String, keystoreAlias: String): SharedPreferences? {
    val firstAttempt = createEncryptedSharedPrefsKeyStore(context = context, fileName = fileName, keystoreAlias = keystoreAlias)
    return if(firstAttempt != null) {
        firstAttempt
    } else {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE).deleteSharedPreference(
            context = context,
            filename = fileName,
            keystoreAlias = keystoreAlias
        )
        createEncryptedSharedPrefsKeyStore(context = context, fileName = fileName, keystoreAlias = keystoreAlias)
    }
}

fun createEncryptedSharedPrefsKeyStore(context: Context, fileName: String, keystoreAlias: String)
    : SharedPreferences? {
    val keySpec = KeyGenParameterSpec.Builder(keystoreAlias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
        .setKeySize(256)
        .setBlockModes(BLOCK_MODE_GCM)
        .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
        .build()

    val keys = try {
        MasterKeys.getOrCreate(keySpec)
    } catch (ex: Exception) {
        //clear corrupted store, contents will be lost
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE).deleteSharedPreference(
            context = context,
            filename = fileName,
            keystoreAlias = keystoreAlias )
        MasterKeys.getOrCreate(keySpec)
    }
    return try {
        EncryptedSharedPreferences.create(
            fileName,
            keys,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch(ex: Exception) {
        null
    }
}