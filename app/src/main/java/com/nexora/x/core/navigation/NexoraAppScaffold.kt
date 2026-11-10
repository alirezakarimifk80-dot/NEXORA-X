package com.nexora.x.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexora.x.core.ui.components.PremiumBottomNavigation
import com.nexora.x.feature.home.HomeScreen
import com.nexora.x.feature.profile.AddProfileScreen
import com.nexora.x.feature.servers.ServersScreen
import com.nexora.x.feature.settings.SettingsScreen
import com.nexora.x.feature.subscriptions.SubscriptionsScreen
import com.nexora.x.feature.tools.ToolsScreen

object NexoraRoutes {
    const val ADD_PROFILE = "profile_editor"
    const val EDIT_PROFILE_WITH_ARG = "profile_editor?profileId={profileId}"
    fun editProfileRoute(profileId: String) = "profile_editor?profileId=$profileId"
}

/**
 * Root application scaffold hosting the 5-destination bottom navigation graph plus the
 * Phase 2 Add/Edit Profile destination.
 */
@Composable
fun NexoraAppScaffold(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val destinations = NexoraDestination.entries
    val currentDestination = destinations.find { it.route == currentRoute } ?: NexoraDestination.SERVERS.takeIf {
        currentRoute?.startsWith(NexoraRoutes.ADD_PROFILE) == true
    } ?: NexoraDestination.HOME

    val navigateToDestination: (NexoraDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PremiumBottomNavigation(
                destinations = destinations,
                currentDestination = currentDestination,
                onNavigate = navigateToDestination
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NexoraDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NexoraDestination.HOME.route) {
                HomeScreen(
                    onNavigateToServers = { navigateToDestination(NexoraDestination.SERVERS) },
                    onNavigateToAddProfile = { navController.navigate(NexoraRoutes.ADD_PROFILE) },
                    onNavigateToSubscriptions = { navigateToDestination(NexoraDestination.SUBSCRIPTIONS) },
                    onNavigateToTools = { navigateToDestination(NexoraDestination.TOOLS) }
                )
            }
            composable(NexoraDestination.SERVERS.route) {
                ServersScreen(
                    onNavigateToAddProfile = { navController.navigate(NexoraRoutes.ADD_PROFILE) },
                    onNavigateToEditProfile = { id ->
                        navController.navigate(NexoraRoutes.editProfileRoute(id))
                    }
                )
            }
            composable(
                route = NexoraRoutes.EDIT_PROFILE_WITH_ARG,
                arguments = listOf(
                    navArgument("profileId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString("profileId")
                AddProfileScreen(
                    editingProfileId = profileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NexoraDestination.SUBSCRIPTIONS.route) {
                SubscriptionsScreen()
            }
            composable(NexoraDestination.TOOLS.route) {
                ToolsScreen()
            }
            composable(NexoraDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}
