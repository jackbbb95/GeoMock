package me.bogle.geomock

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.bogle.geomock.ui.home.Home
import me.bogle.geomock.ui.home.HomeScreen
import me.bogle.geomock.ui.startup.Startup
import me.bogle.geomock.ui.startup.StartupScreen

@Composable
fun MainNavigationGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Startup) {
        composable<Startup> {
            StartupScreen(onNavigateToHome = { navController.navigate(route = Home) })
        }

        composable<Home> {
            HomeScreen()
        }
    }
}