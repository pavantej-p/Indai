package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CreatorProfile
import com.example.data.model.UiState
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.components.HubTag
import com.example.ui.components.PortfolioSnapshotCard
import com.example.ui.components.SkillBadgeCluster
import com.example.ui.components.SkillFilterRow
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElectricTealDark
import com.example.ui.theme.ElectricTealLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SaffronAmberLight
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorsScreen(
    viewModel: GuildViewModel,
    onNavigateToThread: (String) -> Unit,
    onNavigateToStudio: () -> Unit
) {
    val creatorsState by viewModel.publicCreators.collectAsStateWithLifecycle()
    val selectedSkill by viewModel.selectedSkill.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var selectedCreator by remember { mutableStateOf<CreatorProfile?>(null) }

    Scaffold(containerColor = ObsidianBg) { innerPadding ->
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
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Indian AI Creators",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Verified portfolios across AI Video, Image, Web, Chatbots & Automation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                            },
                            placeholder = { Text("Search creators, tools, or hubs...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("creators_search_input")
                        )
                    }
                }

                item {
                    SkillFilterRow(
                        selectedSkill = selectedSkill,
                        onSelectSkill = viewModel::selectSkillFilter
                    )
                }

                when (val state = creatorsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricTeal)
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
                        val q = searchQuery.trim().lowercase()
                        val filtered = state.data.filter { creator ->
                            val matchesSkill = selectedSkill == null || selectedSkill!!.wireName in creator.skills
                            val matchesQuery = q.isEmpty() ||
                                creator.displayName.lowercase().contains(q) ||
                                creator.headline.lowercase().contains(q) ||
                                creator.cityHub.lowercase().contains(q) ||
                                (creator.bio?.lowercase()?.contains(q) == true)
                            matchesSkill && matchesQuery
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyStudioCard(
                                    title = "No creator portfolios found",
                                    subtitle = "Publish your own AI Creator Portfolio in Studio so clients and fellow creators across India can discover and DM you.",
                                    icon = Icons.Filled.Groups,
                                    actionLabel = "Create My Portfolio",
                                    onAction = onNavigateToStudio,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { creator ->
                                CreatorDirectoryCard(
                                    creator = creator,
                                    isMine = creator.userId == viewModel.currentUserId,
                                    onClick = { selectedCreator = creator },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCreator?.let { creator ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var introMsg by remember(creator.userId) {
            mutableStateOf("Hi ${creator.displayName}, we have an AI project opportunity and loved your portfolio!")
        }
        ModalBottomSheet(
            onDismissRequest = { selectedCreator = null },
            sheetState = sheetState,
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PortfolioSnapshotCard(
                    displayName = creator.displayName,
                    headline = creator.headline,
                    cityHub = creator.cityHub,
                    hourlyRateInr = creator.hourlyRateInr,
                    skills = creator.skillEnums(),
                    sampleWorkTitle = creator.sampleWorkTitle,
                    portfolioUrl = creator.portfolioUrl,
                    onOpenUrl = { rawUrl ->
                        val normalized = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
                        runCatching { uriHandler.openUri(normalized) }
                    }
                )
                if (!creator.bio.isNullOrBlank()) {
                    Text(
                        text = creator.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
                if (!creator.portfolioUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val url = creator.portfolioUrl
                            val normalized = if (url.startsWith("http")) url else "https://$url"
                            runCatching { uriHandler.openUri(normalized) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open External Portfolio")
                    }
                }

                if (creator.userId == viewModel.currentUserId) {
                    Button(
                        onClick = {
                            selectedCreator = null
                            onNavigateToStudio()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit My Portfolio in Studio", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = introMsg,
                        onValueChange = { introMsg = it },
                        label = { Text("Direct Message") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.startDmWithPortfolio(
                                partnerUid = creator.userId,
                                partnerName = creator.displayName,
                                project = null,
                                introText = introMsg,
                                attachPortfolio = false,
                                onThreadReady = { convId ->
                                    selectedCreator = null
                                    onNavigateToThread(convId)
                                }
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricTeal,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("message_creator_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Message ${creator.displayName}", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CreatorDirectoryCard(
    creator: CreatorProfile,
    isMine: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("creator_card_${creator.userId}"),
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, if (isMine) ElectricTeal.copy(alpha = 0.55f) else StudioBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = creator.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (creator.availableForPaidWork) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ElectricTealDark
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                                    text = "Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricTealLight
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "₹${creator.hourlyRateInr}/hr",
                    style = MaterialTheme.typography.titleSmall,
                    color = SaffronAmberLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = creator.headline,
                style = MaterialTheme.typography.titleSmall,
                color = ElectricTealLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!creator.bio.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = creator.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            SkillBadgeCluster(skills = creator.skillEnums())
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HubTag(hubName = creator.cityHub)
                Text(
                    text = if (isMine) "Your Profile" else "View Portfolio & DM →",
                    style = MaterialTheme.typography.labelLarge,
                    color = SaffronAmberLight
                )
            }
        }
    }
}
