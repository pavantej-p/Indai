package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SkillCategory(
    val wireName: String,
    val displayName: String,
    val shortLabel: String,
    val summary: String
) {
    AI_VIDEO(
        wireName = "AI_VIDEO",
        displayName = "AI Video Creation",
        shortLabel = "AI Video",
        summary = "Runway, Sora, Kling, HeyGen & commercial reels"
    ),
    AI_IMAGE(
        wireName = "AI_IMAGE",
        displayName = "AI Image Generation",
        shortLabel = "AI Image",
        summary = "Midjourney, Flux, ComfyUI & brand key visuals"
    ),
    WEB_DEV(
        wireName = "WEB_DEV",
        displayName = "Website Development",
        shortLabel = "Web Dev",
        summary = "Next.js, React, Webflow & AI-native web apps"
    ),
    CHATBOT_DEV(
        wireName = "CHATBOT_DEV",
        displayName = "Chatbot Development",
        shortLabel = "Chatbots",
        summary = "WhatsApp bots, RAG agents & voice assistants"
    ),
    AUTOMATION(
        wireName = "AUTOMATION",
        displayName = "Automation",
        shortLabel = "Automation",
        summary = "n8n, Make, Zapier & custom AI workflow pipelines"
    );

    companion object {
        fun fromWire(raw: String?): SkillCategory =
            entries.firstOrNull { it.wireName == raw } ?: AUTOMATION
    }
}

enum class ProjectStatus(val wireName: String, val label: String) {
    OPEN("OPEN", "Open"),
    SHORTLISTING("SHORTLISTING", "Shortlisting"),
    ASSIGNED("ASSIGNED", "Assigned"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromWire(raw: String?): ProjectStatus =
            entries.firstOrNull { it.wireName == raw } ?: OPEN
    }
}

enum class UserMode(val wireName: String, val label: String) {
    CREATOR("CREATOR", "Creator Mode"),
    CLIENT("CLIENT", "Client Mode");

    companion object {
        fun fromWire(raw: String?): UserMode =
            entries.firstOrNull { it.wireName == raw } ?: CREATOR
    }
}

enum class MessageType(val wireName: String) {
    TEXT("TEXT"),
    PORTFOLIO_PITCH("PORTFOLIO_PITCH");

    companion object {
        fun fromWire(raw: String?): MessageType =
            entries.firstOrNull { it.wireName == raw } ?: TEXT
    }
}

data class IndiaTechHub(
    val name: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val highlight: String
)

val INDIA_TECH_HUBS = listOf(
    IndiaTechHub("Bengaluru", "Karnataka", 12.9716, 77.5946, "AI Agents, SaaS & Automation Core"),
    IndiaTechHub("Mumbai", "Maharashtra", 19.0760, 72.8777, "Ad Films, AI Video & Brand Studios"),
    IndiaTechHub("Delhi NCR", "Delhi / Gurugram / Noida", 28.6139, 77.2090, "D2C Growth, Chatbots & Web Systems"),
    IndiaTechHub("Hyderabad", "Telangana", 17.3850, 78.4867, "GenAI Engineering & Cloud Pipelines"),
    IndiaTechHub("Pune", "Maharashtra", 18.5204, 73.8567, "Full-Stack Web & Product Studios"),
    IndiaTechHub("Chennai", "Tamil Nadu", 13.0827, 80.2707, "B2B SaaS, Voice AI & Enterprise Bots"),
    IndiaTechHub("Kolkata", "West Bengal", 22.5726, 88.3639, "Creative AI Art & Design Collectives"),
    IndiaTechHub("Ahmedabad", "Gujarat", 23.0225, 72.5714, "E-Commerce Automation & Media"),
    IndiaTechHub("Kochi", "Kerala", 9.9312, 76.2673, "Indie Web Studios & AI Media"),
    IndiaTechHub("Jaipur", "Rajasthan", 26.9124, 75.7873, "Design Systems & Visual Storytelling")
)

