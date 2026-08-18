package ai.helply.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.helply.app.core.theme.HelplyTheme
import ai.helply.app.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelplyTheme {
                HelplyAppNavigation()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "HOME")
    object Academics : Screen("academics", "ACADEMICS")
    object Memory : Screen("memory", "MEMORY")
    object Placements : Screen("placements", "PLACEMENTS")
    object Portfolio : Screen("portfolio", "PORTFOLIO")
    object Settings : Screen("settings", "SETTINGS")
    object DemoMode : Screen("demo", "DEMO MODE")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelplyAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val screens = listOf(
        Screen.Home,
        Screen.Academics,
        Screen.Memory,
        Screen.Placements,
        Screen.Portfolio,
        Screen.Settings,
        Screen.DemoMode
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            }
                        },
                        label = { Text(screen.title) },
                        icon = {}
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
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Academics.route) { AcademicsScreen() }
            composable(Screen.Memory.route) { MemoryScreen() }
            composable(Screen.Placements.route) { PlacementScreen() }
            composable(Screen.Portfolio.route) { PortfolioScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.DemoMode.route) { DemoModeScreen() }
        }
    }
}
