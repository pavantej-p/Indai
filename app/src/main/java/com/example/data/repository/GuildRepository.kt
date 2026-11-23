package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.CreatorProfile
import com.example.data.model.INDIA_TECH_HUBS
import com.example.data.model.MessageType
import com.example.data.model.PaidProject
import com.example.data.model.ProjectApplication
import com.example.data.model.ProjectStatus
import com.example.data.model.SkillCategory
import com.example.data.model.UserAccount
import com.example.data.model.UserMode
import com.example.data.model.findIndiaHub
import com.example.data.model.formatInr
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class GuildRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- USER ACCOUNT (OWNER-ONLY PII) ---

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        if (userId.isBlank()) throw IllegalStateException("Blank userId")
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun registerUserProfile(
        user: FirebaseUser,
        displayName: String,
        preferredMode: UserMode,
        cityHubName: String
    ): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user mismatch." }
            val cleanName = displayName.trim().take(80)
            require(cleanName.length >= 2) { "Display name must be at least 2 characters." }
            val hub = findIndiaHub(cityHubName).name
            val email = user.email?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
            val photoUrl = user.photoUrl?.toString()?.takeIf { it.isNotEmpty() }?.take(500)

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "preferredMode" to preferredMode.wireName,
                    "cityHub" to hub,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "preferredMode" to preferredMode.wireName,
                    "cityHub" to hub,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (email != null) updates["email"] = email
                if (photoUrl != null) updates["photoUrl"] = photoUrl
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        preferredMode: UserMode,
        cityHub: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user preference write." }
            val hub = findIndiaHub(cityHub).name
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "preferredMode" to preferredMode.wireName,
                        "cityHub" to hub,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CREATOR PORTFOLIOS (PUBLIC SHOWCASE, NO PII) ---

    fun observePublicCreators(currentUserId: String): Flow<List<CreatorProfile>> = flow {
        if (currentUserId.isBlank()) throw IllegalStateException("Unauthenticated creator observation")
        val path = "creators"
        emitAll(
            db.collection("creators")
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CreatorProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    fun observeMyCreatorProfile(userId: String): Flow<CreatorProfile?> = flow {
        if (userId.isBlank()) throw IllegalStateException("Blank userId")
        val path = "creators/$userId"
        emitAll(
            db.collection("creators").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        CreatorProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveCreatorProfile(
        displayName: String,
        headline: String,
        bio: String,
        skills: List<SkillCategory>,
        cityHubName: String,
        hourlyRateInr: Long,
        portfolioUrl: String,
        sampleWorkTitle: String,
        availableForPaidWork: Boolean
    ): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "creators/$uidPreview"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(60)
            val cleanHeadline = headline.trim().take(100)
            val cleanBio = bio.trim().take(500).takeIf { it.isNotEmpty() }
            val cleanUrl = portfolioUrl.trim().take(300).takeIf { it.isNotEmpty() }
            val cleanSample = sampleWorkTitle.trim().take(100).takeIf { it.isNotEmpty() }
            require(cleanName.length >= 2) { "Display name must be at least 2 characters." }
            require(cleanHeadline.length >= 4) { "Headline must be at least 4 characters." }
            require(skills.isNotEmpty()) { "Select at least one AI skill." }
            require(hourlyRateInr in 0L..500000L) { "Hourly rate must be between ₹0 and ₹5,00,000." }

            val hub = findIndiaHub(cityHubName)
            val skillWireList = skills.distinct().take(5).map { it.wireName }
            val docRef = db.collection("creators").document(uid)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "headline" to cleanHeadline,
                    "bio" to cleanBio,
                    "skills" to skillWireList,
                    "cityHub" to hub.name,
                    "latitude" to hub.latitude,
                    "longitude" to hub.longitude,
                    "hourlyRateInr" to hourlyRateInr,
                    "portfolioUrl" to cleanUrl,
                    "sampleWorkTitle" to cleanSample,
                    "availableForPaidWork" to availableForPaidWork,
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "headline" to cleanHeadline,
                    "skills" to skillWireList,
                    "cityHub" to hub.name,
                    "latitude" to hub.latitude,
                    "longitude" to hub.longitude,
                    "hourlyRateInr" to hourlyRateInr,
                    "availableForPaidWork" to availableForPaidWork,
                    "visibility" to "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanBio != null) updates["bio"] = cleanBio
                if (cleanUrl != null) updates["portfolioUrl"] = cleanUrl
                if (cleanSample != null) updates["sampleWorkTitle"] = cleanSample
                docRef.update(updates).await()
            }

            // Keep core identity fields synced on /users/{uid} for Profile & Inbox consistency
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                userRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "cityHub" to hub.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PAID PROJECTS ---

    fun observePublicProjects(currentUserId: String): Flow<List<PaidProject>> = flow {
        if (currentUserId.isBlank()) throw IllegalStateException("Unauthenticated project observation")
        val path = "projects"
        emitAll(
            db.collection("projects")
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaidProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    suspend fun createPaidProject(
        clientName: String,
        title: String,
        description: String,
        requiredSkills: List<SkillCategory>,
        budgetMinInr: Long,
        budgetMaxInr: Long,
        cityHubName: String,
        remoteAllowed: Boolean
    ): Result<String> {
        val projectId = "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "projects/$projectId"
        return try {
            val uid = requireUserId()
            val cleanClient = clientName.trim().take(60)
            val cleanTitle = title.trim().take(100)
            val cleanDesc = description.trim().take(1000)
            require(cleanClient.length >= 2) { "Client or studio name must be at least 2 characters." }
            require(cleanTitle.length >= 4) { "Project title must be at least 4 characters." }
            require(cleanDesc.length >= 10) { "Description must be at least 10 characters." }
            require(requiredSkills.isNotEmpty()) { "Select at least one required AI skill." }
            require(budgetMinInr in 500L..10000000L) { "Minimum budget must be at least ₹500." }
            require(budgetMaxInr in budgetMinInr..10000000L) { "Maximum budget must be >= minimum budget." }

            val hub = findIndiaHub(cityHubName)
            val payload = mapOf(
                "clientId" to uid,
                "clientName" to cleanClient,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "requiredSkills" to requiredSkills.distinct().take(5).map { it.wireName },
                "budgetMinInr" to budgetMinInr,
                "budgetMaxInr" to budgetMaxInr,
                "cityHub" to hub.name,
                "latitude" to hub.latitude,
                "longitude" to hub.longitude,
                "remoteAllowed" to remoteAllowed,
                "status" to ProjectStatus.OPEN.wireName,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("projects").document(projectId).set(payload).await()
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun selectCreatorForProject(
        projectId: String,
        creatorId: String,
        creatorName: String
    ): Result<Unit> {
        val path = "projects/$projectId"
        return try {
            requireUserId()
            require(projectId.isNotBlank() && creatorId.isNotBlank()) { "Missing project or creator ID." }
            db.collection("projects").document(projectId)
                .update(
                    mapOf(
                        "status" to ProjectStatus.ASSIGNED.wireName,
                        "selectedCreatorId" to creatorId,
                        "selectedCreatorName" to creatorName.trim().take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectStatus(
        projectId: String,
        newStatus: ProjectStatus
    ): Result<Unit> {
        val path = "projects/$projectId"
        return try {
            requireUserId()
            require(projectId.isNotBlank()) { "Missing project ID." }
            db.collection("projects").document(projectId)
                .update(
                    mapOf(
                        "status" to newStatus.wireName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DIRECT MESSAGING & PORTFOLIO PITCHES ---

    fun buildConversationId(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}".take(128)
    }

    fun observeMyApplications(userId: String): Flow<List<ProjectApplication>> = flow {
        if (userId.isBlank()) throw IllegalStateException("Blank userId")
        val path = "users/$userId/applications"
        emitAll(
            db.collection("users").document(userId).collection("applications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProjectApplication::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        if (userId.isBlank()) throw IllegalStateException("Blank userId")
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<ChatMessage>> = flow {
        if (userId.isBlank() || conversationId.isBlank()) throw IllegalStateException("Invalid thread args")
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId)
                .collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun openOrCreateConversationAndPitch(
        partnerUid: String,
        partnerName: String,
        myDisplayName: String,
        project: PaidProject?,
        creatorProfile: CreatorProfile?,
        introText: String,
        attachPortfolio: Boolean
    ): Result<String> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val convId = buildConversationId(uidPreview, partnerUid)
        val convPath = "conversations/$convId"
        return try {
            val uid = requireUserId()
            require(uid != partnerUid) { "You cannot start a DM thread with yourself." }
            val actualConvId = buildConversationId(uid, partnerUid)
            val convRef = db.collection("conversations").document(actualConvId)
            val participants = listOf(uid, partnerUid).sorted()
            val cleanIntro = introText.trim().take(1000).ifEmpty {
                if (project != null) "Hi ${partnerName.trim()}, sharing my portfolio for ${project.title}."
                else "Hi ${partnerName.trim()}, would love to connect on IndiAI Guild."
            }

            val existing = convRef.get().await()
            val cleanMyName = myDisplayName.trim().ifEmpty { "AI Creator" }.take(60)
            val cleanPartnerName = partnerName.trim().ifEmpty { "Network Member" }.take(60)

            if (!existing.exists()) {
                val convPayload = mapOf(
                    "participantIds" to participants,
                    "creatorId" to uid,
                    "creatorName" to cleanMyName,
                    "clientId" to partnerUid,
                    "clientName" to cleanPartnerName,
                    "projectId" to project?.id?.takeIf { it.isNotBlank() },
                    "projectTitle" to project?.title?.take(100),
                    "lastMessage" to cleanIntro.take(500),
                    "lastSenderId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                convRef.set(convPayload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to cleanIntro.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (project != null && project.id.isNotBlank()) {
                    updates["projectId"] = project.id
                    updates["projectTitle"] = project.title.take(100)
                }
                convRef.update(updates).await()
            }

            sendMessageInternal(
                conversationId = actualConvId,
                participantIds = participants,
                senderId = uid,
                senderName = cleanMyName,
                text = cleanIntro,
                creatorProfile = if (attachPortfolio) creatorProfile else null
            )

            if (project != null && project.id.isNotBlank() && project.clientId.isNotBlank()) {
                val appRef = db.collection("users").document(uid)
                    .collection("applications").document(project.id)
                val appExisting = appRef.get().await()
                val cleanProjectTitle = project.title.trim().take(100).ifEmpty { "AI Project" }
                val normalizedTitle = if (cleanProjectTitle.length < 2) "AI Project" else cleanProjectTitle
                val budgetLabel = "${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}".take(60)
                val cityHub = project.cityHub.trim().take(40).let {
                    if (it.length >= 2) it else INDIA_TECH_HUBS.first().name
                }
                val hasPortfolioAttached = attachPortfolio &&
                    creatorProfile != null &&
                    creatorProfile.skills.isNotEmpty()

                if (!appExisting.exists()) {
                    val appPayload = mapOf(
                        "applicantId" to uid,
                        "projectId" to project.id,
                        "projectTitle" to normalizedTitle,
                        "clientId" to project.clientId,
                        "clientName" to cleanPartnerName,
                        "conversationId" to actualConvId,
                        "pitchText" to cleanIntro,
                        "attachedPortfolio" to hasPortfolioAttached,
                        "budgetLabel" to budgetLabel,
                        "cityHub" to cityHub,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    appRef.set(appPayload).await()
                } else {
                    val appUpdates = mapOf(
                        "projectTitle" to normalizedTitle,
                        "clientName" to cleanPartnerName,
                        "conversationId" to actualConvId,
                        "pitchText" to cleanIntro,
                        "attachedPortfolio" to hasPortfolioAttached,
                        "budgetLabel" to budgetLabel,
                        "cityHub" to cityHub,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    appRef.update(appUpdates).await()
                }
            }

            Result.success(actualConvId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convPath)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
        portfolioToAttach: CreatorProfile?
    ): Result<Unit> {
        val convPath = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            require(conversation.id.isNotBlank()) { "Conversation ID cannot be blank." }
            val cleanText = text.trim().take(1000).ifEmpty {
                if (portfolioToAttach != null) "Shared my AI creator portfolio card."
                else throw IllegalArgumentException("Message cannot be empty.")
            }
            val cleanSender = senderName.trim().ifEmpty { "AI Creator" }.take(60)

            sendMessageInternal(
                conversationId = conversation.id,
                participantIds = conversation.participantIds,
                senderId = uid,
                senderName = cleanSender,
                text = cleanText,
                creatorProfile = portfolioToAttach
            )

            db.collection("conversations").document(conversation.id)
                .update(
                    mapOf(
                        "lastMessage" to cleanText.take(500),
                        "lastSenderId" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convPath)
            Result.failure(e)
        }
    }

    private suspend fun sendMessageInternal(
        conversationId: String,
        participantIds: List<String>,
        senderId: String,
        senderName: String,
        text: String,
        creatorProfile: CreatorProfile?
    ) {
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val isPitch = creatorProfile != null && creatorProfile.skills.isNotEmpty()
        val msgPayload = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName,
            "participantIds" to participantIds,
            "text" to text,
            "messageType" to if (isPitch) MessageType.PORTFOLIO_PITCH.wireName else MessageType.TEXT.wireName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (isPitch && creatorProfile != null) {
            msgPayload["portfolioHeadline"] = creatorProfile.headline.take(100)
            msgPayload["portfolioCityHub"] = creatorProfile.cityHub.take(40)
            msgPayload["portfolioSkills"] = creatorProfile.skills.take(5)
            msgPayload["portfolioRateInr"] = creatorProfile.hourlyRateInr
            msgPayload["portfolioUrl"] = creatorProfile.portfolioUrl?.take(300)
            msgPayload["portfolioSampleTitle"] = creatorProfile.sampleWorkTitle?.take(100)
        }
        val filtered = msgPayload.filterValues { it != null }
        db.collection("conversations").document(conversationId)
            .collection("messages").document(msgId)
            .set(filtered)
            .await()
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
