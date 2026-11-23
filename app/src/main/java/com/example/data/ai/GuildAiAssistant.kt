package com.example.data.ai

import com.example.data.model.CreatorProfile
import com.example.data.model.PaidProject
import com.example.data.model.SkillCategory
import com.example.data.model.formatInr
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiDraftedProjectBrief(
    val title: String,
    val description: String,
    val skills: List<SkillCategory>,
    val budgetMinInr: Long,
    val budgetMaxInr: Long
)

data class AiPolishedProfile(
    val headline: String,
    val bio: String
)

class GuildAiAssistant {

    private val streamModel by lazy {
        Firebase.ai.generativeModel(modelName = DEFAULT_MODEL)
    }

    private val briefSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Concise paid freelance project title (4-80 chars)"),
                "description" to Schema.string(
                    description = "Clear project deliverables, expected AI tools, and timeline (30-500 chars)"
                ),
                "skills" to Schema.array(
                    items = Schema.enumeration(SkillCategory.entries.map { it.wireName }),
                    description = "1 to 3 matching AI skill categories"
                ),
                "budgetMinInr" to Schema.integer(description = "Suggested minimum budget in INR (between 5000 and 500000)"),
                "budgetMaxInr" to Schema.integer(description = "Suggested maximum budget in INR (>= budgetMinInr)")
            )
        )
    }

    private val briefModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = briefSchema
            }
        )
    }

    private val profileSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Punchy creator headline under 90 characters"),
                "bio" to Schema.string(description = "2-3 sentence client-ready bio highlighting AI tools and impact (under 420 chars)")
            )
        )
    }

    private val profileModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = profileSchema
            }
        )
    }

    suspend fun draftProjectBrief(
        ideaPrompt: String,
        cityHub: String
    ): AiDraftedProjectBrief {
        val cleanIdea = ideaPrompt.trim()
        require(cleanIdea.length >= 4) { "Describe your project idea in a few words first." }

        val prompt = """
            You are helping a client in $cityHub, India post a paid AI freelance gig on IndiAI Guild.
            Convert this short idea into a crisp, realistic freelance brief with fair Indian market pricing in INR:
            Idea: "$cleanIdea"
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            briefModel.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty project draft.")
        val json = JSONObject(jsonText)

        val title = json.optString("title", cleanIdea).trim().take(100)
        val description = json.optString("description", "").trim().take(1000)
        val skillsJson = json.optJSONArray("skills")
        val parsedSkills = buildList {
            if (skillsJson != null) {
                for (i in 0 until skillsJson.length()) {
                    val raw = skillsJson.optString(i)
                    SkillCategory.entries.firstOrNull { it.wireName == raw }?.let { add(it) }
                }
            }
        }.distinct().take(5).ifEmpty { listOf(SkillCategory.AUTOMATION) }

        val rawMin = json.optLong("budgetMinInr", 15000L).coerceIn(500L, 10_000_000L)
        val rawMax = json.optLong("budgetMaxInr", rawMin + 15000L).coerceIn(rawMin, 10_000_000L)

        return AiDraftedProjectBrief(
            title = if (title.length >= 4) title else cleanIdea.take(100),
            description = if (description.length >= 10) description else "Deliverables: $cleanIdea",
            skills = parsedSkills,
            budgetMinInr = rawMin,
            budgetMaxInr = rawMax
        )
    }

    fun streamProjectPitch(
        project: PaidProject,
        creatorProfile: CreatorProfile?,
        creatorName: String
    ): Flow<String> {
        val portfolioContext = if (creatorProfile != null) {
            "Creator: ${creatorProfile.displayName}, Headline: ${creatorProfile.headline}, " +
                "Skills: ${creatorProfile.skillEnums().joinToString { it.displayName }}, " +
                "Hub: ${creatorProfile.cityHub}, Sample work: ${creatorProfile.sampleWorkTitle ?: "AI Portfolio"}"
        } else {
            "Creator: $creatorName (Indian AI freelancer)"
        }

        val prompt = """
            Write a short, friendly, high-converting 3-sentence DM pitch in plain text (no markdown bullets or hashtags) for an Indian AI freelancer applying to this paid project:
            Project: ${project.title}
            Client: ${project.clientName}
            Hub: ${project.cityHub} (Remote OK: ${project.remoteAllowed})
            Budget: ${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}
            Brief: ${project.description}
            Applicant profile: $portfolioContext
            Keep it concise, confident, and ready to send directly in a chat message.
        """.trimIndent()

        return streamModel.generateContentStream(prompt).map { chunk -> chunk.text.orEmpty() }
    }

    suspend fun polishCreatorProfile(
        displayName: String,
        currentHeadline: String,
        currentBio: String,
        skills: List<SkillCategory>,
        cityHub: String,
        sampleWorkTitle: String
    ): AiPolishedProfile {
        val skillNames = skills.joinToString { it.displayName }
        val prompt = """
            Polish an Indian AI creator's freelance portfolio card for IndiAI Guild.
            Creator: $displayName in $cityHub
            AI Specialties: $skillNames
            Featured Work: ${sampleWorkTitle.ifBlank { "Client AI deliverables" }}
            Draft Headline: ${currentHeadline.ifBlank { "None yet" }}
            Draft Stack/Bio: ${currentBio.ifBlank { "None yet" }}
            Write a punchy headline (max 90 chars) and a crisp 2-sentence bio highlighting practical AI tools & client value (max 420 chars).
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            profileModel.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty profile suggestion.")
        val json = JSONObject(jsonText)
        val headline = json.optString("headline", "").trim().take(100)
        val bio = json.optString("bio", "").trim().take(500)
        if (headline.length < 4 || bio.isEmpty()) {
            throw IllegalStateException("Gemini returned an incomplete profile suggestion.")
        }
        return AiPolishedProfile(headline = headline, bio = bio)
    }

    private companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }
}
