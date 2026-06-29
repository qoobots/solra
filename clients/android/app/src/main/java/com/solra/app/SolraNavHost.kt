package com.solra.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/// Tab 导航框架（发现/创作/虚拟人/我的）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolraNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(
        TabItem("discover", "发现", Icons.Filled.Search),
        TabItem("create", "创作", Icons.Filled.Add),
        TabItem("avatar", "虚拟人", Icons.Filled.Person),
        TabItem("profile", "我的", Icons.Filled.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
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
            startDestination = "discover",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("discover") { PlaceholderScreen("空间发现") }
            composable("create") { PlaceholderScreen("空间创作") }
            composable("avatar") { PlaceholderScreen("虚拟人对话") }
            composable("profile") { PlaceholderScreen("个人中心") }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium
    )
}

data class TabItem(val route: String, val label: String, val icon: ImageVector)
