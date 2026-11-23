package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.PaidProject
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.UiState
import com.example.data.model.UserMode
import com.example.data.model.countProjectsByCategory
import com.example.data.model.filterProjects
import com.example.data.model.formatInr
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.components.HubTag
import com.example.ui.components.SkillBadgeCluster
import com.example.ui.components.SkillFilterRow
import com.example.ui.components.StatusPill
import com.example.ui.components.accentColor
import com.example.ui.components.icon
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElectricTealDark
import com.example.ui.theme.ElectricTealLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SaffronAmberDark
import com.example.ui.theme.SaffronAmberLight
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.data.ai.AiDraftedProjectBrief
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiGenerationState
import com.example.ui.viewmodel.GuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: GuildViewModel,
    onNavigateToThread: (String) -> Unit,
    onNavigateToStudio: () -> Unit
) {
    val projectsState by viewModel.publicProjects.collectAsStateWithLifecycle()
    val userState by viewModel.userAccount.collectAsStateWithLifecycle()
    val myPortfolioState by viewModel.myCreatorProfile.collectAsStateWithLifecycle()
    val selectedSkill by viewModel.selectedSkill.collectAsStateWithLifecycle()
    val selectedHub by viewModel.selectedHub.collectAsStateWithLifecycle()
    val remoteOnly by viewModel.remoteOnly.collectAsStateWithLifecycle()
    val availableOnly by viewModel.availableOnly.collectAsStateWithLifecycle()
    val aiPitchState by viewModel.aiPitchState.collectAsStateWithLifecycle()
    val aiBriefState by viewModel.aiBriefState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeProject by remember { mutableStateOf<PaidProject?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val userAccount = (userState as? UiState.Success)?.data
    val myPortfolio = (myPortfolioState as? UiState.Success)?.data
    val allProjects = (projectsState as? UiState.Success)?.data.orEmpty()

    val categoryCounts = remember(allProjects, availableOnly) {
        countProjectsByCategory(allProjects, availableOnly = availableOnly)
    }
    val scopedTotalCount = remember(allProjects, availableOnly) {
        if (availableOnly) allProjects.count { it.isAvailable() } else allProjects.size
    }
    val availableTotalCount = remember(allProjects) {
        allProjects.count { it.isAvailable() }
    }
    val filteredProjects = remember(
        allProjects,
        selectedSkill,
        selectedHub,
        remoteOnly,
        availableOnly,
        searchQuery
    ) {
        filterProjects(
            projects = allProjects,
            selectedSkill = selectedSkill,
            selectedHub = selectedHub,
            remoteOnly = remoteOnly,
            availableOnly = availableOnly,
            searchQuery = searchQuery
        )
    }
    val activeHubCount = remember(allProjects, availableOnly) {
        val scoped = if (availableOnly) allProjects.filter { it.isAvailable() } else allProjects
        scoped.map { it.cityHub.lowercase() }.distinct().size
    }
    val hasActiveFilters = selectedSkill != null ||
        selectedHub != null ||
        remoteOnly ||
        !availableOnly ||
        searchQuery.isNotBlank()

    val currentMode = userAccount?.modeEnum() ?: UserMode.CREATOR

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            if (currentMode == UserMode.CLIENT) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateSheet = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Post New Project", fontWeight = FontWeight.Bold) },
                    containerColor = SaffronAmber,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("post_project_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "IndiAI Dashboard",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (!userAccount?.displayName.isNullOrBlank()) {
                                        "Hi ${userAccount?.displayName} • Live AI freelance projects"
                                    } else {
                                        "Live AI freelance projects across India"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            val currentMode = userAccount?.modeEnum() ?: UserMode.CREATOR
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (currentMode == UserMode.CREATOR) ElectricTealDark else SaffronAmberDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (currentMode == UserMode.CREATOR) ElectricTeal else SaffronAmber
                                ),
                                modifier = Modifier
                                    .clickable {
                                        val next = if (currentMode == UserMode.CREATOR) UserMode.CLIENT else UserMode.CREATOR
                                        viewModel.switchMode(next, userAccount?.cityHub ?: INDIA_TECH_HUBS.first().name)
                                    }
                                    .testTag("mode_toggle_pill")
                            ) {
                                Text(
                                    text = currentMode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (currentMode == UserMode.CREATOR) ElectricTealLight else SaffronAmberLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Live Dashboard Metrics Summary Card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = StudioSurface,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_summary_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                StudioSurfaceElevated,
                                                ElectricTealDark.copy(alpha = 0.35f),
                                                SaffronAmberDark.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(ElectricTeal)
                                        )
                                        Text(
                                            text = "LIVE FIRESTORE FEED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricTealLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (userAccount?.modeEnum() == UserMode.CLIENT) {
                                        Text(
                                            text = "+ Post Project",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SaffronAmberLight,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showCreateSheet = true }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("dashboard_post_project_link")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DashboardStatTile(
                                        value = availableTotalCount.toString(),
                                        label = "Available",
                                        accent = SaffronAmberLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DashboardStatTile(
                                        value = filteredProjects.size.toString(),
                                        label = selectedSkill?.shortLabel ?: "Showing",
                                        accent = ElectricTealLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DashboardStatTile(
                                        value = activeHubCount.toString(),
                                        label = "India Hubs",
                                        accent = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dashboard_hubs_stat")
                                    )
                                }
                            }
                        }

                        if (currentMode == UserMode.CLIENT) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = ElectricTealDark.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateSheet = true }
                                    .testTag("client_mode_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Client Mode • Post a New AI Project",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ElectricTealLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Share title, deliverables, budget in ₹ INR & category to receive creator DMs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { showCreateSheet = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SaffronAmber,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("client_banner_post_button")
                                    ) {
                                        Text("Post", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (myPortfolio == null) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = SaffronAmberDark.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToStudio() }
                                    .testTag("complete_portfolio_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Publish your AI Creator Portfolio",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = SaffronAmberLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Attach your skills & INR rate automatically when you DM clients.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "Setup",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SaffronAmber
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateSearchQuery("") },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            placeholder = { Text("Search AI video, image, web dev, chatbots, city...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("projects_search_input")
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkillFilterRow(
                            selectedSkill = selectedSkill,
                            onSelectSkill = viewModel::selectSkillFilter,
                            totalCount = if (projectsState is UiState.Success) scopedTotalCount else null,
                            categoryCounts = if (projectsState is UiState.Success) categoryCounts else null
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = availableOnly,
                                    onClick = viewModel::toggleAvailableOnly,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(if (availableOnly) "Available Only" else "All Statuses") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricTealDark,
                                        selectedLabelColor = ElectricTealLight,
                                        selectedLeadingIconColor = ElectricTealLight,
                                        containerColor = StudioSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("available_only_chip")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = remoteOnly,
                                    onClick = viewModel::toggleRemoteOnly,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Remote India OK") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronAmberDark,
                                        selectedLabelColor = SaffronAmberLight,
                                        selectedLeadingIconColor = SaffronAmberLight,
                                        containerColor = StudioSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("remote_filter_chip")
                                )
                            }
                            if (selectedHub != null) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { viewModel.selectHubFilter(null) },
                                        label = { Text("Hub: $selectedHub ×") }
                                    )
                                }
                            }
                            if (hasActiveFilters) {
                                item {
                                    TextButton(
                                        onClick = viewModel::clearProjectFilters,
                                        modifier = Modifier.testTag("reset_filters_button")
                                    ) {
                                        Text("Reset", color = SaffronAmberLight)
                                    }
                                }
                            }
                        }
                    }
                }

                when (val state = projectsState) {
                    is UiState.Loading -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = SaffronAmber)
                                Text(
                                    text = "Fetching AI freelance projects from Firestore...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            ErrorStudioBanner(
                                message = state.message,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                EmptyStudioCard(
                                    title = "No paid AI projects posted yet",
                                    subtitle = if (currentMode == UserMode.CLIENT) {
                                        "Share a paid AI video, image, web, chatbot, or automation project with Indian freelancers."
                                    } else {
                                        "Projects shared by clients appear here live. Switch to Client Mode if you want to post a paid project."
                                    },
                                    icon = Icons.Filled.WorkOutline,
                                    actionLabel = if (currentMode == UserMode.CLIENT) "Post New Project" else "Switch to Client Mode",
                                    onAction = {
                                        if (currentMode == UserMode.CLIENT) {
                                            showCreateSheet = true
                                        } else {
                                            viewModel.switchMode(
                                                UserMode.CLIENT,
                                                userAccount?.cityHub ?: INDIA_TECH_HUBS.first().name
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else if (filteredProjects.isEmpty()) {
                            item {
                                val categoryName = selectedSkill?.displayName ?: "selected filters"
                                EmptyStudioCard(
                                    title = "No projects in $categoryName",
                                    subtitle = "There are ${state.data.size} project(s) on the network. Clear or adjust your filters to view them.",
                                    icon = Icons.Filled.Search,
                                    actionLabel = "Show All Categories",
                                    onAction = viewModel::clearProjectFilters,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedSkill != null) {
                                            "${selectedSkill!!.displayName} (${filteredProjects.size})"
                                        } else if (availableOnly) {
                                            "Available Projects (${filteredProjects.size})"
                                        } else {
                                            "All Projects (${filteredProjects.size})"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Sorted: Newest",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            items(filteredProjects, key = { it.id }) { project ->
                                ProjectCard(
                                    project = project,
                                    isMine = project.clientId == viewModel.currentUserId,
                                    onClick = { activeProject = project },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeProject?.let { project ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pitchNote by remember(project.id) {
            mutableStateOf("Hi ${project.clientName}, I'd love to take on \"${project.title}\". Sharing my AI portfolio below!")
        }
        var attachPortfolio by remember(project.id) { mutableStateOf(myPortfolio != null) }

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.resetAiPitchState()
                activeProject = null
            },
            sheetState = sheetState,
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(status = project.statusEnum())
                    Text(
                        text = "${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SaffronAmberLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Posted by ${project.clientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricTealLight
                )
                Spacer(Modifier.height(8.dp))
                HubTag(hubName = project.cityHub, remoteAllowed = project.remoteAllowed)
                Spacer(Modifier.height(12.dp))
                SkillBadgeCluster(skills = project.skillEnums())
                Spacer(Modifier.height(14.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                if (!project.selectedCreatorName.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ElectricTealDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ElectricTealLight)
                            Text(
                                text = "Selected Freelancer: ${project.selectedCreatorName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricTealLight
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (project.clientId == viewModel.currentUserId) {
                    Text(
                        text = "Manage Project Status",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (project.statusEnum() == ProjectStatus.OPEN) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.advanceProjectStatus(project.id, ProjectStatus.SHORTLISTING)
                                    activeProject = null
                                }
                            ) { Text("Mark Shortlisting") }
                        }
                        if (project.statusEnum() != ProjectStatus.COMPLETED) {
                            Button(
                                onClick = {
                                    viewModel.advanceProjectStatus(project.id, ProjectStatus.COMPLETED)
                                    activeProject = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                            ) { Text("Mark Completed") }
                        }
                    }
                } else {
                    val isPitchLoading =
                        aiPitchState is AiGenerationState.Loading || aiPitchState is AiGenerationState.Streaming
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Pitch",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.draftPitchWithGemini(project) { streamedText ->
                                    pitchNote = streamedText
                                }
                            },
                            enabled = !isPitchLoading,
                            border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("gemini_draft_pitch_button")
                        ) {
                            if (isPitchLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricTealLight
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Drafting...", color = ElectricTealLight)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricTealLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Draft with Gemini", color = ElectricTealLight)
                            }
                        }
                    }
                    if (aiPitchState is AiGenerationState.Error) {
                        Spacer(Modifier.height(6.dp))
                        ErrorStudioBanner(message = (aiPitchState as AiGenerationState.Error).message)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pitchNote,
                        onValueChange = { pitchNote = it },
                        label = { Text("Quick DM Intro") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_dm_intro_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = attachPortfolio,
                            onCheckedChange = { checked ->
                                if (checked && myPortfolio == null) {
                                    activeProject = null
                                    onNavigateToStudio()
                                } else {
                                    attachPortfolio = checked
                                }
                            }
                        )
                        Text(
                            text = if (myPortfolio != null) {
                                "Attach my Creator Portfolio Card (${myPortfolio.headline})"
                            } else {
                                "Create Creator Portfolio in Studio to attach card"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.startDmWithPortfolio(
                                partnerUid = project.clientId,
                                partnerName = project.clientName,
                                project = project,
                                introText = pitchNote,
                                attachPortfolio = attachPortfolio,
                                onThreadReady = { convId ->
                                    activeProject = null
                                    onNavigateToThread(convId)
                                }
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dm_with_portfolio_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("DM Client with Portfolio", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showCreateSheet && currentMode == UserMode.CLIENT) {
        PostNewProjectSheet(
            defaultClientName = userAccount?.displayName?.takeIf { it.trim().length >= 2 } ?: "Studio Lead",
            defaultCityHub = userAccount?.cityHub ?: INDIA_TECH_HUBS.first().name,
            isSubmitting = isSubmitting,
            aiBriefState = aiBriefState,
            onDraftWithGemini = { idea, hub, onDrafted ->
                viewModel.draftProjectBriefWithGemini(idea, hub, onDrafted)
            },
            onDismiss = {
                viewModel.resetAiBriefState()
                showCreateSheet = false
            },
            onSubmit = { clientName, title, desc, skills, minInr, maxInr, hub, remote ->
                viewModel.createProject(
                    clientName = clientName,
                    title = title,
                    description = desc,
                    requiredSkills = skills,
                    budgetMinInr = minInr,
                    budgetMaxInr = maxInr,
                    cityHubName = hub,
                    remoteAllowed = remote,
                    onCreated = {
                        viewModel.resetAiBriefState()
                        showCreateSheet = false
                    }
                )
            }
        )
    }
}

@Composable
private fun DashboardStatTile(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = StudioSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: PaidProject,
    isMine: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leadAccent = project.skillEnums().firstOrNull()?.accentColor() ?: SaffronAmber
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, if (isMine) SaffronAmber.copy(alpha = 0.55f) else StudioBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(leadAccent.copy(alpha = 0.75f))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(status = project.statusEnum())
                    Text(
                        text = "${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = SaffronAmberLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "by ${project.clientName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricTealLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                SkillBadgeCluster(skills = project.skillEnums())
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HubTag(hubName = project.cityHub, remoteAllowed = project.remoteAllowed)
                    Text(
                        text = if (isMine) "Manage Post →" else "DM with Portfolio →",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricTealLight
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PostNewProjectSheet(
    defaultClientName: String,
    defaultCityHub: String,
    isSubmitting: Boolean,
    aiBriefState: AiGenerationState,
    onDraftWithGemini: (String, String, (AiDraftedProjectBrief) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, List<SkillCategory>, Long, Long, String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aiIdea by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf(defaultClientName) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budgetMin by remember { mutableStateOf("25000") }
    var budgetMax by remember { mutableStateOf("65000") }
    var selectedHub by remember { mutableStateOf(defaultCityHub) }
    var remoteAllowed by remember { mutableStateOf(true) }
    val selectedSkills = remember { mutableStateListOf(SkillCategory.AI_VIDEO) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val budgetPresets = remember {
        listOf(
            "₹15k" to ("15000" to "25000"),
            "₹35k" to ("25000" to "45000"),
            "₹75k" to ("50000" to "85000"),
            "₹1.5L+" to ("100000" to "200000")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Post New Project",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Published live to IndiAI Guild creators",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricTealDark,
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "CLIENT MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricTealLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricTealLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Gemini Brief Assistant",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricTealLight
                        )
                    }
                    OutlinedTextField(
                        value = aiIdea,
                        onValueChange = { aiIdea = it },
                        label = { Text("Describe your gig in 1 line") },
                        placeholder = { Text("e.g., 30s Hindi AI product reel for D2C launch") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_brief_idea_input")
                    )
                    val isBriefLoading = aiBriefState is AiGenerationState.Loading
                    OutlinedButton(
                        onClick = {
                            val promptInput = aiIdea.ifBlank { title }
                            onDraftWithGemini(promptInput, selectedHub) { drafted ->
                                title = drafted.title
                                description = drafted.description
                                budgetMin = drafted.budgetMinInr.toString()
                                budgetMax = drafted.budgetMaxInr.toString()
                                selectedSkills.clear()
                                selectedSkills.addAll(drafted.skills)
                                validationError = null
                            }
                        },
                        enabled = !isBriefLoading && (aiIdea.trim().length >= 4 || title.trim().length >= 4),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_draft_brief_button")
                    ) {
                        if (isBriefLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ElectricTealLight
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Drafting Brief with Gemini...", color = ElectricTealLight)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricTealLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Auto-Fill Brief & Budget with Gemini", color = ElectricTealLight)
                        }
                    }
                    if (aiBriefState is AiGenerationState.Error) {
                        ErrorStudioBanner(message = aiBriefState.message)
                    }
                }
            }

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it.take(60) },
                label = { Text("Client or Studio Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_project_client_name")
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("Project Title") },
                placeholder = { Text("e.g., D2C Festive AI Brand Film & Reels") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_project_title")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(1000) },
                label = { Text("Project Description & Deliverables") },
                placeholder = { Text("Describe expected outputs, tools, timeline, and reference style...") },
                supportingText = { Text("${description.trim().length}/1000 chars (min 10)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_project_description")
            )

            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkillCategory.entries.forEach { skill ->
                    val active = skill in selectedSkills
                    FilterChip(
                        selected = active,
                        onClick = {
                            if (active && selectedSkills.size > 1) selectedSkills.remove(skill)
                            else if (!active && selectedSkills.size < 5) selectedSkills.add(skill)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = skill.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(skill.shortLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricTealDark,
                            selectedLabelColor = ElectricTealLight,
                            selectedLeadingIconColor = ElectricTealLight,
                            containerColor = StudioSurfaceElevated,
                            labelColor = TextSecondary,
                            iconColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("post_category_${skill.wireName.lowercase()}")
                    )
                }
            }

            Text(
                text = "Budget (₹ INR)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(budgetPresets) { (label, range) ->
                    val isPresetActive = budgetMin == range.first && budgetMax == range.second
                    FilterChip(
                        selected = isPresetActive,
                        onClick = {
                            budgetMin = range.first
                            budgetMax = range.second
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SaffronAmberDark,
                            selectedLabelColor = SaffronAmberLight,
                            containerColor = StudioSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = budgetMin,
                    onValueChange = { budgetMin = it.filter(Char::isDigit).take(8) },
                    label = { Text("Budget / Min (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_project_budget_min")
                )
                OutlinedTextField(
                    value = budgetMax,
                    onValueChange = { budgetMax = it.filter(Char::isDigit).take(8) },
                    label = { Text("Max (₹, optional)") },
                    placeholder = { Text(budgetMin.ifEmpty { "Same as min" }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_project_budget_max")
                )
            }

            Text(
                text = "India Tech Hub",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                INDIA_TECH_HUBS.forEach { hub ->
                    FilterChip(
                        selected = selectedHub == hub.name,
                        onClick = { selectedHub = hub.name },
                        label = { Text(hub.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open to Remote Creators Across India", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = remoteAllowed, onCheckedChange = { remoteAllowed = it })
            }

            if (validationError != null) {
                ErrorStudioBanner(message = validationError!!)
            }

            Button(
                onClick = {
                    val minVal = budgetMin.toLongOrNull() ?: 0L
                    val maxVal = if (budgetMax.isBlank()) minVal else (budgetMax.toLongOrNull() ?: 0L)
                    when {
                        clientName.trim().length < 2 -> validationError = "Enter a client or studio name (at least 2 characters)."
                        title.trim().length < 4 -> validationError = "Project title must be at least 4 characters."
                        description.trim().length < 10 -> validationError = "Description must be at least 10 characters."
                        selectedSkills.isEmpty() -> validationError = "Select at least one project category."
                        minVal !in 500L..10000000L -> validationError = "Budget must be between ₹500 and ₹1,00,00,000."
                        maxVal !in minVal..10000000L -> validationError = "Max budget must be >= Min budget and <= ₹1,00,00,000."
                        else -> {
                            validationError = null
                            onSubmit(
                                clientName,
                                title,
                                description,
                                selectedSkills.toList(),
                                minVal,
                                maxVal,
                                selectedHub,
                                remoteAllowed
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronAmber,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_project_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Post Project to Firestore", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
