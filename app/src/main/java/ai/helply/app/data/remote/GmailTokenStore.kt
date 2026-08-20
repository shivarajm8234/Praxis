package ai.helply.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists Gmail OAuth tokens securely using EncryptedSharedPreferences
 * backed by Android KeyStore AES-256-GCM.
 *
 * Stores:
 * - Access token (short-lived, ~1 hour)
 * - Refresh token (long-lived, used to get new access tokens silently)
 * - Token expiry timestamp
 * - Connected Gmail address and display name
 * - Last poll timestamp (for incremental fetch)
 */
@Singleton
class GmailTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "helply_gmail_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_ACCESS_TOKEN   = "gmail_access_token"
        private const val KEY_REFRESH_TOKEN  = "gmail_refresh_token"
        private const val KEY_EXPIRY_MS      = "gmail_token_expiry_ms"
        private const val KEY_EMAIL          = "gmail_email"
        private const val KEY_DISPLAY_NAME   = "gmail_display_name"
        private const val KEY_LAST_POLL_MS   = "gmail_last_poll_ms"
        private const val KEY_CONNECTED      = "gmail_connected"
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        expiryMs: Long,
        email: String,
        displayName: String?
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply { refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) } }
            .putLong(KEY_EXPIRY_MS, expiryMs)
            .putString(KEY_EMAIL, email)
            .putString(KEY_DISPLAY_NAME, displayName ?: email.substringBefore('@'))
            .putBoolean(KEY_CONNECTED, true)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getTokenExpiryMs(): Long = prefs.getLong(KEY_EXPIRY_MS, 0L)
    fun getConnectedEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)
    fun getLastPollMs(): Long = prefs.getLong(KEY_LAST_POLL_MS, System.currentTimeMillis() - 7 * 86400000L) // last 7 days on first run
    fun isConnected(): Boolean = prefs.getBoolean(KEY_CONNECTED, false) && getRefreshToken() != null

    fun updateAccessToken(accessToken: String, expiryMs: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRY_MS, expiryMs)
            .apply()
    }

    fun updateLastPollMs(ms: Long) {
        prefs.edit().putLong(KEY_LAST_POLL_MS, ms).apply()
    }

    fun isAccessTokenExpired(): Boolean {
        val expiry = getTokenExpiryMs()
        return expiry == 0L || System.currentTimeMillis() > (expiry - 5 * 60 * 1000L) // 5-min buffer
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRY_MS)
            .remove(KEY_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_LAST_POLL_MS)
            .putBoolean(KEY_CONNECTED, false)
            .apply()
    }
}
