package com.aichat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.aichat.app.ui.navigation.AIChatNavHost
import com.aichat.app.ui.navigation.Screen
import kotlinx.coroutines.launch

private data class DrawerEntry(
    val route: String,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf<String?>(Screen.ChatList.route) }

    val drawerItems = remember {
        listOf(
            DrawerEntry(Screen.ChatList.route, "对话列表", "查看与管理历史会话", Icons.Filled.Chat),
            DrawerEntry(Screen.Agents.route, "智能体", "选择预设角色或自定义", Icons.Default.Person),
            DrawerEntry(Screen.Settings.route, "设置", "API 端点、模型与偏好", Icons.Default.Settings)
        )
    }

    fun go(route: String) {
        scope.launch { drawerState.close() }
        if (route == currentRoute) return
        navController.navigate(route) {
            if (route == Screen.ChatList.route) {
                popUpTo(Screen.ChatList.route) { inclusive = true }
            } else {
                popUpTo(Screen.ChatList.route) { inclusive = false }
                launchSingleTop = true
            }
        }
        currentRoute = route
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerContentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    DrawerHeader(onClose = { scope.launch { drawerState.close() } })

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    drawerItems.forEach { entry ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(
                                        text = entry.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = entry.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = currentRoute == entry.route,
                            onClick = { go(entry.route) },
                            icon = {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "月下AI · 本地客户端",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Settings.route -> "设置"
                                    Screen.Agents.route -> "智能体"
                                    else -> "月下AI"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "菜单",
                                    modifier = Modifier.size(26.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (currentRoute == Screen.ChatList.route || currentRoute == Screen.Chat.route) {
                                IconButton(onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.NewChat.route)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新建对话",
                                        modifier = Modifier.size(26.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
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

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "月",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "月下AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "本地AI聊天助手",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭侧边栏",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
