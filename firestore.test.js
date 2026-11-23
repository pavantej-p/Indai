const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_client_2";
const CHARLIE_UID = "charlie_outsider_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. USERS (PRIVATE PII ISOLATION) ---

test("Users: unauthenticated and cross-user reads/writes are denied; owner succeeds", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const aliceUserPayload = {
    userId: ALICE_UID,
    displayName: "Aarav Sharma",
    email: "aarav@example.in",
    preferredMode: "CREATOR",
    cityHub: "Bengaluru",
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(aliceUserPayload));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(aliceUserPayload));

  // Cross-user PII read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: new Date(Date.now() - 1000) })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredMode: "CLIENT",
      cityHub: "Mumbai",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // User-owned applied-to project history subcollection
  const appPayload = {
    applicantId: ALICE_UID,
    projectId: "proj_1",
    projectTitle: "WhatsApp RAG Chatbot",
    clientId: BOB_UID,
    clientName: "Priya Nair Studio",
    conversationId: `${ALICE_UID}_${BOB_UID}`,
    pitchText: "Hi Priya, sharing my chatbot portfolio!",
    attachedPortfolio: true,
    budgetLabel: "₹35,000 – ₹75,000",
    cityHub: "Mumbai",
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("applications").doc("proj_1").set(appPayload));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("applications").doc("proj_1").set(appPayload));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("applications").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("applications").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("applications").doc("proj_1").update({
      pitchText: "Updated pitch note",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

// --- 2. CREATORS (PUBLIC PORTFOLIO DIRECTORY) ---

test("Creators: owner create/update, public query alignment, and cross-user denial", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const creatorPayload = {
    userId: ALICE_UID,
    displayName: "Aarav Sharma",
    headline: "AI Video & ComfyUI Director",
    bio: "Creating commercial Sora/Runway reels & brand workflows.",
    skills: ["AI_VIDEO", "AI_IMAGE"],
    cityHub: "Bengaluru",
    latitude: 12.9716,
    longitude: 77.5946,
    hourlyRateInr: 2500,
    portfolioUrl: "https://portfolio.example.in/aarav",
    sampleWorkTitle: "D2C Festive AI Brand Film",
    availableForPaidWork: true,
    visibility: "PUBLIC",
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(unauthDb.collection("creators").doc(ALICE_UID).set(creatorPayload));
  await assertFails(bobDb.collection("creators").doc(ALICE_UID).set(creatorPayload));
  await assertSucceeds(aliceDb.collection("creators").doc(ALICE_UID).set(creatorPayload));

  // Matching Kotlin query (.whereEqualTo("visibility", "PUBLIC")) succeeds
  await assertSucceeds(
    bobDb.collection("creators").where("visibility", "==", "PUBLIC").get()
  );

  // Unfiltered list query fails (rules are not filters)
  await assertFails(bobDb.collection("creators").get());

  // Cross-user update fails
  await assertFails(
    bobDb.collection("creators").doc(ALICE_UID).update({
      headline: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("creators").doc(ALICE_UID).update({
      hourlyRateInr: 3200,
      skills: ["AI_VIDEO", "AI_IMAGE", "AUTOMATION"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid skill enum rejected
  await assertFails(
    aliceDb.collection("creators").doc(ALICE_UID).update({
      skills: ["CRYPTO_SCAM"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("creators").doc(ALICE_UID).update({
      hourlyRateInr: 3500,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

// --- 3. PAID PROJECTS (CLIENT OWNERSHIP & TERMINAL LOCK) ---

test("Projects: create OPEN, public query alignment, select freelancer, and terminal COMPLETED lock", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const projectPayload = {
    clientId: BOB_UID,
    clientName: "Priya Nair Studio",
    title: "WhatsApp RAG Chatbot & Lead Automation",
    description: "Build a bilingual Hindi/English WhatsApp support bot with n8n CRM sync.",
    requiredSkills: ["CHATBOT_DEV", "AUTOMATION"],
    budgetMinInr: 35000,
    budgetMaxInr: 75000,
    cityHub: "Mumbai",
    latitude: 19.0760,
    longitude: 72.8777,
    remoteAllowed: true,
    status: "OPEN",
    visibility: "PUBLIC",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(bobDb.collection("projects").doc("proj_1").set(projectPayload));

  // Alice cannot update Bob's project
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").update({
      status: "ASSIGNED",
      selectedCreatorId: ALICE_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Query alignment
  await assertSucceeds(aliceDb.collection("projects").where("visibility", "==", "PUBLIC").get());
  await assertFails(aliceDb.collection("projects").get());

  // Bob selects Alice for the paid project
  await assertSucceeds(
    bobDb.collection("projects").doc("proj_1").update({
      status: "ASSIGNED",
      selectedCreatorId: ALICE_UID,
      selectedCreatorName: "Aarav Sharma",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob marks project COMPLETED
  await assertSucceeds(
    bobDb.collection("projects").doc("proj_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state locking: COMPLETED project cannot be reopened or mutated
  await assertFails(
    bobDb.collection("projects").doc("proj_1").update({
      status: "OPEN",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

// --- 4. CONVERSATIONS & PORTFOLIO PITCH MESSAGES ---

test("Conversations & Messages: participant isolation, portfolio pitch card create, and outsider denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const convId = `${ALICE_UID}_${BOB_UID}`;

  const convPayload = {
    participantIds: [ALICE_UID, BOB_UID],
    creatorId: ALICE_UID,
    creatorName: "Aarav Sharma",
    clientId: BOB_UID,
    clientName: "Priya Nair Studio",
    projectId: "proj_1",
    projectTitle: "WhatsApp RAG Chatbot & Lead Automation",
    lastMessage: "Shared portfolio for WhatsApp RAG Chatbot",
    lastSenderId: ALICE_UID,
    createdAt: now,
    updatedAt: now,
  };

  // Outsider Charlie cannot create a conversation for Alice & Bob
  await assertFails(charlieDb.collection("conversations").doc(convId).set(convPayload));
  await assertSucceeds(aliceDb.collection("conversations").doc(convId).set(convPayload));

  // Matching participant query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("conversations").get());
  await assertFails(charlieDb.collection("conversations").doc(convId).get());

  // Portfolio pitch message creation by Alice
  const pitchMsg = {
    senderId: ALICE_UID,
    senderName: "Aarav Sharma",
    participantIds: [ALICE_UID, BOB_UID],
    text: "Hi Priya! Attaching my automation & chatbot portfolio for this project.",
    messageType: "PORTFOLIO_PITCH",
    portfolioHeadline: "AI Video & Automation Builder",
    portfolioCityHub: "Bengaluru",
    portfolioSkills: ["CHATBOT_DEV", "AUTOMATION"],
    portfolioRateInr: 2500,
    portfolioUrl: "https://portfolio.example.in/aarav",
    portfolioSampleTitle: "D2C Support Bot Case Study",
    createdAt: new Date(Date.now() - 2000),
  };

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set(pitchMsg)
  );

  // Outsider Charlie cannot read message doc or run unfiltered list on messages
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_hack")
      .set({
        senderId: CHARLIE_UID,
        senderName: "Charlie",
        participantIds: [ALICE_UID, BOB_UID],
        text: "Spam",
        messageType: "TEXT",
        createdAt: new Date(Date.now() - 1000),
      })
  );

  // Bob can read messages via participantIds filter
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Messages are immutable (update/delete forbidden)
  await assertFails(
    aliceDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1")
      .update({ text: "Edited" })
  );
});
