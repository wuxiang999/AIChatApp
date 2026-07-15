package com.aichat.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aichat.app.ui.agent.AgentScreen
import com.aichat.app.ui.agents.AgentsScreen
import com.aichat.app.ui.chat.ChatCallbacks
import com.aichat.app.ui.chat.ChatScreen
import com.aichat.app.ui.chat.ChatState
import com.aichat.app.ui.chat.ChatViewModel
import com.aichat.app.ui.conversations.ConversationsScreen
import com.aichat.app.ui.conversations.ConversationsViewModel
import com.aichat.app.ui.mcp.McpScreen
import com.aichat.app.ui.memory.MemoryScreen
import com.aichat.app.ui.settings.AboutScreen
import com.aichat.app.ui.settings.AgentSettingsScreen
import com.aichat.app.ui.settings.EndpointSettingsScreen
import com.aichat.app.ui.settings.GitSettingsScreen
import com.aichat.app.ui.settings.SettingsScreen
import com.aichat.app.ui.settings.WorkspaceSettingsScreen
import com.aichat.app.ui.skills.SkillsScreen
import com.aichat.app.ui.imagegen.ImageGenScreen
import com.aichat.app.ui.terminal.TerminalScreen
import com.aichat.app.ui.workflow.WorkflowScreen
import com.aichat.app.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object NewChat : Screen("new_chat")
    object Settings : Screen("settings")
    object Agents : Screen("agents")
    object Terminal : Screen("terminal")
    object Skills : Screen("skills")
    object Mcp : Screen("mcp")
    object Memory : Screen("memory")
    object ImageGen : Screen("image_gen")
    object Agent : Screen("agent")
    object SettingsWorkspace : Screen("settings/workspace")
    object SettingsEndpoints : Screen("settings/endpoints")
    object SettingsAgent : Screen("settings/agent")
    object SettingsGit : Screen("settings/git")
    object SettingsAbout : Screen("settings/about")
    object Workflow : Screen("workflow")
}

@Composable
fun CodeVibeNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onRouteChange: (String?) -> Unit,
    onOpenDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ChatList.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
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
            val currentAgentName by viewModel.currentAgentName.collectAsStateWithLifecycle()

            val state = ChatState(
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
                currentAgentName = currentAgentName
            )

            val callbacks = remember {
                ChatCallbacks(
                    onSendMessage = { message, images -> viewModel.sendMessage(message, images) },
                    onStopGeneration = { viewModel.stopGeneration() },
                    onClearConversation = { viewModel.clearConversation() },
                    onModelChange = { model -> viewModel.setModel(model) },
                    onEndpointChange = { endpointId -> viewModel.selectEndpoint(endpointId) },
                    onNewConversation = { navController.navigate(Screen.NewChat.route) },
                    onGenerateImage = { prompt -> viewModel.generateImage(prompt) },
                    onEditImage = { imageUri, prompt -> viewModel.editImage(imageUri, prompt) },
                    onImageCountChange = { count -> viewModel.setImageCount(count) },
                    onImageSizeChange = { size -> viewModel.setImageSize(size) },
                    onImageModelChange = { model -> viewModel.setImageModel(model) },
                    onImageEditModeChange = { isEdit -> viewModel.setImageEditMode(isEdit) },
                    onRevokeMessage = { index -> viewModel.revokeMessage(index) },
                    onRefreshModels = { viewModel.refreshModels() },
                    onRefreshAgent = { viewModel.refreshAgent() }
                )
            }

            ChatScreen(
                state = state,
                callbacks = callbacks,
                snackbarHostState = snackbarHostState
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
            SettingsScreen(
                onNavigateToWorkspace = { navController.navigate(Screen.SettingsWorkspace.route) },
                onNavigateToEndpoints = { navController.navigate(Screen.SettingsEndpoints.route) },
                onNavigateToAgent = { navController.navigate(Screen.SettingsAgent.route) },
                onNavigateToGit = { navController.navigate(Screen.SettingsGit.route) },
                onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) }
            )
        }

        composable(Screen.SettingsWorkspace.route) {
            onRouteChange(Screen.SettingsWorkspace.route)
            WorkspaceSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SettingsEndpoints.route) {
            onRouteChange(Screen.SettingsEndpoints.route)
            EndpointSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SettingsAgent.route) {
            onRouteChange(Screen.SettingsAgent.route)
            AgentSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SettingsGit.route) {
            onRouteChange(Screen.SettingsGit.route)
            GitSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SettingsAbout.route) {
            onRouteChange(Screen.SettingsAbout.route)
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Agents.route) {
            onRouteChange(Screen.Agents.route)
            AgentsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Terminal.route) {
            onRouteChange(Screen.Terminal.route)
            TerminalScreen()
        }

        composable(Screen.Skills.route) {
            onRouteChange(Screen.Skills.route)
            SkillsScreen()
        }

        composable(Screen.Mcp.route) {
            onRouteChange(Screen.Mcp.route)
            McpScreen()
        }

        composable(Screen.ImageGen.route) {
            onRouteChange(Screen.ImageGen.route)
            ImageGenScreen()
        }

        composable(Screen.Memory.route) {
            onRouteChange(Screen.Memory.route)
            MemoryScreen()
        }

        composable(Screen.Agent.route) {
            onRouteChange(Screen.Agent.route)
            AgentScreen()
        }

        composable(Screen.Workflow.route) {
            onRouteChange(Screen.Workflow.route)
            val vm: WorkflowViewModel = hiltViewModel()
            WorkflowScreen(
                workflowManager = vm.workflowManager,
                onRunWorkflow = { workflow -> vm.runWorkflow(workflow) },
                onEditWorkflow = { workflow ->
                    navController.navigate("workflow_editor/${workflow.id}")
                }
            )
        }
    }
}
