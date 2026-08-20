package ai.helply.app.data.remote

/**
 * Lightweight representation of a raw email fetched from any source
 * (Gmail API, IMAP, etc.) before AI classification.
 *
 * Consumed by [ai.helply.app.domain.EmailScannerEngine].
 */
data class RawEmail(
    /** Unique message ID — Gmail message ID hash or IMAP UID */
    val imapUid: Long = 0L,
    /** Sender email address (e.g. "office@college.edu") */
    val sender: String,
    /** Sender display name (e.g. "Examinations Office") */
    val senderName: String = sender.substringBefore('@'),
    /** Email subject line */
    val subject: String,
    /** Plain-text body (truncated to 3000 chars for AI processing) */
    val body: String,
    /** Epoch millis when the email was received */
    val receivedAt: Long = System.currentTimeMillis()
)
