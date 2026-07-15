package com.aichat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.aichat.app.ui.navigation.CodeVibeNavHost
import com.aichat.app.ui.navigation.Screen
import kotlinx.coroutines.launch

data class DrawerSection(
    val title: String,
    val items: List<DrawerEntry>
)

data class DrawerEntry(
    val route: String,
    val label: String,
    val description: String,
    val icon: ImageVector
)

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentRoute by remember { mutableStateOf<String?>(Screen.ChatList.route) }

    val drawerSections = remember {
        listOf(
            DrawerSection("AI", listOf(
                DrawerEntry(Screen.ChatList.route, "对话", "与 AI 助手交流", Icons.Filled.Chat),
                DrawerEntry(Screen.Agent.route, "智能代理", "自主编程代理 · 工具调用", Icons.Filled.Code)
            )),
            DrawerSection("工具", listOf(
                DrawerEntry(Screen.Terminal.route, "终端日志", "实时日志与连接观测", Icons.Filled.Terminal),
                DrawerEntry(Screen.Workflow.route, "工作流", "自动化任务流水线", Icons.Filled.AccountTree),
                DrawerEntry(Screen.Memory.route, "记忆", "长期记忆 · 知识管理", Icons.Filled.Psychology),
                DrawerEntry(Screen.ImageGen.route, "图片生成", "AI 文生图 / 图生图", Icons.Filled.Image)
            )),
            DrawerSection("系统", listOf(
                DrawerEntry(Screen.Mcp.route, "MCP", "外部工具与数据源", Icons.Filled.Hub),
                DrawerEntry(Screen.Settings.route, "设置", "API 端点 · 模型 · 偏好", Icons.Filled.Settings)
            ))
        )
    }

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Screen.ChatList.route, "对话", Icons.Filled.Chat),
            BottomNavItem(Screen.Agent.route, "代理", Icons.Filled.Code),
            BottomNavItem(Screen.Memory.route, "记忆", Icons.Filled.Psychology),
            BottomNavItem(Screen.Settings.route, "设置", Icons.Filled.Settings)
        )
    }

    val showBottomBar = currentRoute !in listOf(Screen.Chat.route, Screen.NewChat.route)

    fun go(route: String) {
        scope.launch { drawerState.close() }
        if (route == currentRoute) return
        navController.navigate(route) {
            popUpTo(Screen.ChatList.route) { inclusive = false }
            launchSingleTop = true
        }
        currentRoute = route
    }

    val title = when (currentRoute) {
        Screen.Settings.route -> "设置"
        Screen.Memory.route -> "记忆"
        Screen.ImageGen.route -> "图片生成"
        Screen.Agent.route -> "智能代理"
        Screen.Terminal.route -> "终端日志"
        Screen.Mcp.route -> "MCP"
        Screen.Chat.route -> "对话"
        else -> "LH AI"
    }

    val subtitle = when (currentRoute) {
        Screen.Settings.route -> "API 端点 · 模型 · 偏好"
        Screen.Memory.route -> "长期记忆 · 知识管理"
        Screen.ImageGen.route -> "AI 文生图 / 图生图"
        Screen.Agent.route -> "自主编程 · 工具调用 · 代码生成"
        Screen.Terminal.route -> "实时日志与连接观测"
        Screen.Mcp.route -> "外部工具与数据源"
        else -> null
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

                    drawerSections.forEach { section ->
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        section.items.forEach { entry ->
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
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
            Text(
                    text = "LH AI · v2.4.0",
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
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                subtitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { go(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    CodeVibeNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
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
                    text = "L",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LH AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "智能助手 · 对话编程",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭侧边栏",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
