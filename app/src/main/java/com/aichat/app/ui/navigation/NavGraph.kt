package com.aichat.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aichat.app.ui.agents.AgentsScreen
import com.aichat.app.ui.chat.ChatScreen
import com.aichat.app.ui.chat.ChatViewModel
import com.aichat.app.ui.conversations.ConversationsScreen
import com.aichat.app.ui.conversations.ConversationsViewModel
import com.aichat.app.ui.settings.SettingsScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object NewChat : Screen("new_chat")
    object Settings : Screen("settings")
    object Agents : Screen("agents")
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
            val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
            val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
            val endpoints by viewModel.endpoints.collectAsStateWithLifecycle()
            val currentEndpointId by viewModel.currentEndpointId.collectAsStateWithLifecycle()
            val imageCount by viewModel.imageCount.collectAsStateWithLifecycle()
            val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()
            val imageModel by viewModel.imageModel.collectAsStateWithLifecycle()
            val isImageEditMode by viewModel.isImageEditMode.collectAsStateWithLifecycle()

            ChatScreen(
                messages = messages,
                isLoading = isLoading,
                error = error,
                currentModel = currentModel,
                availableModels = availableModels,
                endpoints = endpoints,
                currentEndpointId = currentEndpointId,
                imageCount = imageCount,
                imageSize = imageSize,
                imageModel = imageModel,
                isImageEditMode = isImageEditMode,
                onSendMessage = { message, images ->
                    viewModel.sendMessage(message, images)
                },
                onStopGeneration = {
                    viewModel.stopGeneration()
                },
                onClearConversation = {
                    viewModel.clearConversation()
                },
                onModelChange = { model ->
                    viewModel.setModel(model)
                },
                onEndpointChange = { endpointId ->
                    viewModel.selectEndpoint(endpointId)
                },
                onNewConversation = {
                    navController.navigate(Screen.NewChat.route)
                },
                onGenerateImage = { prompt ->
                    viewModel.generateImage(prompt)
                },
                onEditImage = { imageUri, prompt ->
                    viewModel.editImage(imageUri, prompt)
                },
                onImageCountChange = { count ->
                    viewModel.setImageCount(count)
                },
                onImageSizeChange = { size ->
                    viewModel.setImageSize(size)
                },
                onImageModelChange = { model ->
                    viewModel.setImageModel(model)
                },
                onImageEditModeChange = { isEdit ->
                    viewModel.setImageEditMode(isEdit)
                },
                onRevokeMessage = { index ->
                    viewModel.revokeMessage(index)
                }
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

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        composable(Screen.Settings.route) {
            onRouteChange(Screen.Settings.route)
            SettingsScreen()
        }

        composable(Screen.Agents.route) {
            onRouteChange(Screen.Agents.route)
            AgentsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