fun findIndiaHub(name: String): IndiaTechHub =
    INDIA_TECH_HUBS.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: INDIA_TECH_HUBS.first()

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredMode: String = UserMode.CREATOR.wireName,
    val cityHub: String = INDIA_TECH_HUBS.first().name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun modeEnum(): UserMode = UserMode.fromWire(preferredMode)
}

@Keep
@IgnoreExtraProperties
data class CreatorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val headline: String = "",
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val cityHub: String = INDIA_TECH_HUBS.first().name,
    val latitude: Double = INDIA_TECH_HUBS.first().latitude,
    val longitude: Double = INDIA_TECH_HUBS.first().longitude,
    val hourlyRateInr: Long = 1500L,
    val portfolioUrl: String? = null,
    val sampleWorkTitle: String? = null,
    val availableForPaidWork: Boolean = true,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun skillEnums(): List<SkillCategory> = skills.map { SkillCategory.fromWire(it) }.distinct()
}

@Keep
@IgnoreExtraProperties
data class PaidProject(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val title: String = "",
    val description: String = "",
    val requiredSkills: List<String> = emptyList(),
    val budgetMinInr: Long = 15000L,
    val budgetMaxInr: Long = 45000L,
    val cityHub: String = INDIA_TECH_HUBS.first().name,
    val latitude: Double = INDIA_TECH_HUBS.first().latitude,
    val longitude: Double = INDIA_TECH_HUBS.first().longitude,
    val remoteAllowed: Boolean = true,
    val status: String = ProjectStatus.OPEN.wireName,
    val selectedCreatorId: String? = null,
    val selectedCreatorName: String? = null,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun skillEnums(): List<SkillCategory> = requiredSkills.map { SkillCategory.fromWire(it) }.distinct()
    fun statusEnum(): ProjectStatus = ProjectStatus.fromWire(status)
    fun isAvailable(): Boolean =
        statusEnum() == ProjectStatus.OPEN || statusEnum() == ProjectStatus.SHORTLISTING
}

fun filterProjects(
    projects: List<PaidProject>,
    selectedSkill: SkillCategory?,
    selectedHub: String?,
    remoteOnly: Boolean,
    availableOnly: Boolean,
    searchQuery: String
): List<PaidProject> {
    val q = searchQuery.trim().lowercase()
    return projects.filter { project ->
        val matchesAvailability = !availableOnly || project.isAvailable()
        val matchesSkill = selectedSkill == null || selectedSkill.wireName in project.requiredSkills
        val matchesHub = selectedHub == null || project.cityHub.equals(selectedHub, ignoreCase = true)
        val matchesRemote = !remoteOnly || project.remoteAllowed
        val matchesQuery = q.isEmpty() ||
            project.title.lowercase().contains(q) ||
            project.description.lowercase().contains(q) ||
            project.cityHub.lowercase().contains(q) ||
            project.clientName.lowercase().contains(q) ||
            project.skillEnums().any {
                it.displayName.lowercase().contains(q) || it.shortLabel.lowercase().contains(q)
            }
        matchesAvailability && matchesSkill && matchesHub && matchesRemote && matchesQuery
    }
}

fun countProjectsByCategory(
    projects: List<PaidProject>,
    availableOnly: Boolean
): Map<SkillCategory, Int> {
    val scoped = if (availableOnly) projects.filter { it.isAvailable() } else projects
    return SkillCategory.entries.associateWith { skill ->
        scoped.count { skill.wireName in it.requiredSkills }
    }
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val creatorId: String = "",
    val creatorName: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val projectId: String? = null,
    val projectTitle: String? = null,
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = MessageType.TEXT.wireName,
    val portfolioHeadline: String? = null,
    val portfolioCityHub: String? = null,
    val portfolioSkills: List<String>? = null,
    val portfolioRateInr: Long? = null,
    val portfolioUrl: String? = null,
    val portfolioSampleTitle: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun typeEnum(): MessageType = MessageType.fromWire(messageType)
    fun portfolioSkillEnums(): List<SkillCategory> =
        portfolioSkills?.map { SkillCategory.fromWire(it) }?.distinct().orEmpty()
}

