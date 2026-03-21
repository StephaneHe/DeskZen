package com.deskzen.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deskzen.ui.apps.AppsListScreen
import com.deskzen.ui.homescreen.HomeScreenView
import com.deskzen.ui.suggestions.SuggestionsScreen
import kotlinx.serialization.Serializable

@Serializable data object AppsRoute
@Serializable data object ScreenRoute
@Serializable data object SuggestionsRoute

data class TopLevelRoute(
    val label: String,
    val route: Any,
    val icon: ImageVector
)

@Composable
fun DeskZenNavHost() {
    val navController = rememberNavController()

    val topLevelRoutes = listOf(
        TopLevelRoute(
            label = "Apps",
            route = AppsRoute,
            icon = Icons.Outlined.Apps
        ),
        TopLevelRoute(
            label = "Écran",
            route = ScreenRoute,
            icon = Icons.Outlined.Smartphone
        ),
        TopLevelRoute(
            label = "Suggestions",
            route = SuggestionsRoute,
            icon = Icons.Outlined.AutoAwesome
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                topLevelRoutes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = topLevelRoute.icon,
                                contentDescription = topLevelRoute.label
                            )
                        },
                        label = { Text(topLevelRoute.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(topLevelRoute.route::class)
                        } == true,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppsRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<AppsRoute> {
                AppsListScreen()
            }
            composable<ScreenRoute> {
                HomeScreenView()
            }
            composable<SuggestionsRoute> {
                SuggestionsScreen()
            }
        }
    }
}
