package com.sanshare.smsgateway.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sanshare.smsgateway.ui.dashboard.DashboardRoute
import com.sanshare.smsgateway.ui.logs.LogsRoute
import com.sanshare.smsgateway.ui.messages.MessagesRoute
import com.sanshare.smsgateway.ui.settings.SettingsRoute

@Composable
fun SmsGatewayApp() {
    val navController = rememberNavController()
    val destinations = Destination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(destination.label) },
                        icon = { Text(destination.label.first().toString()) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Dashboard.route) {
                DashboardRoute(onOpenSettings = { navController.navigate(Destination.Settings.route) })
            }
            composable(Destination.Messages.route) { MessagesRoute() }
            composable(Destination.Logs.route) { LogsRoute() }
            composable(Destination.Settings.route) { SettingsRoute() }
        }
    }
}
