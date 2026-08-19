package ai.helply.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches emails from the Gmail REST API using a Bearer OAuth token.
 *
 * Replaces ImapEmailClient — produces identical [RawEmail] objects consumed
 * by [ai.helply.app.domain.EmailScannerEngine].
 *
 * API Reference: https://developers.google.com/gmail/api/reference/rest
 */
@Singleton
class GmailApiClient @Inject constructor(
    private val tokenStore: GmailTokenStore,
    private val oAuthManager: GmailOAuthManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "GmailApiClient"
        private const val GMAIL_BASE = "https://gmail.googleapis.com/gmail/v1/users/me"
        private const val MAX_RESULTS = 30
    }

    /**
     * Fetches up to [MAX_RESULTS] unread emails from INBOX since [sinceTimestamp].
     * Automatically refreshes the access token if expired.
     */
    suspend fun fetchNewEmails(sinceTimestamp: Long, clientId: String): List<RawEmail> =
        withContext(Dispatchers.IO) {
            try {
                val accessToken = oAuthManager.getValidAccessToken(clientId)
                    ?: return@withContext emptyList<RawEmail>()

                val sinceSeconds = sinceTimestamp / 1000
                val query = "is:unread in:inbox after:$sinceSeconds"
                val listUrl = "$GMAIL_BASE/messages?maxResults=$MAX_RESULTS&q=${urlEncode(query)}"

                val listResponse = get(listUrl, accessToken) ?: return@withContext emptyList<RawEmail>()
                val messageIds = parseMessageIds(listResponse)

                if (messageIds.isEmpty()) return@withContext emptyList<RawEmail>()

                val emails = messageIds.mapNotNull { msgId ->
                    fetchMessage(msgId, accessToken)
                }

                // Update last poll timestamp to now
                tokenStore.updateLastPollMs(System.currentTimeMillis())

                emails
            } catch (e: Exception) {
                Log.e(TAG, "fetchNewEmails failed: ${e.message}", e)
                emptyList()
            }
        }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private fun fetchMessage(messageId: String, accessToken: String): RawEmail? {
        return try {
            val url = "$GMAIL_BASE/messages/$messageId?format=full"
            val json = get(url, accessToken) ?: return null
            parseMessage(json)
        } catch (e: Exception) {
            Log.w(TAG, "fetchMessage $messageId failed: ${e.message}")
            null
        }
    }

    private fun parseMessage(json: String): RawEmail? {
        return try {
            val obj = JSONObject(json)
            val id = obj.getString("id")
            val internalDate = obj.optLong("internalDate", System.currentTimeMillis())

            val payload = obj.optJSONObject("payload") ?: return null
            val headers = payload.optJSONArray("headers") ?: return null

            var subject = "(No Subject)"
            var from = "unknown@gmail.com"
            var fromName = "Unknown"

            for (i in 0 until headers.length()) {
                val header = headers.getJSONObject(i)
                when (header.getString("name").lowercase()) {
                    "subject" -> subject = header.getString("value")
                    "from" -> {
                        val raw = header.getString("value")
                        // Parse "Display Name <email@gmail.com>"
                        val emailMatch = Regex("<([^>]+)>").find(raw)
                        from = emailMatch?.groupValues?.get(1) ?: raw.trim()
                        fromName = raw.substringBefore("<").trim().removeSurrounding("\"").ifBlank { from.substringBefore('@') }
                    }
                }
            }

            val body = extractBody(payload).take(3000)

            RawEmail(
                imapUid = id.hashCode().toLong(),
                sender = from,
                senderName = fromName,
                subject = subject,
                body = body,
                receivedAt = internalDate
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseMessage failed: ${e.message}")
            null
        }
    }

    private fun extractBody(payload: JSONObject): String {
        // Try direct body first (simple text/plain messages)
        val mimeType = payload.optString("mimeType", "")
        val bodyData = payload.optJSONObject("body")?.optString("data", "")

        if (!mimeType.isNullOrBlank() && mimeType.startsWith("text/plain") && !bodyData.isNullOrBlank()) {
            return decodeBase64Url(bodyData)
        }

        // Walk multipart parts
        val parts = payload.optJSONArray("parts") ?: return ""
        var plainText = ""
        var htmlText = ""

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val partMime = part.optString("mimeType", "")
            val partData = part.optJSONObject("body")?.optString("data", "") ?: ""

            when {
                partMime == "text/plain" && partData.isNotBlank() ->
                    plainText = decodeBase64Url(partData)
                partMime == "text/html" && partData.isNotBlank() ->
                    htmlText = stripHtml(decodeBase64Url(partData))
                partMime.startsWith("multipart/") -> {
                    // Recurse into nested multipart
                    val nested = extractBody(part)
                    if (nested.isNotBlank()) plainText = nested
                }
            }
        }

        return plainText.ifBlank { htmlText }
    }

    private fun parseMessageIds(json: String): List<String> {
        return try {
            val obj = JSONObject(json)
            val messages = obj.optJSONArray("messages") ?: return emptyList()
            (0 until messages.length()).map { messages.getJSONObject(it).getString("id") }
        } catch (_: Exception) { emptyList() }
    }

    private fun get(url: String, accessToken: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "HTTP ${response.code} for $url")
            return null
        }
        return response.body?.string()
    }

    private fun decodeBase64Url(data: String): String {
        val bytes = android.util.Base64.decode(
            data.replace('-', '+').replace('_', '/'),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        )
        return String(bytes, Charsets.UTF_8)
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun urlEncode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")
}
