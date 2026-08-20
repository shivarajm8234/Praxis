package ai.helply.app.data.remote

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Gmail OAuth2 authorization flow using Google Play Services Native Sign-In
 * and [GoogleAuthUtil] for secure on-device access token generation.
 */
@Singleton
class GmailOAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: GmailTokenStore
) {
    companion object {
        private const val TAG = "GmailOAuthManager"
        private const val SCOPE_GMAIL_FULL = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
        private const val SCOPE_GMAIL_RAW = "https://www.googleapis.com/auth/gmail.readonly"
    }

    /** Builds the Google Sign-In intent requesting Gmail read-only scope. */
    fun getAuthIntent(clientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(SCOPE_GMAIL_RAW))
            .requestEmail()
            .requestProfile()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    /**
     * Handles the Intent returned by Google Sign-In, retrieves the OAuth access token
     * directly via Google Play Services, and saves it in [GmailTokenStore].
     */
    suspend fun handleAuthResponse(
        intent: Intent,
        clientId: String
    ): GmailAuthResult = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)

            val email = account.email ?: "unknown@gmail.com"
            val displayName = account.displayName ?: email.substringBefore('@')
            val googleAccount = account.account

            if (googleAccount == null) {
                Log.e(TAG, "GoogleAccount object was null for $email")
                return@withContext GmailAuthResult.Error("Could not retrieve account details.")
            }

            // Fetch access token directly using Google Play Services
            val accessToken = GoogleAuthUtil.getToken(context, googleAccount, SCOPE_GMAIL_FULL)

            if (!accessToken.isNullOrBlank()) {
                tokenStore.saveTokens(
                    accessToken = accessToken,
                    refreshToken = "play_services_managed", // Managed natively by Google Play Services
                    expiryMs = System.currentTimeMillis() + (3600 * 1000L),
                    email = email,
                    displayName = displayName
                )
                Log.i(TAG, "Gmail OAuth sign-in successful via GoogleAuthUtil: $email")
                GmailAuthResult.Success(email, displayName)
            } else {
                GmailAuthResult.Error("Failed to obtain access token from Google Play Services.")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed code=${e.statusCode}", e)
            GmailAuthResult.Error("Sign-in failed (Code ${e.statusCode}): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected auth error", e)
            GmailAuthResult.Error("Sign-in error: ${e.localizedMessage}")
        }
    }

    /**
     * Returns a valid access token. Silently fetches or refreshes via Google Play Services.
     */
    suspend fun getValidAccessToken(clientId: String): String? = withContext(Dispatchers.IO) {
        val email = tokenStore.getConnectedEmail() ?: return@withContext null
        try {
            val androidAccount = android.accounts.Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, androidAccount, SCOPE_GMAIL_FULL)
            if (!token.isNullOrBlank()) {
                tokenStore.updateAccessToken(token, System.currentTimeMillis() + (3600 * 1000L))
            }
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token via GoogleAuthUtil", e)
            tokenStore.getAccessToken() // fallback to cached token
        }
    }
}

/** Result of the OAuth authorization flow. */
sealed class GmailAuthResult {
    data class Success(val email: String, val displayName: String) : GmailAuthResult()
    data class Error(val message: String) : GmailAuthResult()
}
