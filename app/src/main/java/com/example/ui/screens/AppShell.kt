package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.UserMode
import com.example.data.repository.GuildRepository
import com.example.data.repository.signOutUser
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SaffronAmberDark
import com.example.ui.theme.SaffronAmberLight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmCoral
import com.example.ui.viewmodel.GuildViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable object ProjectsRoute
@Serializable object CreatorsRoute
@Serializable object InboxRoute
@Serializable object StudioRoute
@Serializable data class ThreadRoute(val conversationId: String)

private data class TopTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String
)

private val TOP_TABS = listOf(
    TopTab(R.string.nav_projects, Icons.Filled.Work, Icons.Outlined.WorkOutline, ProjectsRoute, "nav_projects"),
    TopTab(R.string.nav_creators, Icons.Filled.Groups, Icons.Outlined.Groups, CreatorsRoute, "nav_creators"),
    TopTab(R.string.nav_inbox, Icons.Filled.Forum, Icons.Outlined.Forum, InboxRoute, "nav_inbox"),
    TopTab(R.string.nav_studio, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, StudioRoute, "nav_studio")
)

@Composable
fun AppShell(
    currentUserId: String,
    initialMode: UserMode = UserMode.CREATOR
) {
    val viewModel: GuildViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GuildViewModel(GuildRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val userAccountState by viewModel.userAccount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    when (val accountState = userAccountState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = SaffronAmber)
                    Text(
                        text = "Loading your studio...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ErrorStudioBanner(message = accountState.message)
                    OutlinedButton(
                        onClick = { signOutUser(credentialManager, scope) },
                        border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = WarmCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = WarmCoral)
                    }
                }
            }
        }

        is UiState.Success -> {
            val registeredAccount = accountState.data
            if (registeredAccount == null) {
                Scaffold(
                    containerColor = ObsidianBg,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        RegistrationSetupScreen(
                            initialMode = initialMode,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                val isThreadScreen = currentDestination?.hasRoute<ThreadRoute>() == true

                fun navigateTopTab(route: Any) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = ObsidianBg,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                if (!useRail && !isThreadScreen) {
                    NavigationBar(
                        containerColor = StudioSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SaffronAmberLight,
                                    selectedTextColor = SaffronAmberLight,
                                    indicatorColor = SaffronAmberDark,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isThreadScreen) {
                    NavigationRail(containerColor = StudioSurface) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SaffronAmberLight,
                                    selectedTextColor = SaffronAmberLight,
                                    indicatorColor = SaffronAmberDark,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ProjectsRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<ProjectsRoute> {
                        ProjectsScreen(
                            viewModel = viewModel,
                            onNavigateToThread = { convId ->
                                navController.navigate(ThreadRoute(convId))
                            },
                            onNavigateToStudio = { navigateTopTab(StudioRoute) }
                        )
                    }
                    composable<CreatorsRoute> {
                        CreatorsScreen(
                            viewModel = viewModel,
                            onNavigateToThread = { convId ->
                                navController.navigate(ThreadRoute(convId))
                            },
                            onNavigateToStudio = { navigateTopTab(StudioRoute) }
                        )
                    }
                    composable<InboxRoute> {
                        InboxScreen(
                            viewModel = viewModel,
                            onOpenConversation = { convId ->
                                navController.navigate(ThreadRoute(convId))
                            },
                            onExploreProjects = { navigateTopTab(ProjectsRoute) }
                        )
                    }
                    composable<StudioRoute> {
                        StudioScreen(
                            viewModel = viewModel,
                            onNavigateToThread = { convId ->
                                navController.navigate(ThreadRoute(convId))
                            },
                            onBrowseProjects = { navigateTopTab(ProjectsRoute) }
                        )
                    }
                    composable<ThreadRoute> { entry ->
                        val args = entry.toRoute<ThreadRoute>()
                        ChatThreadScreen(
                            conversationId = args.conversationId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
                }
            }
        }
    }
}
