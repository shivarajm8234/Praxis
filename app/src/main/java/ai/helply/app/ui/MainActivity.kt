package ai.helply.app.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import ai.helply.app.BuildConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.helply.app.core.theme.HelplyTheme
import ai.helply.app.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var gemmaEngine: ai.helply.app.ai.GemmaEngineManager

    private val inferenceReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val prompt = intent?.getStringExtra("prompt") ?: "Summarize key concepts of Computer Science & AI"
            val modelId = intent?.getStringExtra("model") ?: "qwen-05b-it"

            android.util.Log.d("AI_RESPONSE", "==================================================")
            android.util.Log.d("AI_RESPONSE", "ADB REQUEST RECEIVED | Model: $modelId | Prompt: \"$prompt\"")
            android.util.Log.d("AI_RESPONSE", "==================================================")

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                gemmaEngine.initializeModel(modelId) { }
                gemmaEngine.generateStreamingResponse(prompt = prompt, modelId = modelId).collect { chunk ->
                    android.util.Log.d("AI_RESPONSE", chunk)
                }
                android.util.Log.d("AI_RESPONSE", "==================================================")
                android.util.Log.d("AI_RESPONSE", "END OF AI INFERENCE RESPONSE")
                android.util.Log.d("AI_RESPONSE", "==================================================")
            }
        }
    }

    private val viewModel: HelplyViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[HelplyViewModel::class.java]
    }

    /** Launcher for Gmail OAuth browser flow. Result dispatched to ViewModel. */
    private val gmailAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            viewModel.handleGmailOAuthResult(data, BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
        } else {
            android.util.Log.w("GMAIL_OAUTH", "Gmail OAuth cancelled or failed (resultCode=${result.resultCode})")
        }
    }

    /** Called from EmailIntelligenceScreen to kick off Gmail sign-in. */
    fun launchGmailOAuth() {
        val intent = viewModel.getGmailAuthIntent(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
        gmailAuthLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthRedirect(intent)

        val filter = android.content.IntentFilter("ai.helply.app.TEST_INFERENCE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(inferenceReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(inferenceReceiver, filter)
        }

        setContent {
            HelplyTheme {
                HelplyAppNavigation(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        android.util.Log.d("HELPLY_OAUTH", "OAuth redirect received: $uri")
        val code = uri.getQueryParameter("code")
        android.util.Log.d("HELPLY_OAUTH", "Extracted code: $code")
        if (!code.isNullOrBlank()) {
            android.util.Log.d("HELPLY_OAUTH", "Calling handleOAuthCode with code: ${code.take(6)}...")
            viewModel.handleOAuthCode(code)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(inferenceReceiver)
        } catch (_: Exception) {}
    }
}


sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Chatbot : Screen("chatbot", "AI Chat", Icons.Default.Send)
    object Academics : Screen("academics", "Academics", Icons.Default.Edit)
    object NotepadMemory : Screen("memory", "Memory", Icons.Default.Star)
    object Placements : Screen("placements", "Placements", Icons.Default.Person)
    object EmailIntelligence : Screen("email_intelligence", "Emails", Icons.Default.Email)
    object Settings : Screen("settings", "AI Models", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Portfolio : Screen("portfolio", "Portfolio", Icons.Default.Share)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelplyAppNavigation(viewModel: HelplyViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // 5 Clean Footer Navigation Tabs

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Academics,
        Screen.NotepadMemory,
        Screen.Placements,
        Screen.EmailIntelligence,
        Screen.Settings
    )

    Scaffold(
        topBar = {
            // Top Bar with Profile Button & AI Chat Button visible on EVERY section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Helply OS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.clickable {
                                if (currentRoute != Screen.Chatbot.route) {
                                    navController.navigate(Screen.Chatbot.route)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🤖", fontSize = 13.sp)
                                Text(
                                    text = "AI Chat",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentRoute != Screen.Portfolio.route) {
                                    navController.navigate(Screen.Portfolio.route)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Portfolio Publisher",
                                tint = Color(0xFF4F46E5)
                            )
                        }

                        // Profile Icon Button on Top Header
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4F46E5),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    if (currentRoute != Screen.Profile.route) {
                                        navController.navigate(Screen.Profile.route)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SG",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController, viewModel) }
            composable(Screen.Chatbot.route) { ChatbotScreen(viewModel) }
            composable(Screen.Academics.route) { AcademicsScreen(viewModel) }
            composable(Screen.NotepadMemory.route) { MemoryScreen(viewModel) }
            composable(Screen.Placements.route) { PlacementScreen(viewModel) }
            composable(Screen.EmailIntelligence.route) { ai.helply.app.ui.screens.EmailIntelligenceScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
            composable(Screen.Portfolio.route) { PortfolioScreen(viewModel) }
        }
    }
}
