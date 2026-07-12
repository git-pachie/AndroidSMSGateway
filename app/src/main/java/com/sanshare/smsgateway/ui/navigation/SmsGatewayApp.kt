package com.sanshare.smsgateway.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sanshare.smsgateway.ui.dashboard.DashboardRoute
import com.sanshare.smsgateway.ui.logs.LogsRoute
import com.sanshare.smsgateway.ui.messages.MessagesRoute
import com.sanshare.smsgateway.ui.settings.SettingsRoute
import kotlinx.coroutines.launch

@Composable
fun SmsGatewayApp() {
    val navController = rememberNavController()
    val destinations = Destination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun copyToClipboard(label: String, value: String) {
        clipboardManager.setText(AnnotatedString(value))
        scope.launch { snackbarHostState.showSnackbar("$label copied") }
    }

    BoxWithConstraints {
        val useBottomBar = maxWidth < 720.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (useBottomBar) {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigate(destination.route) },
                                label = { Text(destination.label) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(modifier = Modifier.padding(padding)) {
                if (!useBottomBar) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigate(destination.route) },
                                label = { Text(destination.label) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                            )
                        }
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = Destination.Dashboard.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(Destination.Dashboard.route) {
                        DashboardRoute(
                            onOpenSettings = { navigate(Destination.Settings.route) },
                            onOpenLogs = { navigate(Destination.Logs.route) },
                            onOpenMessages = { navigate(Destination.Messages.route) },
                            onCopyText = ::copyToClipboard,
                            onShowSnackbar = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        )
                    }
                    composable(Destination.Messages.route) {
                        MessagesRoute(
                            onCopyText = ::copyToClipboard,
                            onShowSnackbar = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        )
                    }
                    composable(Destination.Logs.route) { LogsRoute() }
                    composable(Destination.Settings.route) {
                        SettingsRoute(
                            onCopyText = ::copyToClipboard,
                            onShowSnackbar = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        )
                    }
                }
            }
        }
    }
}
