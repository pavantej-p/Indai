package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.SkillCategory
import com.example.data.model.UserMode
import com.example.data.repository.signOutUser
import com.example.ui.components.ErrorStudioBanner
import com.example.ui.components.RoleModeSelector
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
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmCoral
import com.example.ui.viewmodel.GuildViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationSetupScreen(
    initialMode: UserMode,
    viewModel: GuildViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseUser = remember { Firebase.auth.currentUser }
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) }
    var displayName by remember(firebaseUser?.uid) {
        mutableStateOf(firebaseUser?.displayName?.trim().orEmpty())
    }
    var cityHub by remember { mutableStateOf(INDIA_TECH_HUBS.first().name) }

    var publishPortfolioNow by remember { mutableStateOf(true) }
    var headline by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("1800") }
    var sampleWorkTitle by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }
    val selectedSkills = remember { mutableStateListOf(SkillCategory.AI_VIDEO) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ElectricTealDark,
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "PROFILE REGISTRATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTealLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { signOutUser(credentialManager, scope) },
                        border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("registration_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Google account",
                            tint = WarmCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Switch Account", color = WarmCoral)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Welcome to IndiAI Guild",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Choose how you're joining India's AI network and set up your member profile. You can toggle modes anytime in Studio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Signed-in Google identity card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = SaffronAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: "Google Member",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = firebaseUser?.email ?: "Verified with Google Sign-In",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified Google account",
                            tint = ElectricTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Mode toggle card: Client vs Creator
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Your Primary Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        RoleModeSelector(
                            selectedMode = selectedMode,
                            onSelectMode = {
                                selectedMode = it
                                validationError = null
                            },
                            testTagPrefix = "reg_mode"
                        )
                    }
                }

                // Core Profile Setup
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (selectedMode == UserMode.CREATOR) "Creator Identity" else "Client / Studio Identity",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = {
                                Text(
                                    if (selectedMode == UserMode.CREATOR) "Creator or Studio Name"
                                    else "Client / Brand / Studio Name"
                                )
                            },
                            placeholder = { Text("e.g., Aarav Kapoor or Studio Veda") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_display_name_input")
                        )

                        Text(
                            text = "Primary India Tech Hub",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            INDIA_TECH_HUBS.forEach { hub ->
                                val selected = cityHub == hub.name
                                FilterChip(
                                    selected = selected,
                                    onClick = { cityHub = hub.name },
                                    label = { Text(hub.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronAmberDark,
                                        selectedLabelColor = SaffronAmberLight,
                                        containerColor = StudioSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("reg_hub_${hub.name.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }

                // Creator portfolio setup when Creator mode is active
                AnimatedVisibility(visible = selectedMode == UserMode.CREATOR) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = StudioSurface,
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f)),
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
                                        text = "Publish Creator Portfolio Now",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Attach your portfolio card when DMing paid projects.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = publishPortfolioNow,
                                    onCheckedChange = { publishPortfolioNow = it },
                                    modifier = Modifier.testTag("reg_publish_portfolio_switch")
                                )
                            }

                            if (publishPortfolioNow) {
                                OutlinedTextField(
                                    value = headline,
                                    onValueChange = { headline = it },
                                    label = { Text("Specialty Headline") },
                                    placeholder = { Text("e.g., AI Video Director & n8n Automation Builder") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_headline_input")
                                )
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = { Text("Stack & Experience (Optional)") },
                                    placeholder = { Text("Runway, Flux, ComfyUI, Next.js, WhatsApp bots...") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Select AI Specialties",
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
                                                else if (!active) selectedSkills.add(skill)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = skill.icon(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text(skill.shortLabel) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = hourlyRate,
                                    onValueChange = { hourlyRate = it.filter(Char::isDigit) },
                                    label = { Text("Hourly Rate (₹ INR)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_rate_input")
                                )
                                OutlinedTextField(
                                    value = sampleWorkTitle,
                                    onValueChange = { sampleWorkTitle = it },
                                    label = { Text("Featured Sample Work (Optional)") },
                                    placeholder = { Text("e.g., D2C Festive AI Launch Film") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = portfolioUrl,
                                    onValueChange = { portfolioUrl = it },
                                    label = { Text("Portfolio / Reel URL (Optional)") },
                                    placeholder = { Text("https://behance.net/... or GitHub") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Client overview card when Client mode is active
                AnimatedVisibility(visible = selectedMode == UserMode.CLIENT) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = ElectricTealDark.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Client Workspace Ready",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricTealLight
                            )
                            Text(
                                text = "As a Client, you can share paid AI projects in ₹ INR, browse Indian creators by tech hub and specialty, review attached portfolio cards in your DM Inbox, and assign freelancers directly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (validationError != null) {
                    ErrorStudioBanner(message = validationError!!)
                }

                Button(
                    onClick = {
                        val cleanName = displayName.trim()
                        val rate = hourlyRate.toLongOrNull() ?: 0L
                        when {
                            cleanName.length < 2 -> {
                                validationError = "Display name must be at least 2 characters."
                            }
                            selectedMode == UserMode.CREATOR && publishPortfolioNow && headline.trim().length < 4 -> {
                                validationError = "Add a specialty headline (at least 4 characters) or toggle off immediate publishing."
                            }
                            selectedMode == UserMode.CREATOR && publishPortfolioNow && rate !in 0L..500000L -> {
                                validationError = "Hourly rate must be between ₹0 and ₹5,00,000."
                            }
                            else -> {
                                validationError = null
                                viewModel.completeRegistration(
                                    displayName = cleanName,
                                    selectedMode = selectedMode,
                                    cityHubName = cityHub,
                                    publishCreatorPortfolio = selectedMode == UserMode.CREATOR && publishPortfolioNow,
                                    headline = headline,
                                    bio = bio,
                                    skills = selectedSkills.toList(),
                                    hourlyRateInr = rate,
                                    portfolioUrl = portfolioUrl,
                                    sampleWorkTitle = sampleWorkTitle
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronAmber,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("complete_registration_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (selectedMode == UserMode.CREATOR) "Enter Guild as Creator" else "Enter Guild as Client",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
