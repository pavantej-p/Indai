package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiDraftedProjectBrief
import com.example.data.ai.AiPolishedProfile
import com.example.data.ai.GuildAiAssistant
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.CreatorProfile
import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.PaidProject
import com.example.data.model.ProjectApplication
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserMode
import com.example.data.repository.GuildRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Loading : AiGenerationState
    data class Streaming(val text: String) : AiGenerationState
    data object Success : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

@OptIn(ExperimentalCoroutinesApi::class)
class GuildViewModel(
    private val repository: GuildRepository,
    val currentUserId: String,
    private val aiAssistant: GuildAiAssistant = GuildAiAssistant()
) : ViewModel() {

    private val _selectedSkill = MutableStateFlow<SkillCategory?>(null)
    val selectedSkill: StateFlow<SkillCategory?> = _selectedSkill.asStateFlow()

    private val _selectedHub = MutableStateFlow<String?>(null)
    val selectedHub: StateFlow<String?> = _selectedHub.asStateFlow()

    private val _remoteOnly = MutableStateFlow(false)
    val remoteOnly: StateFlow<Boolean> = _remoteOnly.asStateFlow()

    private val _availableOnly = MutableStateFlow(true)
    val availableOnly: StateFlow<Boolean> = _availableOnly.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _aiBriefState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiBriefState: StateFlow<AiGenerationState> = _aiBriefState.asStateFlow()

    private val _aiPitchState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiPitchState: StateFlow<AiGenerationState> = _aiPitchState.asStateFlow()

    private val _aiProfileState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiProfileState: StateFlow<AiGenerationState> = _aiProfileState.asStateFlow()

    val userAccount: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId)
            .toUiState("Failed to load user profile")

    val myCreatorProfile: StateFlow<UiState<CreatorProfile?>> =
        repository.observeMyCreatorProfile(currentUserId)
            .toUiState("Failed to load your creator portfolio")

    val publicCreators: StateFlow<UiState<List<CreatorProfile>>> =
        repository.observePublicCreators(currentUserId)
            .toUiState("Failed to load AI creators")

    val publicProjects: StateFlow<UiState<List<PaidProject>>> =
        repository.observePublicProjects(currentUserId)
            .toUiState("Failed to load paid projects")

    val conversations: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .toUiState("Failed to load DM inbox")

    val myApplications: StateFlow<UiState<List<ProjectApplication>>> =
        repository.observeMyApplications(currentUserId)
            .toUiState("Failed to load applied projects")

    val activeMessages: StateFlow<UiState<List<ChatMessage>>> =
        _activeConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(convId, currentUserId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing messages", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load messages"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun completeRegistration(
        displayName: String,
        selectedMode: UserMode,
        cityHubName: String,
        publishCreatorPortfolio: Boolean,
        headline: String,
        bio: String,
        skills: List<SkillCategory>,
        hourlyRateInr: Long,
        portfolioUrl: String,
        sampleWorkTitle: String,
        availableForPaidWork: Boolean = true,
        onCompleted: () -> Unit = {}
    ) {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser == null) {
            _events.tryEmit("Please sign in with Google first.")
            return
        }
        val cleanName = displayName.trim()
        if (cleanName.length < 2) {
            _events.tryEmit("Display name must be at least 2 characters.")
            return
        }
        if (selectedMode == UserMode.CREATOR && publishCreatorPortfolio) {
            if (headline.trim().length < 4) {
                _events.tryEmit("Headline must be at least 4 characters.")
                return
            }
            if (skills.isEmpty()) {
                _events.tryEmit("Select at least one AI specialty.")
                return
            }
            if (hourlyRateInr !in 0L..500000L) {
                _events.tryEmit("Hourly rate must be between ₹0 and ₹5,00,000.")
                return
            }
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val accountResult = repository.registerUserProfile(
                user = firebaseUser,
                displayName = cleanName,
                preferredMode = selectedMode,
                cityHubName = cityHubName
            )
            if (accountResult.isFailure) {
                _isSubmitting.value = false
                _events.tryEmit(accountResult.exceptionOrNull()?.localizedMessage ?: "Registration failed")
                return@launch
            }

            if (selectedMode == UserMode.CREATOR && publishCreatorPortfolio) {
                val portfolioResult = repository.saveCreatorProfile(
                    displayName = cleanName,
                    headline = headline,
                    bio = bio,
                    skills = skills,
                    cityHubName = cityHubName,
                    hourlyRateInr = hourlyRateInr,
                    portfolioUrl = portfolioUrl,
                    sampleWorkTitle = sampleWorkTitle,
                    availableForPaidWork = availableForPaidWork
                )
                if (portfolioResult.isFailure) {
                    _isSubmitting.value = false
                    _events.tryEmit(
                        portfolioResult.exceptionOrNull()?.localizedMessage
                            ?: "Profile saved, but portfolio publish failed"
                    )
                    return@launch
                }
            }

            _isSubmitting.value = false
            _events.tryEmit("Welcome to IndiAI Guild (${selectedMode.label})")
            onCompleted()
        }
    }

    fun selectSkillFilter(skill: SkillCategory?) {
        _selectedSkill.value = if (_selectedSkill.value == skill) null else skill
    }

    fun selectHubFilter(hubName: String?) {
        _selectedHub.value = if (_selectedHub.value == hubName) null else hubName
    }

    fun toggleRemoteOnly() {
        _remoteOnly.value = !_remoteOnly.value
    }

    fun toggleAvailableOnly() {
        _availableOnly.value = !_availableOnly.value
    }

    fun clearProjectFilters() {
        _selectedSkill.value = null
        _selectedHub.value = null
        _remoteOnly.value = false
        _availableOnly.value = true
        _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openThread(conversationId: String) {
        _activeConversationId.value = conversationId
    }

    fun switchMode(newMode: UserMode, currentHub: String) {
        viewModelScope.launch {
            val result = repository.updateUserPreferences(currentUserId, newMode, currentHub)
            result.onSuccess {
                _events.tryEmit("Switched to ${newMode.label}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not switch mode")
            }
        }
    }

    fun savePortfolio(
        displayName: String,
        headline: String,
        bio: String,
        skills: List<SkillCategory>,
        cityHubName: String,
        hourlyRateInr: Long,
        portfolioUrl: String,
        sampleWorkTitle: String,
        availableForPaidWork: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveCreatorProfile(
                displayName = displayName,
                headline = headline,
                bio = bio,
                skills = skills,
                cityHubName = cityHubName,
                hourlyRateInr = hourlyRateInr,
                portfolioUrl = portfolioUrl,
                sampleWorkTitle = sampleWorkTitle,
                availableForPaidWork = availableForPaidWork
            )
            _isSubmitting.value = false
            result.onSuccess {
                _events.tryEmit("Creator portfolio published to IndiAI Guild")
                onSaved()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not save portfolio")
            }
        }
    }

    fun createProject(
        clientName: String,
        title: String,
        description: String,
        requiredSkills: List<SkillCategory>,
        budgetMinInr: Long,
        budgetMaxInr: Long,
        cityHubName: String,
        remoteAllowed: Boolean,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createPaidProject(
                clientName = clientName,
                title = title,
                description = description,
                requiredSkills = requiredSkills,
                budgetMinInr = budgetMinInr,
                budgetMaxInr = budgetMaxInr,
                cityHubName = cityHubName,
                remoteAllowed = remoteAllowed
            )
            _isSubmitting.value = false
            result.onSuccess {
                _events.tryEmit("Paid project shared with the network")
                onCreated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not post project")
            }
        }
    }

    fun startDmWithPortfolio(
        partnerUid: String,
        partnerName: String,
        project: PaidProject?,
        introText: String,
        attachPortfolio: Boolean,
        onThreadReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName
                ?: Firebase.auth.currentUser?.displayName
                ?: "AI Creator"
            val myPortfolio = (myCreatorProfile.value as? UiState.Success)?.data
            val result = repository.openOrCreateConversationAndPitch(
                partnerUid = partnerUid,
                partnerName = partnerName,
                myDisplayName = myName,
                project = project,
                creatorProfile = myPortfolio,
                introText = introText,
                attachPortfolio = attachPortfolio
            )
            _isSubmitting.value = false
            result.onSuccess { convId ->
                _activeConversationId.value = convId
                onThreadReady(convId)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to start DM")
            }
        }
    }

    fun sendThreadMessage(
        conversation: Conversation,
        text: String,
        attachPortfolio: Boolean,
        onSent: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName
                ?: Firebase.auth.currentUser?.displayName
                ?: "AI Creator"
            val myPortfolio = if (attachPortfolio) {
                (myCreatorProfile.value as? UiState.Success)?.data
            } else null

            if (attachPortfolio && (myPortfolio == null || myPortfolio.skills.isEmpty())) {
                _events.tryEmit("Upload your skills in Profile first to attach your portfolio card.")
                return@launch
            }

            _isSubmitting.value = true
            val result = repository.sendMessage(
                conversation = conversation,
                senderName = myName,
                text = text,
                portfolioToAttach = myPortfolio
            )
            _isSubmitting.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Message failed to send")
            }
        }
    }

    fun selectFreelancerForProject(
        projectId: String,
        creatorId: String,
        creatorName: String
    ) {
        viewModelScope.launch {
            val result = repository.selectCreatorForProject(projectId, creatorId, creatorName)
            result.onSuccess {
                _events.tryEmit("Selected $creatorName for this paid project")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not assign freelancer")
            }
        }
    }

    fun advanceProjectStatus(projectId: String, newStatus: ProjectStatus) {
        viewModelScope.launch {
            val result = repository.updateProjectStatus(projectId, newStatus)
            result.onSuccess {
                _events.tryEmit("Project updated to ${newStatus.label}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update project")
            }
        }
    }

    fun resetAiBriefState() {
        _aiBriefState.value = AiGenerationState.Idle
    }

    fun resetAiPitchState() {
        _aiPitchState.value = AiGenerationState.Idle
    }

    fun resetAiProfileState() {
        _aiProfileState.value = AiGenerationState.Idle
    }

    fun draftProjectBriefWithGemini(
        ideaPrompt: String,
        cityHub: String,
        onDrafted: (AiDraftedProjectBrief) -> Unit
    ) {
        viewModelScope.launch {
            _aiBriefState.value = AiGenerationState.Loading
            try {
                val drafted = aiAssistant.draftProjectBrief(ideaPrompt = ideaPrompt, cityHub = cityHub)
                _aiBriefState.value = AiGenerationState.Success
                onDrafted(drafted)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini brief drafting timed out", e)
                _aiBriefState.value = AiGenerationState.Error(
                    "Gemini request timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini brief drafting failed", e)
                _aiBriefState.value = AiGenerationState.Error(
                    e.localizedMessage ?: "Gemini could not draft the project brief."
                )
            }
        }
    }

    fun draftPitchWithGemini(
        project: PaidProject,
        onStreamingText: (String) -> Unit
    ) {
        viewModelScope.launch {
            _aiPitchState.value = AiGenerationState.Loading
            val accumulated = StringBuilder()
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName
                ?: Firebase.auth.currentUser?.displayName
                ?: "AI Creator"
            val myPortfolio = (myCreatorProfile.value as? UiState.Success)?.data
            try {
                withTimeout(60_000L) {
                    aiAssistant.streamProjectPitch(
                        project = project,
                        creatorProfile = myPortfolio,
                        creatorName = myName
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            val current = accumulated.toString()
                            _aiPitchState.value = AiGenerationState.Streaming(current)
                            onStreamingText(current)
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    _aiPitchState.value = AiGenerationState.Success
                } else {
                    _aiPitchState.value = AiGenerationState.Error("Gemini returned an empty pitch.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini pitch streaming timed out", e)
                _aiPitchState.value = AiGenerationState.Error(
                    "Gemini pitch timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini pitch streaming failed", e)
                _aiPitchState.value = AiGenerationState.Error(
                    e.localizedMessage ?: "Gemini could not draft the pitch."
                )
            }
        }
    }

    fun polishProfileWithGemini(
        displayName: String,
        currentHeadline: String,
        currentBio: String,
        skills: List<SkillCategory>,
        cityHub: String,
        sampleWorkTitle: String,
        onPolished: (AiPolishedProfile) -> Unit
    ) {
        viewModelScope.launch {
            _aiProfileState.value = AiGenerationState.Loading
            try {
                val polished = aiAssistant.polishCreatorProfile(
                    displayName = displayName,
                    currentHeadline = currentHeadline,
                    currentBio = currentBio,
                    skills = skills,
                    cityHub = cityHub,
                    sampleWorkTitle = sampleWorkTitle
                )
                _aiProfileState.value = AiGenerationState.Success
                onPolished(polished)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini profile polish timed out", e)
                _aiProfileState.value = AiGenerationState.Error(
                    "Gemini request timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini profile polish failed", e)
                _aiProfileState.value = AiGenerationState.Error(
                    e.localizedMessage ?: "Gemini could not polish your profile."
                )
            }
        }
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "GuildVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
