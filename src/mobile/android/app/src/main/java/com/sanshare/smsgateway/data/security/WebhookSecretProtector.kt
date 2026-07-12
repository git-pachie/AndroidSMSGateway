package com.sanshare.smsgateway.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookSecretProtector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun protect(secret: String): String {
        val key = "webhook_secret_${System.currentTimeMillis()}"
        encryptedPrefs().edit().putString(key, secret).apply()
        return key
    }

    fun reveal(reference: String?): String? {
        if (reference.isNullOrBlank()) return null
        return runCatching { encryptedPrefs().getString(reference, null) }.getOrNull()
    }

    fun clear(reference: String?) {
        if (reference.isNullOrBlank()) return
        runCatching { encryptedPrefs().edit().remove(reference).apply() }
    }

    private fun encryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        "secure_gateway_secrets",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