@Keep
@IgnoreExtraProperties
data class ProjectApplication(
    @DocumentId val id: String = "",
    val applicantId: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val conversationId: String = "",
    val pitchText: String = "",
    val attachedPortfolio: Boolean = false,
    val budgetLabel: String = "",
    val cityHub: String = INDIA_TECH_HUBS.first().name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AppliedProjectHistoryItem(
    val projectId: String,
    val projectTitle: String,
    val clientName: String,
    val conversationId: String,
    val pitchText: String,
    val attachedPortfolio: Boolean,
    val budgetLabel: String,
    val cityHub: String,
    val liveProject: PaidProject?,
    val isSelectedCreator: Boolean,
    val sortTimestampSeconds: Long
)

fun mergeAppliedProjectHistory(
    currentUserId: String,
    applications: List<ProjectApplication>,
    conversations: List<Conversation>,
    publicProjects: List<PaidProject>
): List<AppliedProjectHistoryItem> {
    val projectsById = publicProjects.associateBy { it.id }
    val resultByProjectId = linkedMapOf<String, AppliedProjectHistoryItem>()

    applications.forEach { app ->
        val pid = app.projectId.ifBlank { app.id }
        if (pid.isNotBlank()) {
            val live = projectsById[pid]
            val ts = (app.updatedAt ?: app.createdAt)?.seconds ?: 0L
            resultByProjectId[pid] = AppliedProjectHistoryItem(
                projectId = pid,
                projectTitle = live?.title ?: app.projectTitle,
                clientName = live?.clientName ?: app.clientName,
                conversationId = app.conversationId,
                pitchText = app.pitchText,
                attachedPortfolio = app.attachedPortfolio,
                budgetLabel = live?.let { "${formatInr(it.budgetMinInr)} – ${formatInr(it.budgetMaxInr)}" }
                    ?: app.budgetLabel,
                cityHub = live?.cityHub ?: app.cityHub,
                liveProject = live,
                isSelectedCreator = live?.selectedCreatorId == currentUserId,
                sortTimestampSeconds = ts
            )
        }
    }

    conversations.forEach { conv ->
        val pid = conv.projectId?.takeIf { it.isNotBlank() } ?: return@forEach
        if (conv.creatorId != currentUserId) return@forEach
        if (pid in resultByProjectId) return@forEach
        val live = projectsById[pid]
        if (live?.clientId == currentUserId) return@forEach
        val ts = (conv.updatedAt ?: conv.createdAt)?.seconds ?: 0L
        resultByProjectId[pid] = AppliedProjectHistoryItem(
            projectId = pid,
            projectTitle = live?.title ?: conv.projectTitle ?: "AI Project",
            clientName = live?.clientName ?: conv.clientName,
            conversationId = conv.id,
            pitchText = conv.lastMessage,
            attachedPortfolio = false,
            budgetLabel = live?.let { "${formatInr(it.budgetMinInr)} – ${formatInr(it.budgetMaxInr)}" } ?: "Paid Project",
            cityHub = live?.cityHub ?: INDIA_TECH_HUBS.first().name,
            liveProject = live,
            isSelectedCreator = live?.selectedCreatorId == currentUserId,
            sortTimestampSeconds = ts
        )
    }

    return resultByProjectId.values.sortedByDescending { it.sortTimestampSeconds }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun formatInr(amount: Long): String {
    val str = amount.toString()
    if (str.length <= 3) return "₹$str"
    val lastThree = str.takeLast(3)
    val rest = str.dropLast(3)
    val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
    return "₹$grouped,$lastThree"
}
