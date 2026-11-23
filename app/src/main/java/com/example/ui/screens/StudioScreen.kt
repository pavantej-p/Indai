package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AppliedProjectHistoryItem
import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.PaidProject
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.UiState
import com.example.data.model.UserMode
import com.example.data.model.formatInr
import com.example.data.model.mergeAppliedProjectHistory
import com.example.data.repository.signOutUser
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.components.HubTag
import com.example.ui.components.SkillBadgeCluster
import com.example.ui.components.StatusPill
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmCoral
import com.example.ui.viewmodel.AiGenerationState
import com.example.ui.viewmodel.GuildViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private enum class ProfileHistoryTab {
    APPLIED,
    POSTED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: GuildViewModel,
    onNavigateToThread: (String) -> Unit = {},
    onBrowseProjects: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val userState by viewModel.userAccount.collectAsStateWithLifecycle()
    val myPortfolioState by viewModel.myCreatorProfile.collectAsStateWithLifecycle()
    val projectsState by viewModel.publicProjects.collectAsStateWithLifecycle()
    val myApplicationsState by viewModel.myApplications.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val aiProfileState by viewModel.aiProfileState.collectAsStateWithLifecycle()

    val userAccount = (userState as? UiState.Success)?.data
    val myPortfolio = (myPortfolioState as? UiState.Success)?.data
    val allProjects = (projectsState as? UiState.Success)?.data.orEmpty()
    val myApplications = (myApplicationsState as? UiState.Success)?.data.orEmpty()
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()

    val myPostedProjects = remember(allProjects, viewModel.currentUserId) {
        allProjects.filter { it.clientId == viewModel.currentUserId }
    }
    val appliedHistory = remember(viewModel.currentUserId, myApplications, conversations, allProjects) {
        mergeAppliedProjectHistory(
            currentUserId = viewModel.currentUserId,
            applications = myApplications,
            conversations = conversations,
            publicProjects = allProjects
        )
    }

    val firebaseUser = Firebase.auth.currentUser
    var displayName by remember { mutableStateOf(firebaseUser?.displayName ?: "AI Creator") }
    var headline by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var cityHub by remember { mutableStateOf(INDIA_TECH_HUBS.first().name) }
    var hourlyRate by remember { mutableStateOf("1800") }
    var portfolioUrl by remember { mutableStateOf("") }
    var sampleTitle by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    val selectedSkills = remember { mutableStateListOf(SkillCategory.AI_VIDEO) }
    var initializedFromDb by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var historyTab by rememberSaveable { mutableStateOf(ProfileHistoryTab.APPLIED) }
    var tabInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(myPortfolio, userAccount) {
        if (!initializedFromDb && myPortfolio != null) {
            displayName = myPortfolio.displayName
            headline = myPortfolio.headline
            bio = myPortfolio.bio.orEmpty()
            cityHub = myPortfolio.cityHub
            hourlyRate = myPortfolio.hourlyRateInr.toString()
            portfolioUrl = myPortfolio.portfolioUrl.orEmpty()
            sampleTitle = myPortfolio.sampleWorkTitle.orEmpty()
            available = myPortfolio.availableForPaidWork
            selectedSkills.clear()
            selectedSkills.addAll(myPortfolio.skillEnums().ifEmpty { listOf(SkillCategory.AI_VIDEO) })
            initializedFromDb = true
        } else if (!initializedFromDb && userAccount != null) {
            displayName = userAccount.displayName
            cityHub = userAccount.cityHub
        }
    }

    LaunchedEffect(userAccount, appliedHistory.size, myPostedProjects.size) {
        if (!tabInitialized && userAccount != null) {
            if (userAccount.modeEnum() == UserMode.CLIENT &&
                appliedHistory.isEmpty() &&
                myPostedProjects.isNotEmpty()
            ) {
                historyTab = ProfileHistoryTab.POSTED
            }
            tabInitialized = true
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileIdentityHeroCard(
                        displayName = userAccount?.displayName ?: firebaseUser?.displayName ?: "Member Profile",
                        email = firebaseUser?.email ?: "Signed in with Google",
                        photoUrl = firebaseUser?.photoUrl?.toString(),
                        cityHub = userAccount?.cityHub ?: cityHub,
                        currentMode = userAccount?.modeEnum() ?: UserMode.CREATOR,
                        skillsCount = myPortfolio?.skills?.size ?: selectedSkills.size,
                        appliedCount = appliedHistory.size,
                        postedCount = myPostedProjects.size,
                        onSwitchMode = { newMode -> viewModel.switchMode(newMode, cityHub) },
                        onSignOut = { signOutUser(credentialManager, scope) }
                    )
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = StudioSurface,
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Upload Skills & Creator Portfolio",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Stored in Firestore & visible to clients hiring AI talent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (myPortfolio != null) {
                                    Surface(shape = MaterialTheme.shapes.small, color = ElectricTealDark) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = ElectricTealLight,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "Live",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricTealLight
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Creator Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = headline,
                                onValueChange = { headline = it },
                                label = { Text("Specialty Headline") },
                                placeholder = { Text("e.g., AI Video Director & n8n Automation Builder") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portfolio_headline_input")
                            )
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Experience & AI Stack") },
                                placeholder = { Text("Tools you use (Runway, Flux, Next.js, LangGraph, Make)...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val isAiPolishing = aiProfileState is AiGenerationState.Loading
                            OutlinedButton(
                                onClick = {
                                    viewModel.polishProfileWithGemini(
                                        displayName = displayName,
                                        currentHeadline = headline,
                                        currentBio = bio,
                                        skills = selectedSkills.toList(),
                                        cityHub = cityHub,
                                        sampleWorkTitle = sampleTitle
                                    ) { polished ->
                                        headline = polished.headline
                                        bio = polished.bio
                                        validationError = null
                                    }
                                },
                                enabled = !isAiPolishing,
                                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("gemini_polish_profile_button")
                            ) {
                                if (isAiPolishing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ElectricTealLight
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Polishing with Gemini...", color = ElectricTealLight)
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricTealLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Polish Headline & Bio with Gemini", color = ElectricTealLight)
                                }
                            }
                            if (aiProfileState is AiGenerationState.Error) {
                                ErrorStudioBanner(message = (aiProfileState as AiGenerationState.Error).message)
                            }

                            Text("AI Specialties (1–5 skills)", style = MaterialTheme.typography.titleSmall)
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
                                            selectedLeadingIconColor = ElectricTealLight
                                        ),
                                        modifier = Modifier.testTag("profile_skill_${skill.wireName.lowercase()}")
                                    )
                                }
                            }

                            Text("Primary India Tech Hub", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                INDIA_TECH_HUBS.forEach { hub ->
                                    FilterChip(
                                        selected = cityHub == hub.name,
                                        onClick = { cityHub = hub.name },
                                        label = { Text(hub.name) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = hourlyRate,
                                onValueChange = { hourlyRate = it.filter(Char::isDigit) },
                                label = { Text("Hourly Rate (₹ INR)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = sampleTitle,
                                onValueChange = { sampleTitle = it },
                                label = { Text("Featured Sample Work Title") },
                                placeholder = { Text("e.g., AI Ad Film for Fintech Launch") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = portfolioUrl,
                                onValueChange = { portfolioUrl = it },
                                label = { Text("Portfolio / Reel URL") },
                                placeholder = { Text("https://behance.net/... or GitHub / Drive") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portfolio_url_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Available for Paid Projects", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = available, onCheckedChange = { available = it })
                            }

                            if (validationError != null) {
                                ErrorStudioBanner(message = validationError!!)
                            }

                            Button(
                                onClick = {
                                    val rate = hourlyRate.toLongOrNull() ?: 0L
                                    when {
                                        displayName.trim().length < 2 ->
                                            validationError = "Display name must be at least 2 characters."
                                        headline.trim().length < 4 ->
                                            validationError = "Headline must be at least 4 characters."
                                        rate !in 0L..500000L ->
                                            validationError = "Hourly rate must be between ₹0 and ₹5,00,000."
                                        else -> {
                                            validationError = null
                                            viewModel.savePortfolio(
                                                displayName = displayName,
                                                headline = headline,
                                                bio = bio,
                                                skills = selectedSkills.toList(),
                                                cityHubName = cityHub,
                                                hourlyRateInr = rate,
                                                portfolioUrl = portfolioUrl,
                                                sampleWorkTitle = sampleTitle,
                                                availableForPaidWork = available
                                            )
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("save_portfolio_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save & Upload Skills", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkHistory,
                                contentDescription = null,
                                tint = SaffronAmber
                            )
                            Text(
                                text = "Project History",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HistoryTabButton(
                                label = "Applied (${appliedHistory.size})",
                                selected = historyTab == ProfileHistoryTab.APPLIED,
                                accentColor = SaffronAmber,
                                containerColor = SaffronAmberDark,
                                textColor = SaffronAmberLight,
                                onClick = { historyTab = ProfileHistoryTab.APPLIED },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("history_tab_applied")
                            )
                            HistoryTabButton(
                                label = "Posted (${myPostedProjects.size})",
                                selected = historyTab == ProfileHistoryTab.POSTED,
                                accentColor = ElectricTeal,
                                containerColor = ElectricTealDark,
                                textColor = ElectricTealLight,
                                onClick = { historyTab = ProfileHistoryTab.POSTED },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("history_tab_posted")
                            )
                        }
                    }
                }

                if (historyTab == ProfileHistoryTab.APPLIED && myApplicationsState is UiState.Error) {
                    item {
                        ErrorStudioBanner(message = (myApplicationsState as UiState.Error).message)
                    }
                }

                if (historyTab == ProfileHistoryTab.POSTED && projectsState is UiState.Error) {
                    item {
                        ErrorStudioBanner(message = (projectsState as UiState.Error).message)
                    }
                }

                if (historyTab == ProfileHistoryTab.APPLIED) {
                    if (appliedHistory.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Applied Projects Yet",
                                subtitle = "Pitch your AI skills and portfolio card on paid briefs to track every application here.",
                                icon = Icons.Filled.WorkOutline,
                                actionLabel = "Browse Paid Projects",
                                onAction = onBrowseProjects
                            )
                        }
                    } else {
                        items(appliedHistory, key = { it.projectId }) { item ->
                            AppliedProjectHistoryCard(
                                item = item,
                                onOpenThread = { onNavigateToThread(item.conversationId) }
                            )
                        }
                    }
                } else {
                    if (myPostedProjects.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Posted Projects Yet",
                                subtitle = "Post a paid AI brief from the Projects tab to hire creators from India's tech hubs.",
                                icon = Icons.Filled.WorkOutline,
                                actionLabel = "Browse Projects",
                                onAction = onBrowseProjects
                            )
                        }
                    } else {
                        items(myPostedProjects, key = { it.id }) { proj ->
                            PostedProjectHistoryCard(
                                project = proj,
                                onAdvanceStatus = { status ->
                                    viewModel.advanceProjectStatus(proj.id, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileIdentityHeroCard(
    displayName: String,
    email: String,
    photoUrl: String?,
    cityHub: String,
    currentMode: UserMode,
    skillsCount: Int,
    appliedCount: Int,
    postedCount: Int,
    onSwitchMode: (UserMode) -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(StudioSurfaceElevated, StudioSurface)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "$displayName avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initials = displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "AI" }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SaffronAmberDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = SaffronAmberLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HubTag(hubName = cityHub)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = WarmCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out", color = WarmCoral)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onSwitchMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("profile_mode_${mode.wireName.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatTile(
                    label = "Skills",
                    value = skillsCount.toString(),
                    accentColor = ElectricTealLight,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatTile(
                    label = "Applied",
                    value = appliedCount.toString(),
                    accentColor = SaffronAmberLight,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatTile(
                    label = "Posted",
                    value = postedCount.toString(),
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun HistoryTabButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    containerColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) containerColor else StudioSurfaceElevated,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else StudioBorder
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) textColor else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppliedProjectHistoryCard(
    item: AppliedProjectHistoryItem,
    onOpenThread: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = StudioSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isSelectedCreator) ElectricTeal else StudioBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("applied_project_card_${item.projectId}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.liveProject != null) {
                        StatusPill(status = item.liveProject.statusEnum())
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SaffronAmberDark,
                            border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "APPLIED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaffronAmberLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (item.isSelectedCreator) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ElectricTealDark,
                            border = BorderStroke(1.dp, ElectricTeal)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricTealLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Selected!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricTealLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = item.budgetLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = SaffronAmberLight
                )
            }

            Text(
                text = item.projectTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Client: ${item.clientName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            HubTag(
                hubName = item.cityHub,
                remoteAllowed = item.liveProject?.remoteAllowed == true
            )

            if (item.liveProject != null) {
                SkillBadgeCluster(skills = item.liveProject.skillEnums())
            }

            if (item.attachedPortfolio) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricTealDark.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = ElectricTealLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Portfolio Card Attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTealLight
                        )
                    }
                }
            }

            if (item.pitchText.isNotBlank()) {
                Text(
                    text = "Pitch: ${item.pitchText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onOpenThread,
                border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.55f)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("open_applied_thread_${item.projectId}")
            ) {
                Text("Open DM Thread", color = SaffronAmberLight)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SaffronAmberLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PostedProjectHistoryCard(
    project: PaidProject,
    onAdvanceStatus: (ProjectStatus) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("posted_project_card_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status = project.statusEnum())
                Text(
                    text = "${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SaffronAmberLight
                )
            }
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            HubTag(hubName = project.cityHub, remoteAllowed = project.remoteAllowed)
            SkillBadgeCluster(skills = project.skillEnums())
            if (!project.selectedCreatorName.isNullOrBlank()) {
                Text(
                    text = "Assigned to: ${project.selectedCreatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricTealLight
                )
            }
            if (project.statusEnum() != ProjectStatus.COMPLETED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (project.statusEnum() == ProjectStatus.OPEN) {
                        OutlinedButton(
                            onClick = { onAdvanceStatus(ProjectStatus.SHORTLISTING) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Mark Shortlisting")
                        }
                    }
                    OutlinedButton(
                        onClick = { onAdvanceStatus(ProjectStatus.COMPLETED) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Mark Completed")
                    }
                }
            }
        }
    }
}
