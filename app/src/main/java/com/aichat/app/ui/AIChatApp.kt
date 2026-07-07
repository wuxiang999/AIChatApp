package com.aichat.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.aichat.app.ui.navigation.AIChatNavHost
import com.aichat.app.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatApp() {
    val navController = rememberNavController()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf<String?>(Screen.ChatList.route) }

    Surface(color = MaterialTheme.colorScheme.background) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                            Text(
                                text = "AI 聊天",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            androidx.compose.foundation.layout.Divider()

                            NavigationDrawerItem(
                                label = { Text("对话列表") },
                                selected = currentRoute == Screen.ChatList.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.ChatList.route) {
                                        popUpTo(Screen.ChatList.route) { inclusive = true }
                                    }
                                    currentRoute = Screen.ChatList.route
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                label = { Text("图片生成") },
                                selected = currentRoute == Screen.ImageGen.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.ImageGen.route)
                                    currentRoute = Screen.ImageGen.route
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                label = { Text("设置") },
                                selected = currentRoute == Screen.Settings.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Settings.route)
                                    currentRoute = Screen.Settings.route
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when (currentRoute) {
                                    Screen.ChatList.route -> "对话"
                                    Screen.Chat.route -> "AI 聊天"
                                    Screen.ImageGen.route -> "图片生成"
                                    Screen.Settings.route -> "设置"
                                    else -> "AI 聊天"
                                }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "菜单"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (currentRoute == Screen.ChatList.route || currentRoute == Screen.Chat.route) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Screen.NewChat.route)
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "新建对话")
                        }
                    }
                }
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                    AIChatNavHost(
                        navController = navController,
                        onRouteChange = { route -> currentRoute = route },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}

private val Icons.Default.Menu get() = androidx.compose.material.icons.Icons.Filled.Menu
