package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.UserMode
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElectricTealDark
import com.example.ui.theme.ElectricTealLight
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SaffronAmberDark
import com.example.ui.theme.SaffronAmberLight
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmCoral
import com.example.ui.theme.WarmCoralDark

fun SkillCategory.icon(): ImageVector = when (this) {
    SkillCategory.AI_VIDEO -> Icons.Filled.Videocam
    SkillCategory.AI_IMAGE -> Icons.Filled.Image
    SkillCategory.WEB_DEV -> Icons.Filled.Language
    SkillCategory.CHATBOT_DEV -> Icons.Filled.SmartToy
    SkillCategory.AUTOMATION -> Icons.Filled.Bolt
}

fun SkillCategory.accentColor(): Color = when (this) {
    SkillCategory.AI_VIDEO -> SaffronAmber
    SkillCategory.AI_IMAGE -> WarmCoral
    SkillCategory.WEB_DEV -> ElectricTeal
    SkillCategory.CHATBOT_DEV -> SaffronAmberLight
    SkillCategory.AUTOMATION -> ElectricTealLight
}

@Composable
fun SkillFilterRow(
    selectedSkill: SkillCategory?,
    onSelectSkill: (SkillCategory?) -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Int? = null,
    categoryCounts: Map<SkillCategory, Int>? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val allLabel = if (totalCount != null) "All ($totalCount)" else "All Skills"
            FilterChip(
                selected = selectedSkill == null,
                onClick = { onSelectSkill(null) },
                label = { Text(allLabel) },
                modifier = Modifier.testTag("filter_skill_all"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaffronAmberDark,
                    selectedLabelColor = SaffronAmberLight,
                    containerColor = StudioSurfaceElevated,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedSkill == null,
                    borderColor = StudioBorder,
                    selectedBorderColor = SaffronAmber
                )
            )
        }
        items(SkillCategory.entries) { skill ->
            val selected = selectedSkill == skill
            val count = categoryCounts?.get(skill)
            val labelText = if (count != null) "${skill.shortLabel} ($count)" else skill.shortLabel
            FilterChip(
                selected = selected,
                onClick = { onSelectSkill(skill) },
                leadingIcon = {
                    Icon(
                        imageVector = skill.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(labelText) },
                modifier = Modifier.testTag("filter_skill_${skill.wireName.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricTealDark,
                    selectedLabelColor = ElectricTealLight,
                    selectedLeadingIconColor = ElectricTealLight,
                    containerColor = StudioSurfaceElevated,
                    labelColor = TextSecondary,
                    iconColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = StudioBorder,
                    selectedBorderColor = ElectricTeal
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillBadgeCluster(
    skills: List<SkillCategory>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        skills.forEach { skill ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = skill.accentColor().copy(alpha = 0.14f),
                border = BorderStroke(1.dp, skill.accentColor().copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = skill.icon(),
                        contentDescription = null,
                        tint = skill.accentColor(),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = skill.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = skill.accentColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: ProjectStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        ProjectStatus.OPEN -> ElectricTealDark to ElectricTealLight
        ProjectStatus.SHORTLISTING -> SaffronAmberDark to SaffronAmberLight
        ProjectStatus.ASSIGNED -> WarmCoralDark to WarmCoral
        ProjectStatus.COMPLETED -> StudioSurfaceElevated to TextMuted
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HubTag(hubName: String, remoteAllowed: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = SaffronAmber,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (remoteAllowed) "$hubName • Remote OK" else hubName,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyStudioCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Filled.AutoAwesome,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SaffronAmberDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SaffronAmberLight,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAmber)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorStudioBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = WarmCoralDark,
        border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = WarmCoral
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PortfolioSnapshotCard(
    displayName: String,
    headline: String,
    cityHub: String,
    hourlyRateInr: Long,
    skills: List<SkillCategory>,
    sampleWorkTitle: String?,
    portfolioUrl: String?,
    onOpenUrl: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PORTFOLIO CARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricTealLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "₹$hourlyRateInr/hr",
                    style = MaterialTheme.typography.labelLarge,
                    color = SaffronAmberLight
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            SkillBadgeCluster(skills = skills)
            Spacer(Modifier.height(8.dp))
            HubTag(hubName = cityHub)
            if (!sampleWorkTitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Featured: $sampleWorkTitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = SaffronAmberLight
                )
            }
            if (!portfolioUrl.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = portfolioUrl,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricTealLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = onOpenUrl != null) { onOpenUrl?.invoke(portfolioUrl) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RoleModeSelector(
    selectedMode: UserMode,
    onSelectMode: (UserMode) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoleModeOptionCard(
            title = "Creator",
            subtitle = "Pitch AI portfolio & claim paid gigs",
            icon = Icons.Filled.AutoAwesome,
            selected = selectedMode == UserMode.CREATOR,
            accentColor = SaffronAmber,
            containerColor = SaffronAmberDark,
            labelColor = SaffronAmberLight,
            onClick = { onSelectMode(UserMode.CREATOR) },
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_creator")
        )
        RoleModeOptionCard(
            title = "Client",
            subtitle = "Post paid projects & hire AI talent",
            icon = Icons.Filled.WorkOutline,
            selected = selectedMode == UserMode.CLIENT,
            accentColor = ElectricTeal,
            containerColor = ElectricTealDark,
            labelColor = ElectricTealLight,
            onClick = { onSelectMode(UserMode.CLIENT) },
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_client")
        )
    }
}

@Composable
private fun RoleModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    containerColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) containerColor.copy(alpha = 0.7f) else StudioSurfaceElevated,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else StudioBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) labelColor else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "$title selected",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) labelColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f) else TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
