package ai.helply.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.core.theme.HelplyTheme

class AppLockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "Restricted App"
        val packageName = intent.getStringExtra("BLOCKED_PACKAGE_NAME") ?: ""
        val lockReason = intent.getStringExtra("LOCK_REASON") ?: "This application is currently locked to preserve study focus."
        val iconEmoji = intent.getStringExtra("ICON_EMOJI") ?: "🔒"

        setContent {
            HelplyTheme {
                AppLockScreen(
                    appName = appName,
                    packageName = packageName,
                    lockReason = lockReason,
                    iconEmoji = iconEmoji,
                    onReturnHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onOpenHelply = {
                        val helplyIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(helplyIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AppLockScreen(
    appName: String,
    packageName: String,
    lockReason: String,
    iconEmoji: String,
    onReturnHome: () -> Unit,
    onOpenHelply: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Sleek dark focus background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF87171))
                    Text(
                        text = "ACCESS RESTRICTED BY HELPLY OS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Icon Emoji & Title
            Text(text = iconEmoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$appName is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Why is this app blocked? (Reasoning Card)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📋 Why is this app blocked?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    Text(
                        text = lockReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Strict Focus Mode Active: Unlocking or bypassing is disabled until exam period ends or manual lock is cleared in Helply OS.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Button(
                onClick = onOpenHelply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Helply Student OS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("Return to Device Home Screen")
            }
        }
    }
}
