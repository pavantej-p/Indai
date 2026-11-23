package com.example

import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.findIndiaHub
import com.example.data.model.formatInr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatInr_formatsIndianNumberingSystem() {
        assertEquals("₹500", formatInr(500L))
        assertEquals("₹1,500", formatInr(1500L))
        assertEquals("₹25,000", formatInr(25000L))
        assertEquals("₹1,50,000", formatInr(150000L))
    }

    @Test
    fun skillAndHubMappings_resolveSafely() {
        assertEquals(SkillCategory.AI_VIDEO, SkillCategory.fromWire("AI_VIDEO"))
        assertEquals(SkillCategory.AUTOMATION, SkillCategory.fromWire("UNKNOWN_SKILL"))
        assertEquals(ProjectStatus.OPEN, ProjectStatus.fromWire("LEGACY_STATUS"))
        assertTrue(INDIA_TECH_HUBS.size >= 10)
        assertEquals("Bengaluru", findIndiaHub("bengaluru").name)
    }

    @Test
    fun darkStudioThemePalette_matchesDefinedHexValues() {
        assertEquals(0xFF121212L, com.example.ui.theme.StudioDarkColorScheme.background.value.toLong() ushr 32)
        assertEquals(0xFFFFBF00L, com.example.ui.theme.StudioDarkColorScheme.primary.value.toLong() ushr 32)
        assertEquals(0xFF008080L, com.example.ui.theme.StudioDarkColorScheme.secondary.value.toLong() ushr 32)
    }

    @Test
    fun userMode_fromWire_resolvesClientAndCreator() {
        assertEquals(com.example.data.model.UserMode.CLIENT, com.example.data.model.UserMode.fromWire("CLIENT"))
        assertEquals(com.example.data.model.UserMode.CREATOR, com.example.data.model.UserMode.fromWire("CREATOR"))
        assertEquals(com.example.data.model.UserMode.CREATOR, com.example.data.model.UserMode.fromWire(null))
    }

    @Test
    fun filterProjects_andCategoryCounts_filterBySkillAndAvailability() {
        val videoProject = com.example.data.model.PaidProject(
            id = "p1",
            clientName = "Studio A",
            title = "AI Commercial Reel",
            description = "Runway Gen-3 commercial video ad",
            requiredSkills = listOf("AI_VIDEO"),
            cityHub = "Mumbai",
            remoteAllowed = true,
            status = "OPEN"
        )
        val imageCompleted = com.example.data.model.PaidProject(
            id = "p2",
            clientName = "Studio B",
            title = "Brand Key Visuals",
            description = "Midjourney product renders",
            requiredSkills = listOf("AI_IMAGE"),
            cityHub = "Bengaluru",
            remoteAllowed = false,
            status = "COMPLETED"
        )
        val devProject = com.example.data.model.PaidProject(
            id = "p3",
            clientName = "Studio C",
            title = "Next.js AI Portal",
            description = "Website development with AI chat",
            requiredSkills = listOf("WEB_DEV", "CHATBOT_DEV"),
            cityHub = "Bengaluru",
            remoteAllowed = true,
            status = "SHORTLISTING"
        )
        val all = listOf(videoProject, imageCompleted, devProject)

        val availableOnlyCounts = com.example.data.model.countProjectsByCategory(all, availableOnly = true)
        assertEquals(1, availableOnlyCounts[SkillCategory.AI_VIDEO])
        assertEquals(0, availableOnlyCounts[SkillCategory.AI_IMAGE])
        assertEquals(1, availableOnlyCounts[SkillCategory.WEB_DEV])
        assertEquals(1, availableOnlyCounts[SkillCategory.CHATBOT_DEV])

        val filteredVideo = com.example.data.model.filterProjects(
            projects = all,
            selectedSkill = SkillCategory.AI_VIDEO,
            selectedHub = null,
            remoteOnly = false,
            availableOnly = true,
            searchQuery = ""
        )
        assertEquals(listOf("p1"), filteredVideo.map { it.id })

        val filteredAllStatusImage = com.example.data.model.filterProjects(
            projects = all,
            selectedSkill = SkillCategory.AI_IMAGE,
            selectedHub = null,
            remoteOnly = false,
            availableOnly = false,
            searchQuery = ""
        )
        assertEquals(listOf("p2"), filteredAllStatusImage.map { it.id })
    }

    @Test
    fun mergeAppliedProjectHistory_mergesAndDeduplicatesApplications() {
        val uid = "creator_1"
        val liveOpen = com.example.data.model.PaidProject(
            id = "proj_1",
            clientId = "client_1",
            clientName = "Studio Alpha",
            title = "AI Commercial Spot",
            requiredSkills = listOf("AI_VIDEO"),
            budgetMinInr = 25000,
            budgetMaxInr = 50000,
            cityHub = "Mumbai",
            remoteAllowed = true,
            status = "ASSIGNED",
            selectedCreatorId = uid
        )
        val app1 = com.example.data.model.ProjectApplication(
            id = "proj_1",
            applicantId = uid,
            projectId = "proj_1",
            projectTitle = "AI Commercial Spot",
            clientId = "client_1",
            clientName = "Studio Alpha",
            conversationId = "client_1_creator_1",
            pitchText = "Sharing my Runway & Kling commercial reel.",
            attachedPortfolio = true,
            budgetLabel = "₹25,000 – ₹50,000",
            cityHub = "Mumbai",
            updatedAt = com.google.firebase.Timestamp(200, 0)
        )
        val legacyConvDuplicate = com.example.data.model.Conversation(
            id = "client_1_creator_1",
            participantIds = listOf("client_1", uid),
            creatorId = uid,
            clientId = "client_1",
            clientName = "Studio Alpha",
            projectId = "proj_1",
            projectTitle = "AI Commercial Spot",
            lastMessage = "Old DM text",
            updatedAt = com.google.firebase.Timestamp(100, 0)
        )
        val legacyConvOther = com.example.data.model.Conversation(
            id = "client_2_creator_1",
            participantIds = listOf("client_2", uid),
            creatorId = uid,
            clientId = "client_2",
            clientName = "Noida Bots",
            projectId = "proj_2",
            projectTitle = "WhatsApp RAG Agent",
            lastMessage = "Pitched LangGraph workflow",
            updatedAt = com.google.firebase.Timestamp(150, 0)
        )
        val clientOwnedConv = com.example.data.model.Conversation(
            id = "creator_1_other",
            participantIds = listOf(uid, "other"),
            creatorId = "other",
            clientId = uid,
            clientName = "Me",
            projectId = "proj_3",
            projectTitle = "Should Not Appear",
            lastMessage = "Hi",
            updatedAt = com.google.firebase.Timestamp(300, 0)
        )

        val merged = com.example.data.model.mergeAppliedProjectHistory(
            currentUserId = uid,
            applications = listOf(app1),
            conversations = listOf(legacyConvDuplicate, legacyConvOther, clientOwnedConv),
            publicProjects = listOf(liveOpen)
        )

        assertEquals(listOf("proj_1", "proj_2"), merged.map { it.projectId })
        assertTrue(merged[0].isSelectedCreator)
        assertTrue(merged[0].attachedPortfolio)
        assertEquals("Sharing my Runway & Kling commercial reel.", merged[0].pitchText)
        assertEquals("WhatsApp RAG Agent", merged[1].projectTitle)
    }
}
