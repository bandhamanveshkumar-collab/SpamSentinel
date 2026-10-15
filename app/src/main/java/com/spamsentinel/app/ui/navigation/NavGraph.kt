package com.spamsentinel.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.ui.screens.ChatbotScreen
import com.spamsentinel.app.ui.screens.ChatbotUiState
import com.spamsentinel.app.ui.screens.ChatbotViewModel
import com.spamsentinel.app.ui.screens.HomeScreen
import com.spamsentinel.app.ui.screens.HomeUiState
import com.spamsentinel.app.ui.screens.HomeViewModel
import com.spamsentinel.app.ui.screens.InsightsScreen
import com.spamsentinel.app.ui.screens.InsightsUiState
import com.spamsentinel.app.ui.screens.InsightsViewModel
import com.spamsentinel.app.ui.screens.LiveCheckerScreen
import com.spamsentinel.app.ui.screens.LiveCheckerUiState
import com.spamsentinel.app.ui.screens.LiveCheckerViewModel
import com.spamsentinel.app.ui.screens.MessageDetailScreen
import com.spamsentinel.app.ui.screens.MessageDetailUiState
import com.spamsentinel.app.ui.screens.MessageDetailViewModel
import com.spamsentinel.app.ui.screens.MessagesScreen
import com.spamsentinel.app.ui.screens.MessagesUiState
import com.spamsentinel.app.ui.screens.MessagesViewModel
import com.spamsentinel.app.ui.screens.OnboardingScreen
import com.spamsentinel.app.ui.screens.SettingsScreen
import com.spamsentinel.app.ui.screens.SettingsUiState
import com.spamsentinel.app.ui.screens.SettingsViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Messages : Screen("messages", "Messages", Icons.Default.Message)
    object Chatbot : Screen("chatbot", "Sentinel AI", Icons.Default.SmartToy)
    object Checker : Screen("checker", "Checker", Icons.Default.FlashOn)
    object Insights : Screen("insights", "Insights", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object MessageDetail : Screen("detail/{id}", "Detail", Icons.Default.Message) {
        fun createRoute(id: Long) = "detail/$id"
    }
}

@Composable
fun MainAppNavigation(
    startDestination: String = Screen.Home.route
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SpamSentinelApp
    val repository = app.container.repository

    val homeViewModel = remember { HomeViewModel(repository) }
    val messagesViewModel = remember { MessagesViewModel(repository) }
    val detailViewModel = remember { MessageDetailViewModel(repository) }
    val checkerViewModel = remember { LiveCheckerViewModel(repository) }
    val insightsViewModel = remember { InsightsViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }
    val chatbotViewModel = remember { ChatbotViewModel() }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Messages,
        Screen.Chatbot,
        Screen.Checker,
        Screen.Insights,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val state: HomeUiState by homeViewModel.uiState.collectAsState()
                HomeScreen(
                    uiState = state,
                    onProtectionToggle = homeViewModel::setProtectionActive,
                    onNavigateToChecker = { navController.navigate(Screen.Checker.route) },
                    onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) },
                    onMessageClick = { id -> navController.navigate(Screen.MessageDetail.createRoute(id)) },
                    onMarkSafe = homeViewModel::markAsSafe,
                    onBlockSender = homeViewModel::blockSender
                )
            }

            composable(Screen.Messages.route) {
                val state: MessagesUiState by messagesViewModel.uiState.collectAsState()
                MessagesScreen(
                    uiState = state,
                    onTabSelected = messagesViewModel::setTab,
                    onSearchQueryChanged = messagesViewModel::setSearchQuery,
                    onScamTypeFilterChanged = messagesViewModel::setScamTypeFilter,
                    onToggleSelection = messagesViewModel::toggleSelection,
                    onClearSelection = messagesViewModel::clearSelection,
                    onBulkMarkSafe = messagesViewModel::bulkMarkAsSafe,
                    onBulkBlockSender = messagesViewModel::bulkBlockSender,
                    onBulkDelete = messagesViewModel::bulkDelete,
                    onMessageClick = { id -> navController.navigate(Screen.MessageDetail.createRoute(id)) }
                )
            }

            composable(Screen.Chatbot.route) {
                val state: ChatbotUiState by chatbotViewModel.uiState.collectAsState()
                ChatbotScreen(
                    uiState = state,
                    promptCategories = chatbotViewModel.promptCategories,
                    onInputTextChanged = chatbotViewModel::onInputTextChanged,
                    onSendMessage = chatbotViewModel::sendMessage,
                    onClearChat = chatbotViewModel::clearChat,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Checker.route) {
                val state: LiveCheckerUiState by checkerViewModel.uiState.collectAsState()
                LiveCheckerScreen(
                    uiState = state,
                    sampleSmsList = checkerViewModel.sampleSmsList,
                    onSenderChanged = checkerViewModel::onSenderChanged,
                    onTextChanged = checkerViewModel::onTextChanged,
                    onLoadSample = checkerViewModel::loadSample,
                    onSimulateIncomingSms = { checkerViewModel.simulateIncomingSms(context) },
                    onSeedDemoData = checkerViewModel::seedDemoData
                )
            }

            composable(Screen.Insights.route) {
                val state: InsightsUiState by insightsViewModel.uiState.collectAsState()
                InsightsScreen(
                    uiState = state,
                    onTabSelected = insightsViewModel::setTab,
                    onLoadData = insightsViewModel::loadBenchmarkData,
                    onCFPChanged = insightsViewModel::setCFP,
                    onCFNChanged = insightsViewModel::setCFN,
                    onCRChanged = insightsViewModel::setCR,
                    onTauSimChanged = insightsViewModel::setTauSim
                )
            }

            composable(Screen.Settings.route) {
                val state: SettingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsScreen(
                    uiState = state,
                    onProtectionToggle = settingsViewModel::setProtectionActive,
                    onTauLowChanged = settingsViewModel::setTauLow,
                    onTauHighChanged = settingsViewModel::setTauHigh,
                    onStrictModeToggle = settingsViewModel::setStrictMode,
                    onRetrainOnChargingToggle = settingsViewModel::setRetrainOnCharging,
                    onBiometricToggle = settingsViewModel::setBiometricEnabled,
                    onPrivacyBlurToggle = settingsViewModel::setPrivacyBlur,
                    onTrustContactsToggle = settingsViewModel::setTrustContacts,
                    onWeeklyDigestToggle = settingsViewModel::setWeeklyDigestEnabled,
                    onAddRule = settingsViewModel::addRule,
                    onRemoveRule = settingsViewModel::removeRule,
                    onRetrainNow = settingsViewModel::triggerRetrainNow,
                    onRunSelfTest = settingsViewModel::runModelSelfTest,
                    onClearAllData = settingsViewModel::clearAllData
                )
            }

            composable(
                route = Screen.MessageDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: -1L
                val state: MessageDetailUiState by detailViewModel.uiState.collectAsState()

                androidx.compose.runtime.LaunchedEffect(id) {
                    detailViewModel.loadMessage(id)
                }

                MessageDetailScreen(
                    uiState = state,
                    onBackClick = { navController.popBackStack() },
                    onMarkSafe = detailViewModel::markAsSafe,
                    onMarkSpam = detailViewModel::markAsSpam
                )
            }
        }
    }
}
