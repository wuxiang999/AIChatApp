package com.aichat.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aichat.app.ui.chat.ChatScreen
import com.aichat.app.ui.chat.ChatViewModel
import com.aichat.app.ui.conversations.ConversationsScreen
import com.aichat.app.ui.conversations.ConversationsViewModel
import com.aichat.app.ui.imagegen.ImageGenScreen
import com.aichat.app.ui.settings.SettingsScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object NewChat : Screen("new_chat")
    object ImageGen : Screen("image_gen")
    object Settings : Screen("settings")
}

@Composable
fun AIChatNavHost(
    navController: NavHostController,
    onRouteChange: (String?) -> Unit,
    onOpenDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ChatList.route
    ) {
        composable(Screen.ChatList.route) {
            onRouteChange(Screen.ChatList.route)
            val viewModel: ConversationsViewModel = hiltViewModel()
            val conversations by viewModel.conversations.collectAsStateWithLifecycle()

            ConversationsScreen(
                conversations = conversations,
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversation = {
                    navController.navigate(Screen.NewChat.route)
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onRenameConversation = { id, title ->
                    viewModel.updateConversationTitle(id, title)
                },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            onRouteChange(Screen.Chat.route)
            val viewModel: ChatViewModel = hiltViewModel(backStackEntry)
            val messages by viewModel.messages.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val error by viewModel.error.collectAsStateWithLifecycle()

            ChatScreen(
                messages = messages,
                isLoading = isLoading,
                error = error,
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                },
                onStopGeneration = {
                    viewModel.stopGeneration()
                },
                onClearConversation = {
                    viewModel.clearConversation()
                },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(Screen.NewChat.route) {
            val viewModel: ConversationsViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                val conversation = viewModel.createConversation()
                delay(100)
                navController.navigate(Screen.Chat.createRoute(conversation.id)) {
                    popUpTo(Screen.ChatList.route) { inclusive = false }
                }
            }

            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.foundation.layout.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }

        composable(Screen.ImageGen.route) {
            onRouteChange(Screen.ImageGen.route)
            ImageGenScreen()
        }

        composable(Screen.Settings.route) {
            onRouteChange(Screen.Settings.route)
            SettingsScreen()
        }
    }
}
