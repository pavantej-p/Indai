package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageType
import com.example.data.model.ProjectStatus
import com.example.data.model.UiState
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.components.PortfolioSnapshotCard
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
import com.example.ui.viewmodel.GuildViewModel

@Composable
fun InboxScreen(
    viewModel: GuildViewModel,
    onOpenConversation: (String) -> Unit,
    onExploreProjects: () -> Unit
) {
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()

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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Portfolio DM Inbox",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Direct messages, portfolio pitches & paid project selections",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                when (val state = conversationsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SaffronAmber)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item { ErrorStudioBanner(message = state.message) }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                EmptyStudioCard(
                                    title = "Your DM inbox is quiet",
                                    subtitle = "Browse Paid Projects and tap 'DM Client with Portfolio' or message any AI creator from the directory.",
                                    icon = Icons.Filled.Forum,
                                    actionLabel = "Browse Paid Projects",
                                    onAction = onExploreProjects
                                )
                            }
                        } else {
                            items(state.data, key = { it.id }) { conv ->
                                val isCreatorSide = conv.creatorId == viewModel.currentUserId
                                val partnerName = if (isCreatorSide) conv.clientName else conv.creatorName
                                ConversationRowCard(
                                    partnerName = partnerName,
                                    projectTitle = conv.projectTitle,
                                    lastMessage = conv.lastMessage,
                                    onClick = { onOpenConversation(conv.id) },
                                    modifier = Modifier.testTag("conversation_row_${conv.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRowCard(
    partnerName: String,
    projectTitle: String?,
    lastMessage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SaffronAmberDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partnerName.firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleLarge,
                    color = SaffronAmberLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!projectTitle.isNullOrBlank()) {
                    Text(
                        text = "Project: $projectTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricTealLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversationId: String,
    viewModel: GuildViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(conversationId) {
        viewModel.openThread(conversationId)
    }

    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessages.collectAsStateWithLifecycle()
    val projectsState by viewModel.publicProjects.collectAsStateWithLifecycle()
    val myPortfolioState by viewModel.myCreatorProfile.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    val conversation = (conversationsState as? UiState.Success)?.data
        ?.firstOrNull { it.id == conversationId }
    val myPortfolio = (myPortfolioState as? UiState.Success)?.data
    val linkedProject = conversation?.projectId?.let { pid ->
        (projectsState as? UiState.Success)?.data?.firstOrNull { it.id == pid }
    }

    val partnerUid = conversation?.participantIds?.firstOrNull { it != viewModel.currentUserId }.orEmpty()
    val partnerName = if (conversation != null) {
        if (conversation.creatorId == viewModel.currentUserId) conversation.clientName else conversation.creatorName
    } else "Conversation"

    var messageText by remember { mutableStateOf("") }
    var attachPortfolio by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val currentMessages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.lastIndex)
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("thread_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Inbox")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = partnerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!conversation?.projectTitle.isNullOrBlank()) {
                            Text(
                                text = conversation?.projectTitle.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricTealLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // If current user owns the linked project, offer 1-tap freelancer selection!
            if (linkedProject != null &&
                linkedProject.clientId == viewModel.currentUserId &&
                linkedProject.statusEnum() != ProjectStatus.COMPLETED &&
                partnerUid.isNotBlank()
            ) {
                val alreadySelected = linkedProject.selectedCreatorId == partnerUid
                Surface(
                    color = if (alreadySelected) ElectricTealDark else SaffronAmberDark.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (alreadySelected) "Selected for Paid Project" else "Ready to hire for ${linkedProject.title}?",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (alreadySelected) ElectricTealLight else SaffronAmberLight
                            )
                            Text(
                                text = if (alreadySelected) "$partnerName is assigned to this gig." else "Share project kickoff & mark Assigned.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (!alreadySelected) {
                            Button(
                                onClick = {
                                    viewModel.selectFreelancerForProject(
                                        projectId = linkedProject.id,
                                        creatorId = partnerUid,
                                        creatorName = partnerName
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber),
                                modifier = Modifier.testTag("select_freelancer_button")
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Select")
                            }
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = ElectricTealLight)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = messagesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SaffronAmber)
                        }
                    }
                    is UiState.Error -> {
                        ErrorStudioBanner(
                            message = state.message,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    is UiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data, key = { it.id }) { msg ->
                                ChatBubble(
                                    message = msg,
                                    isMine = msg.senderId == viewModel.currentUserId,
                                    onOpenUrl = { rawUrl ->
                                        val normalized = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
                                        runCatching { uriHandler.openUri(normalized) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Composer
            Surface(
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = attachPortfolio,
                            onClick = { attachPortfolio = !attachPortfolio },
                            leadingIcon = {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    if (myPortfolio != null) "Attach Portfolio Card"
                                    else "Setup Portfolio in Studio to Attach"
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricTealDark,
                                selectedLabelColor = ElectricTealLight,
                                selectedLeadingIconColor = ElectricTealLight
                            ),
                            modifier = Modifier.testTag("attach_portfolio_chip")
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Write a DM or pitch your work...") },
                            maxLines = 4,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("thread_message_input")
                        )
                        Button(
                            onClick = {
                                val conv = conversation ?: return@Button
                                viewModel.sendThreadMessage(
                                    conversation = conv,
                                    text = messageText,
                                    attachPortfolio = attachPortfolio,
                                    onSent = {
                                        messageText = ""
                                        attachPortfolio = false
                                    }
                                )
                            },
                            enabled = conversation != null && !isSubmitting && (messageText.isNotBlank() || attachPortfolio),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send DM")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isMine) "You" else message.senderName,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) SaffronAmberDark else StudioSurfaceElevated,
            border = BorderStroke(
                1.dp,
                if (isMine) SaffronAmber.copy(alpha = 0.45f) else StudioBorder
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (message.typeEnum() == MessageType.PORTFOLIO_PITCH && !message.portfolioHeadline.isNullOrBlank()) {
                    PortfolioSnapshotCard(
                        displayName = message.senderName,
                        headline = message.portfolioHeadline,
                        cityHub = message.portfolioCityHub ?: "India",
                        hourlyRateInr = message.portfolioRateInr ?: 0L,
                        skills = message.portfolioSkillEnums(),
                        sampleWorkTitle = message.portfolioSampleTitle,
                        portfolioUrl = message.portfolioUrl,
                        onOpenUrl = onOpenUrl
                    )
                }
            }
        }
    }
}
