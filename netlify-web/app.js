import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
  collection,
  doc,
  getDoc,
  getFirestore,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import { FIREBASE_WEB_CONFIG, FIRESTORE_DATABASE_ID } from "./firebase-config.js";

const SKILLS = [
  { id: "AI_VIDEO", label: "AI Video" },
  { id: "AI_IMAGE", label: "AI Image" },
  { id: "WEB_DEV", label: "Web & App Dev" },
  { id: "CHATBOT_DEV", label: "Chatbot Dev" },
  { id: "AUTOMATION", label: "Automation" }
];

const HUBS = [
  { name: "Bengaluru", lat: 12.9716, lng: 77.5946 },
  { name: "Mumbai", lat: 19.076, lng: 72.8777 },
  { name: "Delhi NCR", lat: 28.6139, lng: 77.209 },
  { name: "Hyderabad", lat: 17.385, lng: 78.4867 },
  { name: "Pune", lat: 18.5204, lng: 73.8567 },
  { name: "Chennai", lat: 13.0827, lng: 80.2707 },
  { name: "Kolkata", lat: 22.5726, lng: 88.3639 },
  { name: "Ahmedabad", lat: 23.0225, lng: 72.5714 },
  { name: "Kochi", lat: 9.9312, lng: 76.2673 },
  { name: "Jaipur", lat: 26.9124, lng: 75.7873 }
];

const rootEl = document.getElementById("app");

function el(tag, attrs = {}, children = []) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === null || v === undefined) continue;
    if (k === "class") node.className = v;
    else if (k === "text") node.textContent = String(v);
    else if (k.startsWith("on") && typeof v === "function") {
      node.addEventListener(k.slice(2).toLowerCase(), v);
    } else if (k === "checked") {
      node.checked = Boolean(v);
    } else if (k === "disabled") {
      node.disabled = Boolean(v);
    } else if (k === "value") {
      node.value = String(v);
    } else {
      node.setAttribute(k, String(v));
    }
  }
  for (const child of Array.isArray(children) ? children : [children]) {
    if (child === null || child === undefined) continue;
    node.appendChild(typeof child === "string" ? document.createTextNode(child) : child);
  }
  return node;
}

function formatInr(amount) {
  const num = Math.max(0, Math.round(Number(amount) || 0));
  return "₹" + num.toLocaleString("en-IN");
}

function skillLabel(wire) {
  return SKILLS.find((s) => s.id === wire)?.label || wire;
}

function findHub(name) {
  return HUBS.find((h) => h.name.toLowerCase() === (name || "").trim().toLowerCase()) || HUBS[0];
}

function safeExternalUrl(raw) {
  if (!raw) return null;
  try {
    const u = new URL(raw.trim());
    return u.protocol === "https:" || u.protocol === "http:" ? u.toString() : null;
  } catch {
    return null;
  }
}

function isConfigMissing() {
  return (
    !FIREBASE_WEB_CONFIG.apiKey ||
    FIREBASE_WEB_CONFIG.apiKey.includes("YOUR_FIREBASE") ||
    !FIREBASE_WEB_CONFIG.appId ||
    FIREBASE_WEB_CONFIG.appId.includes("YOUR_FIREBASE")
  );
}

const state = {
  user: null,
  authReady: false,
  userAccount: undefined,
  myCreatorProfile: null,
  projects: [],
  creators: [],
  conversations: [],
  applications: [],
  activeMessages: [],
  tab: "projects",
  activeThreadId: null,
  skillFilter: null,
  availableOnly: true,
  searchQuery: "",
  historyTab: "applied",
  modal: null, // { type: 'post' } | { type: 'pitch', partnerUid, partnerName, project }
  toast: null,
  error: null,
  regMode: "CREATOR",
  regHub: HUBS[0].name
};

let toastTimer = null;
function showToast(msg) {
  state.toast = msg;
  render();
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    state.toast = null;
    render();
  }, 3200);
}

if (isConfigMissing()) {
  renderSetupScreen();
} else {
  const firebaseApp = initializeApp(FIREBASE_WEB_CONFIG);
  const auth = getAuth(firebaseApp);
  const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);
  const googleProvider = new GoogleAuthProvider();

  let unsubs = [];
  let unsubMsgs = null;

  function clearDataListeners() {
    unsubs.forEach((u) => u());
    unsubs = [];
    if (unsubMsgs) {
      unsubMsgs();
      unsubMsgs = null;
    }
  }

  function startListeners(uid) {
    clearDataListeners();
    unsubs.push(
      onSnapshot(
        doc(db, "users", uid),
        (snap) => {
          state.userAccount = snap.exists() ? { id: snap.id, ...snap.data() } : null;
          render();
        },
        (err) => {
          state.error = err.message;
          render();
        }
      )
    );
    unsubs.push(
      onSnapshot(doc(db, "creators", uid), (snap) => {
        state.myCreatorProfile = snap.exists() ? { id: snap.id, ...snap.data() } : null;
        render();
      })
    );
    unsubs.push(
      onSnapshot(
        query(collection(db, "projects"), where("visibility", "==", "PUBLIC")),
        (snap) => {
          state.projects = snap.docs
            .map((d) => ({ id: d.id, ...d.data() }))
            .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
          render();
        }
      )
    );
    unsubs.push(
      onSnapshot(
        query(collection(db, "creators"), where("visibility", "==", "PUBLIC")),
        (snap) => {
          state.creators = snap.docs
            .map((d) => ({ id: d.id, ...d.data() }))
            .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
          render();
        }
      )
    );
    unsubs.push(
      onSnapshot(
        query(collection(db, "conversations"), where("participantIds", "array-contains", uid)),
        (snap) => {
          state.conversations = snap.docs
            .map((d) => ({ id: d.id, ...d.data() }))
            .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
          render();
        }
      )
    );
    unsubs.push(
      onSnapshot(collection(db, "users", uid, "applications"), (snap) => {
        state.applications = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
        render();
      })
    );
  }

  function openThread(convId) {
    state.activeThreadId = convId;
    state.activeMessages = [];
    if (unsubMsgs) unsubMsgs();
    const uid = state.user.uid;
    unsubMsgs = onSnapshot(
      query(
        collection(db, "conversations", convId, "messages"),
        where("participantIds", "array-contains", uid)
      ),
      (snap) => {
        state.activeMessages = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
        render();
      }
    );
    render();
  }

  onAuthStateChanged(auth, (user) => {
    state.user = user;
    state.authReady = true;
    state.error = null;
    if (user) {
      state.userAccount = undefined;
      startListeners(user.uid);
    } else {
      clearDataListeners();
      state.userAccount = undefined;
      state.myCreatorProfile = null;
      state.projects = [];
      state.creators = [];
      state.conversations = [];
      state.applications = [];
      state.activeThreadId = null;
      render();
    }
  });

  window.__actions = {
    signIn: async () => {
      try {
        state.error = null;
        await signInWithPopup(auth, googleProvider);
      } catch (e) {
        state.error = e.message;
        render();
      }
    },
    signOut: () => signOut(auth),
    completeRegistration: async (displayName, mode, hubName) => {
      const uid = state.user.uid;
      const cleanName = displayName.trim().slice(0, 80);
      if (cleanName.length < 2) return showToast("Name must be at least 2 characters.");
      const hub = findHub(hubName).name;
      const payload = {
        userId: uid,
        displayName: cleanName,
        preferredMode: mode,
        cityHub: hub,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      };
      if (state.user.email) payload.email = state.user.email.slice(0, 160);
      if (state.user.photoURL) payload.photoUrl = state.user.photoURL.slice(0, 500);
      await setDoc(doc(db, "users", uid), payload);
      showToast(`Welcome to IndiAI Guild (${mode === "CLIENT" ? "Client Mode" : "Creator Mode"})`);
    },
    switchMode: async (newMode) => {
      const uid = state.user.uid;
      await updateDoc(doc(db, "users", uid), {
        preferredMode: newMode,
        updatedAt: serverTimestamp()
      });
      showToast(`Switched to ${newMode === "CLIENT" ? "Client Mode" : "Creator Mode"}`);
    },
    savePortfolio: async (data) => {
      const uid = state.user.uid;
      const hub = findHub(data.cityHub);
      const ref = doc(db, "creators", uid);
      const existing = await getDoc(ref);
      const base = {
        displayName: data.displayName.trim().slice(0, 60),
        headline: data.headline.trim().slice(0, 100),
        skills: data.skills.slice(0, 5),
        cityHub: hub.name,
        latitude: hub.lat,
        longitude: hub.lng,
        hourlyRateInr: Math.max(0, Math.min(500000, Math.round(Number(data.hourlyRateInr) || 0))),
        availableForPaidWork: Boolean(data.availableForPaidWork),
        visibility: "PUBLIC",
        updatedAt: serverTimestamp()
      };
      if (data.bio.trim()) base.bio = data.bio.trim().slice(0, 500);
      if (data.portfolioUrl.trim()) base.portfolioUrl = data.portfolioUrl.trim().slice(0, 300);
      if (data.sampleWorkTitle.trim()) base.sampleWorkTitle = data.sampleWorkTitle.trim().slice(0, 100);

      if (!existing.exists()) {
        await setDoc(ref, { ...base, userId: uid, createdAt: serverTimestamp() });
      } else {
        await updateDoc(ref, base);
      }
      await updateDoc(doc(db, "users", uid), {
        displayName: base.displayName,
        cityHub: hub.name,
        updatedAt: serverTimestamp()
      });
      showToast("Skills & portfolio saved to Firestore");
    },
    createProject: async (data) => {
      const uid = state.user.uid;
      const hub = findHub(data.cityHub);
      const projId = "proj_" + crypto.randomUUID().replace(/-/g, "").slice(0, 16);
      const minInr = Math.max(500, Math.round(Number(data.budgetMinInr) || 5000));
      const maxInr = Math.max(minInr, Math.round(Number(data.budgetMaxInr) || minInr));
      await setDoc(doc(db, "projects", projId), {
        clientId: uid,
        clientName: (state.userAccount?.displayName || state.user.displayName || "Client").slice(0, 60),
        title: data.title.trim().slice(0, 100),
        description: data.description.trim().slice(0, 1000),
        requiredSkills: data.requiredSkills.slice(0, 5),
        budgetMinInr: minInr,
        budgetMaxInr: maxInr,
        cityHub: hub.name,
        latitude: hub.lat,
        longitude: hub.lng,
        remoteAllowed: Boolean(data.remoteAllowed),
        status: "OPEN",
        visibility: "PUBLIC",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      state.modal = null;
      showToast("Paid project posted to IndiAI Guild");
      render();
    },
    updateProjectStatus: async (projectId, newStatus) => {
      await updateDoc(doc(db, "projects", projectId), {
        status: newStatus,
        updatedAt: serverTimestamp()
      });
      showToast(`Project updated to ${newStatus}`);
    },
    startDmPitch: async ({ partnerUid, partnerName, project, introText, attachPortfolio }) => {
      const uid = state.user.uid;
      if (uid === partnerUid) return showToast("You cannot DM yourself.");
      const participants = [uid, partnerUid].sort();
      const convId = `${participants[0]}_${participants[1]}`.slice(0, 128);
      const myName = (state.userAccount?.displayName || state.user.displayName || "AI Creator").slice(0, 60);
      const cleanPartner = (partnerName || "Member").trim().slice(0, 60);
      const cleanIntro =
        introText.trim().slice(0, 1000) ||
        (project ? `Hi ${cleanPartner}, sharing my pitch for ${project.title}.` : `Hi ${cleanPartner}, connecting on IndiAI Guild.`);

      const convRef = doc(db, "conversations", convId);
      const convSnap = await getDoc(convRef);
      if (!convSnap.exists()) {
        const convPayload = {
          participantIds: participants,
          creatorId: uid,
          creatorName: myName,
          clientId: partnerUid,
          clientName: cleanPartner,
          lastMessage: cleanIntro.slice(0, 500),
          lastSenderId: uid,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        };
        if (project?.id) {
          convPayload.projectId = project.id;
          convPayload.projectTitle = project.title.slice(0, 100);
        }
        await setDoc(convRef, convPayload);
      } else {
        const updates = {
          lastMessage: cleanIntro.slice(0, 500),
          lastSenderId: uid,
          updatedAt: serverTimestamp()
        };
        if (project?.id) {
          updates.projectId = project.id;
          updates.projectTitle = project.title.slice(0, 100);
        }
        await updateDoc(convRef, updates);
      }

      const portfolio = state.myCreatorProfile;
      const canAttach = Boolean(attachPortfolio && portfolio && portfolio.skills?.length);
      const msgId = "msg_" + crypto.randomUUID().replace(/-/g, "").slice(0, 16);
      const msgPayload = {
        senderId: uid,
        senderName: myName,
        participantIds: participants,
        text: cleanIntro,
        messageType: canAttach ? "PORTFOLIO_PITCH" : "TEXT",
        createdAt: serverTimestamp()
      };
      if (canAttach) {
        msgPayload.portfolioHeadline = (portfolio.headline || "").slice(0, 100);
        msgPayload.portfolioCityHub = (portfolio.cityHub || "Bengaluru").slice(0, 40);
        msgPayload.portfolioSkills = portfolio.skills.slice(0, 5);
        msgPayload.portfolioRateInr = Math.round(Number(portfolio.hourlyRateInr) || 0);
        if (portfolio.portfolioUrl) msgPayload.portfolioUrl = portfolio.portfolioUrl.slice(0, 300);
        if (portfolio.sampleWorkTitle) msgPayload.portfolioSampleTitle = portfolio.sampleWorkTitle.slice(0, 100);
      }
      await setDoc(doc(db, "conversations", convId, "messages", msgId), msgPayload);

      if (project?.id && project.clientId) {
        const appRef = doc(db, "users", uid, "applications", project.id);
        const appSnap = await getDoc(appRef);
        const appFields = {
          projectTitle: (project.title || "AI Project").slice(0, 100),
          clientName: cleanPartner,
          conversationId: convId,
          pitchText: cleanIntro,
          attachedPortfolio: canAttach,
          budgetLabel: `${formatInr(project.budgetMinInr)} – ${formatInr(project.budgetMaxInr)}`.slice(0, 60),
          cityHub: (project.cityHub || "Bengaluru").slice(0, 40),
          updatedAt: serverTimestamp()
        };
        if (!appSnap.exists()) {
          await setDoc(appRef, {
            ...appFields,
            applicantId: uid,
            projectId: project.id,
            clientId: project.clientId,
            createdAt: serverTimestamp()
          });
        } else {
          await updateDoc(appRef, appFields);
        }
      }

      state.modal = null;
      state.tab = "inbox";
      openThread(convId);
      showToast("Application pitch sent via DM");
    },
    sendMessage: async (conv, text, attachPortfolio) => {
      const uid = state.user.uid;
      const myName = (state.userAccount?.displayName || state.user.displayName || "AI Creator").slice(0, 60);
      const portfolio = state.myCreatorProfile;
      const canAttach = Boolean(attachPortfolio && portfolio && portfolio.skills?.length);
      if (attachPortfolio && !canAttach) {
        return showToast("Upload your skills in Profile first to attach your portfolio card.");
      }
      const cleanText =
        text.trim().slice(0, 1000) || (canAttach ? "Shared my AI creator portfolio card." : "");
      if (!cleanText) return;

      const msgId = "msg_" + crypto.randomUUID().replace(/-/g, "").slice(0, 16);
      const msgPayload = {
        senderId: uid,
        senderName: myName,
        participantIds: conv.participantIds,
        text: cleanText,
        messageType: canAttach ? "PORTFOLIO_PITCH" : "TEXT",
        createdAt: serverTimestamp()
      };
      if (canAttach) {
        msgPayload.portfolioHeadline = (portfolio.headline || "").slice(0, 100);
        msgPayload.portfolioCityHub = (portfolio.cityHub || "Bengaluru").slice(0, 40);
        msgPayload.portfolioSkills = portfolio.skills.slice(0, 5);
        msgPayload.portfolioRateInr = Math.round(Number(portfolio.hourlyRateInr) || 0);
        if (portfolio.portfolioUrl) msgPayload.portfolioUrl = portfolio.portfolioUrl.slice(0, 300);
        if (portfolio.sampleWorkTitle) msgPayload.portfolioSampleTitle = portfolio.sampleWorkTitle.slice(0, 100);
      }
      await setDoc(doc(db, "conversations", conv.id, "messages", msgId), msgPayload);
      await updateDoc(doc(db, "conversations", conv.id), {
        lastMessage: cleanText.slice(0, 500),
        lastSenderId: uid,
        updatedAt: serverTimestamp()
      });
    },
    openThread: (convId) => {
      state.tab = "inbox";
      openThread(convId);
    }
  };

  render();
}

function renderSetupScreen() {
  rootEl.innerHTML = "";
  rootEl.appendChild(
    el("div", { class: "auth-center" }, [
      el("div", { class: "card amber-border auth-card" }, [
        el("div", { class: "row" }, [
          el("div", { class: "brand-badge", text: "IA" }),
          el("div", { class: "stack-sm" }, [
            el("h1", { class: "h2", text: "Connect Firebase Web Config" }),
            el("div", { class: "muted", text: "One-time step before deploying to Netlify" })
          ])
        ]),
        el("p", {
          class: "muted",
          text: "Your Firebase project (gen-lang-client-0370058646) currently only has an Android app registered. To enable Google Sign-In and live Firestore in browsers:"
        }),
        el("div", { class: "card elevated stack-sm" }, [
          el("div", { text: "1. Open Firebase Console -> gen-lang-client-0370058646 -> Project Settings -> Add app -> Web (</>)." }),
          el("div", { text: "2. Copy your Web apiKey and appId into netlify-web/firebase-config.js." }),
          el("div", { text: "3. Add your *.netlify.app domain under Authentication -> Settings -> Authorized domains." }),
          el("div", { text: "4. Drag and drop the netlify-web folder onto app.netlify.com/drop." })
        ])
      ])
    ])
  );
}

function render() {
  if (isConfigMissing()) return renderSetupScreen();
  rootEl.innerHTML = "";

  if (!state.authReady) {
    rootEl.appendChild(
      el("div", { class: "auth-center" }, [
        el("div", { class: "muted", text: "Connecting to IndiAI Guild..." })
      ])
    );
    return;
  }

  if (!state.user) {
    rootEl.appendChild(renderAuthScreen());
    return;
  }

  if (state.userAccount === undefined) {
    rootEl.appendChild(
      el("div", { class: "auth-center" }, [
        el("div", { class: "muted", text: "Loading your studio profile..." })
      ])
    );
    return;
  }

  if (state.userAccount === null) {
    rootEl.appendChild(renderRegistrationScreen());
    return;
  }

  const navItems = [
    { id: "projects", label: "Projects" },
    { id: "creators", label: "Creators" },
    { id: "inbox", label: "Inbox" },
    { id: "profile", label: "Profile" }
  ];

  const shell = el("div", { class: "app-shell" }, [
    el("aside", { class: "nav-rail" }, [
      el("div", { class: "brand" }, [
        el("div", { class: "brand-badge", text: "IA" }),
        el("div", { class: "stack-sm" }, [
          el("div", { class: "brand-title", text: "IndiAI Guild" }),
          el("div", { class: "brand-sub", text: state.userAccount.preferredMode === "CLIENT" ? "Client Mode" : "Creator Mode" })
        ])
      ]),
      ...navItems.map((item) =>
        el("button", {
          class: `nav-btn ${state.tab === item.id ? "active" : ""}`,
          text: item.label,
          onClick: () => {
            state.tab = item.id;
            if (item.id !== "inbox") state.activeThreadId = null;
            render();
          }
        })
      )
    ]),
    el("main", { class: "main-area" }, [
      el("div", { class: "container" }, [
        state.error ? el("div", { class: "banner-error", text: state.error }) : null,
        state.tab === "projects" ? renderProjectsTab() : null,
        state.tab === "creators" ? renderCreatorsTab() : null,
        state.tab === "inbox" ? renderInboxTab() : null,
        state.tab === "profile" ? renderProfileTab() : null
      ])
    ]),
    el(
      "nav",
      { class: "bottom-nav" },
      navItems.map((item) =>
        el("button", {
          class: `nav-btn ${state.tab === item.id ? "active" : ""}`,
          text: item.label,
          onClick: () => {
            state.tab = item.id;
            if (item.id !== "inbox") state.activeThreadId = null;
            render();
          }
        })
      )
    )
  ]);

  rootEl.appendChild(shell);

  if (state.modal?.type === "post") rootEl.appendChild(renderPostProjectModal());
  if (state.modal?.type === "pitch") rootEl.appendChild(renderPitchModal(state.modal));
  if (state.toast) rootEl.appendChild(el("div", { class: "toast", text: state.toast }));
}

function renderAuthScreen() {
  return el("div", { class: "auth-center" }, [
    el("div", { class: "card amber-border auth-card" }, [
      el("div", { class: "row" }, [
        el("div", { class: "brand-badge", text: "IA" }),
        el("div", { class: "stack-sm" }, [
          el("h1", { class: "h1", text: "IndiAI Guild" }),
          el("div", { class: "muted", text: "India's AI Freelancer & Client Network" })
        ])
      ]),
      el("p", {
        class: "muted",
        text: "Browse paid AI briefs, upload your creator skill portfolio, and pitch clients directly in-app."
      }),
      state.error ? el("div", { class: "banner-error", text: state.error }) : null,
      el("button", {
        class: "btn primary full",
        text: "Continue with Google",
        onClick: () => window.__actions.signIn()
      })
    ])
  ]);
}

function renderRegistrationScreen() {
  let nameInput = state.user.displayName || "AI Creator";
  return el("div", { class: "auth-center" }, [
    el("div", { class: "card teal-border auth-card" }, [
      el("h1", { class: "h2", text: "Set Up Your IndiAI Profile" }),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Display Name" }),
        el("input", {
          class: "input",
          value: nameInput,
          onInput: (e) => (nameInput = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Primary Mode (switchable anytime)" }),
        el("div", { class: "segmented" }, [
          el("button", {
            class: `btn ${state.regMode === "CREATOR" ? "primary" : ""}`,
            text: "Creator Mode",
            onClick: () => {
              state.regMode = "CREATOR";
              render();
            }
          }),
          el("button", {
            class: `btn ${state.regMode === "CLIENT" ? "teal" : ""}`,
            text: "Client Mode",
            onClick: () => {
              state.regMode = "CLIENT";
              render();
            }
          })
        ])
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "India Tech Hub" }),
        el(
          "select",
          {
            class: "select",
            value: state.regHub,
            onChange: (e) => (state.regHub = e.target.value)
          },
          HUBS.map((h) => el("option", { value: h.name, text: h.name }))
        )
      ]),
      el("button", {
        class: "btn primary full",
        text: "Enter IndiAI Guild",
        onClick: () => window.__actions.completeRegistration(nameInput, state.regMode, state.regHub)
      })
    ])
  ]);
}

function renderProjectsTab() {
  const isClient = state.userAccount.preferredMode === "CLIENT";
  const scoped = state.availableOnly
    ? state.projects.filter((p) => p.status === "OPEN" || p.status === "SHORTLISTING")
    : state.projects;
  const filtered = scoped.filter((p) => {
    if (state.skillFilter && !(p.requiredSkills || []).includes(state.skillFilter)) return false;
    if (state.searchQuery.trim()) {
      const q = state.searchQuery.trim().toLowerCase();
      return (
        (p.title || "").toLowerCase().includes(q) ||
        (p.description || "").toLowerCase().includes(q) ||
        (p.clientName || "").toLowerCase().includes(q) ||
        (p.cityHub || "").toLowerCase().includes(q)
      );
    }
    return true;
  });

  return el("div", { class: "stack-md" }, [
    el("div", { class: "row between wrap" }, [
      el("div", { class: "stack-sm" }, [
        el("h1", { class: "h1", text: "Paid AI Projects" }),
        el("div", { class: "muted", text: "Live gigs across India's AI hubs" })
      ]),
      isClient
        ? el("button", {
            class: "btn primary",
            text: "+ Post New Project",
            onClick: () => {
              state.modal = { type: "post" };
              render();
            }
          })
        : el("button", {
            class: "btn",
            text: "Switch to Client Mode to Post",
            onClick: () => window.__actions.switchMode("CLIENT")
          })
    ]),
    el("div", { class: "row wrap" }, [
      el("input", {
        class: "input",
        style: "flex:1; min-width:220px;",
        placeholder: "Search AI video, chatbot, hub, client...",
        value: state.searchQuery,
        onInput: (e) => {
          state.searchQuery = e.target.value;
          render();
        }
      }),
      el("button", {
        class: `chip ${state.availableOnly ? "selected-teal" : ""}`,
        text: state.availableOnly ? "Open & Shortlisting Only" : "All Statuses",
        onClick: () => {
          state.availableOnly = !state.availableOnly;
          render();
        }
      })
    ]),
    el("div", { class: "chips" }, [
      el("button", {
        class: `chip ${!state.skillFilter ? "selected-amber" : ""}`,
        text: `All (${scoped.length})`,
        onClick: () => {
          state.skillFilter = null;
          render();
        }
      }),
      ...SKILLS.map((s) => {
        const count = scoped.filter((p) => (p.requiredSkills || []).includes(s.id)).length;
        return el("button", {
          class: `chip ${state.skillFilter === s.id ? "selected-teal" : ""}`,
          text: `${s.label} (${count})`,
          onClick: () => {
            state.skillFilter = state.skillFilter === s.id ? null : s.id;
            render();
          }
        });
      })
    ]),
    filtered.length === 0
      ? el("div", { class: "card", text: "No matching projects yet. Clear filters or post a new brief!" })
      : el(
          "div",
          { class: "stack-md" },
          filtered.map((proj) => renderProjectCard(proj))
        )
  ]);
}

function renderProjectCard(proj) {
  const statusClass = (proj.status || "OPEN").toLowerCase();
  const isOwner = proj.clientId === state.user.uid;
  return el("div", { class: "card" }, [
    el("div", { class: "row between wrap" }, [
      el("span", { class: `pill ${statusClass}`, text: proj.status || "OPEN" }),
      el("span", {
        class: "price",
        text: `${formatInr(proj.budgetMinInr)} – ${formatInr(proj.budgetMaxInr)}`
      })
    ]),
    el("h2", { class: "h3", text: proj.title }),
    el("div", {
      class: "muted",
      text: `${proj.clientName} • ${proj.cityHub}${proj.remoteAllowed ? " • Remote OK" : ""}`
    }),
    el("p", { class: "muted", style: "margin:0; color:#d8d9e0;", text: proj.description }),
    el(
      "div",
      { class: "chips" },
      (proj.requiredSkills || []).map((s) => el("span", { class: "badge-skill", text: skillLabel(s) }))
    ),
    !isOwner
      ? el("div", { class: "row" }, [
          el("button", {
            class: "btn primary",
            text: "Pitch & DM Portfolio",
            onClick: () => {
              state.modal = {
                type: "pitch",
                partnerUid: proj.clientId,
                partnerName: proj.clientName,
                project: proj
              };
              render();
            }
          })
        ])
      : el("div", { class: "tiny-muted", text: "Posted by you (manage status in Profile)" })
  ]);
}

function renderCreatorsTab() {
  return el("div", { class: "stack-md" }, [
    el("div", { class: "stack-sm" }, [
      el("h1", { class: "h1", text: "AI Creators Directory" }),
      el("div", { class: "muted", text: "Verified portfolios published across India" })
    ]),
    state.creators.length === 0
      ? el("div", { class: "card", text: "No creator portfolios published yet. Upload yours on the Profile tab!" })
      : el(
          "div",
          { class: "stack-md" },
          state.creators.map((c) => {
            const url = safeExternalUrl(c.portfolioUrl);
            return el("div", { class: "card teal-border" }, [
              el("div", { class: "row between wrap" }, [
                el("div", { class: "stack-sm" }, [
                  el("h2", { class: "h3", text: c.displayName }),
                  el("div", { class: "muted", text: c.headline })
                ]),
                el("span", { class: "price", text: `${formatInr(c.hourlyRateInr)}/hr` })
              ]),
              c.bio ? el("p", { class: "muted", style: "margin:0;", text: c.bio }) : null,
              el(
                "div",
                { class: "chips" },
                (c.skills || []).map((s) => el("span", { class: "badge-skill", text: skillLabel(s) }))
              ),
              el("div", { class: "tiny-muted", text: `Hub: ${c.cityHub}` }),
              url
                ? el("a", {
                    href: url,
                    target: "_blank",
                    rel: "noopener noreferrer",
                    class: "tiny-muted",
                    style: "color: var(--teal-light);",
                    text: url
                  })
                : null,
              c.userId !== state.user.uid
                ? el("button", {
                    class: "btn",
                    text: "Message Creator",
                    onClick: () => {
                      state.modal = {
                        type: "pitch",
                        partnerUid: c.userId,
                        partnerName: c.displayName,
                        project: null
                      };
                      render();
                    }
                  })
                : null
            ]);
          })
        )
  ]);
}

function renderInboxTab() {
  if (state.activeThreadId) {
    const conv = state.conversations.find((c) => c.id === state.activeThreadId);
    if (!conv) {
      state.activeThreadId = null;
    } else {
      let msgDraft = "";
      let attachCard = Boolean(state.myCreatorProfile?.skills?.length);
      const partnerName = conv.creatorId === state.user.uid ? conv.clientName : conv.creatorName;
      return el("div", { class: "stack-md" }, [
        el("div", { class: "row between" }, [
          el("button", {
            class: "btn",
            text: "← Back to Inbox",
            onClick: () => {
              state.activeThreadId = null;
              render();
            }
          }),
          el("div", { class: "stack-sm", style: "text-align:right;" }, [
            el("div", { class: "h3", text: partnerName }),
            conv.projectTitle ? el("div", { class: "tiny-muted", text: conv.projectTitle }) : null
          ])
        ]),
        el("div", { class: "card" }, [
          el(
            "div",
            { class: "chat-list" },
            state.activeMessages.length === 0
              ? [el("div", { class: "muted", text: "No messages yet." })]
              : state.activeMessages.map((m) => {
                  const mine = m.senderId === state.user.uid;
                  const safeUrl = safeExternalUrl(m.portfolioUrl);
                  return el("div", { class: `bubble ${mine ? "mine" : ""}` }, [
                    el("div", { class: "tiny-muted", text: m.senderName }),
                    m.messageType === "PORTFOLIO_PITCH"
                      ? el("div", { class: "portfolio-card-inline" }, [
                          el("div", { class: "row between" }, [
                            el("span", { class: "tiny-muted", style: "color:var(--teal-light);font-weight:700;", text: "PORTFOLIO CARD" }),
                            el("span", { class: "price", text: `${formatInr(m.portfolioRateInr)}/hr` })
                          ]),
                          el("div", { class: "h3", text: m.portfolioHeadline || "" }),
                          el(
                            "div",
                            { class: "chips" },
                            (m.portfolioSkills || []).map((s) => el("span", { class: "badge-skill", text: skillLabel(s) }))
                          ),
                          safeUrl
                            ? el("a", {
                                href: safeUrl,
                                target: "_blank",
                                rel: "noopener noreferrer",
                                class: "tiny-muted",
                                style: "color:var(--teal-light);",
                                text: safeUrl
                              })
                            : null
                        ])
                      : null,
                    el("div", { text: m.text })
                  ]);
                })
          ),
          el("label", { class: "row muted", style: "cursor:pointer;" }, [
            el("input", {
              type: "checkbox",
              checked: attachCard,
              onChange: (e) => (attachCard = e.target.checked)
            }),
            el("span", { text: "Attach my portfolio card" })
          ]),
          el("div", { class: "row" }, [
            el("input", {
              class: "input",
              placeholder: "Write a reply...",
              onInput: (e) => (msgDraft = e.target.value)
            }),
            el("button", {
              class: "btn primary",
              text: "Send",
              onClick: () => window.__actions.sendMessage(conv, msgDraft, attachCard)
            })
          ])
        ])
      ]);
    }
  }

  return el("div", { class: "stack-md" }, [
    el("div", { class: "stack-sm" }, [
      el("h1", { class: "h1", text: "DM Inbox" }),
      el("div", { class: "muted", text: "Portfolio pitches & project conversations" })
    ]),
    state.conversations.length === 0
      ? el("div", { class: "card", text: "No DM threads yet. Pitch a paid project to start a conversation!" })
      : el(
          "div",
          { class: "stack-md" },
          state.conversations.map((conv) => {
            const partnerName = conv.creatorId === state.user.uid ? conv.clientName : conv.creatorName;
            return el(
              "button",
              {
                class: "card elevated",
                style: "text-align:left; cursor:pointer;",
                onClick: () => window.__actions.openThread(conv.id)
              },
              [
                el("div", { class: "row between" }, [
                  el("div", { class: "h3", text: partnerName }),
                  conv.projectTitle ? el("span", { class: "badge-skill", text: conv.projectTitle }) : null
                ]),
                el("div", { class: "muted", text: conv.lastMessage })
              ]
            );
          })
        )
  ]);
}

function renderProfileTab() {
  const uid = state.user.uid;
  const acct = state.userAccount;
  const p = state.myCreatorProfile;
  const myPosted = state.projects.filter((proj) => proj.clientId === uid);

  // Merge applications + legacy creator conversations
  const projById = new Map(state.projects.map((pr) => [pr.id, pr]));
  const appliedMap = new Map();
  for (const app of state.applications) {
    const pid = app.projectId || app.id;
    const live = projById.get(pid);
    appliedMap.set(pid, {
      projectId: pid,
      projectTitle: live?.title || app.projectTitle,
      clientName: live?.clientName || app.clientName,
      conversationId: app.conversationId,
      pitchText: app.pitchText,
      attachedPortfolio: app.attachedPortfolio,
      budgetLabel: live ? `${formatInr(live.budgetMinInr)} – ${formatInr(live.budgetMaxInr)}` : app.budgetLabel,
      cityHub: live?.cityHub || app.cityHub,
      liveProject: live || null,
      isSelectedCreator: live?.selectedCreatorId === uid
    });
  }
  for (const conv of state.conversations) {
    if (!conv.projectId || conv.creatorId !== uid || appliedMap.has(conv.projectId)) continue;
    const live = projById.get(conv.projectId);
    if (live?.clientId === uid) continue;
    appliedMap.set(conv.projectId, {
      projectId: conv.projectId,
      projectTitle: live?.title || conv.projectTitle || "AI Project",
      clientName: live?.clientName || conv.clientName,
      conversationId: conv.id,
      pitchText: conv.lastMessage,
      attachedPortfolio: false,
      budgetLabel: live ? `${formatInr(live.budgetMinInr)} – ${formatInr(live.budgetMaxInr)}` : "Paid Project",
      cityHub: live?.cityHub || HUBS[0].name,
      liveProject: live || null,
      isSelectedCreator: live?.selectedCreatorId === uid
    });
  }
  const appliedList = Array.from(appliedMap.values());

  const form = {
    displayName: p?.displayName || acct.displayName || "AI Creator",
    headline: p?.headline || "",
    bio: p?.bio || "",
    cityHub: p?.cityHub || acct.cityHub || HUBS[0].name,
    hourlyRateInr: String(p?.hourlyRateInr ?? 1800),
    sampleWorkTitle: p?.sampleWorkTitle || "",
    portfolioUrl: p?.portfolioUrl || "",
    availableForPaidWork: p?.availableForPaidWork ?? true,
    skills: new Set(p?.skills?.length ? p.skills : ["AI_VIDEO"])
  };

  const initials = (acct.displayName || "AI")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((s) => s[0].toUpperCase())
    .join("");

  return el("div", { class: "stack-md" }, [
    el("div", { class: "card amber-border" }, [
      el("div", { class: "row between wrap" }, [
        el("div", { class: "row" }, [
          state.user.photoURL
            ? el("img", { class: "avatar", src: state.user.photoURL, alt: "Avatar" })
            : el("div", { class: "avatar", text: initials || "AI" }),
          el("div", { class: "stack-sm" }, [
            el("h1", { class: "h2", text: acct.displayName }),
            el("div", { class: "muted", text: state.user.email || "Google Account" }),
            el("div", { class: "tiny-muted", text: `Hub: ${acct.cityHub}` })
          ])
        ]),
        el("button", { class: "btn danger", text: "Sign Out", onClick: () => window.__actions.signOut() })
      ]),
      el("div", { class: "chips" }, [
        el("button", {
          class: `chip ${acct.preferredMode === "CREATOR" ? "selected-amber" : ""}`,
          text: "Creator Mode",
          onClick: () => window.__actions.switchMode("CREATOR")
        }),
        el("button", {
          class: `chip ${acct.preferredMode === "CLIENT" ? "selected-teal" : ""}`,
          text: "Client Mode",
          onClick: () => window.__actions.switchMode("CLIENT")
        })
      ]),
      el("div", { class: "stats-grid" }, [
        el("div", { class: "stat-tile" }, [
          el("div", { class: "stat-val", style: "color:var(--teal-light);", text: String(p?.skills?.length || form.skills.size) }),
          el("div", { class: "tiny-muted", text: "Skills" })
        ]),
        el("div", { class: "stat-tile" }, [
          el("div", { class: "stat-val", style: "color:var(--amber-light);", text: String(appliedList.length) }),
          el("div", { class: "tiny-muted", text: "Applied" })
        ]),
        el("div", { class: "stat-tile" }, [
          el("div", { class: "stat-val", text: String(myPosted.length) }),
          el("div", { class: "tiny-muted", text: "Posted" })
        ])
      ])
    ]),

    el("div", { class: "card teal-border" }, [
      el("div", { class: "row between" }, [
        el("h2", { class: "h2", text: "Upload Skills & Creator Portfolio" }),
        p ? el("span", { class: "pill open", text: "LIVE" }) : null
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Creator Name" }),
        el("input", { class: "input", value: form.displayName, onInput: (e) => (form.displayName = e.target.value) })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Specialty Headline" }),
        el("input", {
          class: "input",
          placeholder: "e.g., AI Video Director & n8n Automation Builder",
          value: form.headline,
          onInput: (e) => (form.headline = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Experience & AI Stack" }),
        el("textarea", {
          class: "textarea",
          placeholder: "Runway, Flux, Next.js, LangGraph, Make...",
          value: form.bio,
          onInput: (e) => (form.bio = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "AI Specialties (1–5 skills)" }),
        el(
          "div",
          { class: "chips" },
          SKILLS.map((s) =>
            el("button", {
              class: `chip ${form.skills.has(s.id) ? "selected-teal" : ""}`,
              text: s.label,
              onClick: (e) => {
                if (form.skills.has(s.id) && form.skills.size > 1) {
                  form.skills.delete(s.id);
                  e.currentTarget.classList.remove("selected-teal");
                } else if (!form.skills.has(s.id) && form.skills.size < 5) {
                  form.skills.add(s.id);
                  e.currentTarget.classList.add("selected-teal");
                }
              }
            })
          )
        )
      ]),
      el("div", { class: "row wrap" }, [
        el("div", { class: "field-group", style: "flex:1; min-width:180px;" }, [
          el("label", { class: "field-label", text: "Primary India Tech Hub" }),
          el(
            "select",
            {
              class: "select",
              value: form.cityHub,
              onChange: (e) => (form.cityHub = e.target.value)
            },
            HUBS.map((h) => el("option", { value: h.name, text: h.name }))
          )
        ]),
        el("div", { class: "field-group", style: "flex:1; min-width:160px;" }, [
          el("label", { class: "field-label", text: "Hourly Rate (₹ INR)" }),
          el("input", {
            class: "input",
            type: "number",
            value: form.hourlyRateInr,
            onInput: (e) => (form.hourlyRateInr = e.target.value)
          })
        ])
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Featured Sample Work Title" }),
        el("input", {
          class: "input",
          value: form.sampleWorkTitle,
          onInput: (e) => (form.sampleWorkTitle = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Portfolio / Reel URL" }),
        el("input", {
          class: "input",
          placeholder: "https://...",
          value: form.portfolioUrl,
          onInput: (e) => (form.portfolioUrl = e.target.value)
        })
      ]),
      el("label", { class: "row", style: "cursor:pointer;" }, [
        el("input", {
          type: "checkbox",
          checked: form.availableForPaidWork,
          onChange: (e) => (form.availableForPaidWork = e.target.checked)
        }),
        el("span", { text: "Available for Paid Projects" })
      ]),
      el("button", {
        class: "btn primary full",
        text: "Save & Upload Skills",
        onClick: () => {
          if (form.displayName.trim().length < 2) return showToast("Name must be at least 2 characters.");
          if (form.headline.trim().length < 4) return showToast("Headline must be at least 4 characters.");
          window.__actions.savePortfolio({ ...form, skills: Array.from(form.skills) });
        }
      })
    ]),

    el("div", { class: "stack-md" }, [
      el("h2", { class: "h2", text: "Project History" }),
      el("div", { class: "segmented" }, [
        el("button", {
          class: `btn ${state.historyTab === "applied" ? "primary" : ""}`,
          text: `Applied (${appliedList.length})`,
          onClick: () => {
            state.historyTab = "applied";
            render();
          }
        }),
        el("button", {
          class: `btn ${state.historyTab === "posted" ? "teal" : ""}`,
          text: `Posted (${myPosted.length})`,
          onClick: () => {
            state.historyTab = "posted";
            render();
          }
        })
      ]),
      ...(state.historyTab === "applied"
        ? appliedList.length === 0
          ? [el("div", { class: "card", text: "No applied projects yet. Pitch on paid briefs in Projects!" })]
          : appliedList.map((item) =>
              el("div", { class: `card elevated ${item.isSelectedCreator ? "teal-border" : ""}` }, [
                el("div", { class: "row between wrap" }, [
                  el("div", { class: "row" }, [
                    el("span", {
                      class: `pill ${(item.liveProject?.status || "shortlisting").toLowerCase()}`,
                      text: item.liveProject?.status || "APPLIED"
                    }),
                    item.isSelectedCreator ? el("span", { class: "pill open", text: "SELECTED!" }) : null
                  ]),
                  el("span", { class: "price", text: item.budgetLabel })
                ]),
                el("div", { class: "h3", text: item.projectTitle }),
                el("div", { class: "muted", text: `Client: ${item.clientName} • ${item.cityHub}` }),
                item.attachedPortfolio
                  ? el("span", { class: "badge-skill", style: "align-self:flex-start;", text: "Portfolio Card Attached" })
                  : null,
                item.pitchText ? el("div", { class: "muted", text: `Pitch: ${item.pitchText}` }) : null,
                el("button", {
                  class: "btn",
                  text: "Open DM Thread →",
                  onClick: () => window.__actions.openThread(item.conversationId)
                })
              ])
            )
        : myPosted.length === 0
          ? [el("div", { class: "card", text: "No posted projects yet." })]
          : myPosted.map((proj) =>
              el("div", { class: "card elevated" }, [
                el("div", { class: "row between wrap" }, [
                  el("span", { class: `pill ${(proj.status || "OPEN").toLowerCase()}`, text: proj.status }),
                  el("span", {
                    class: "price",
                    text: `${formatInr(proj.budgetMinInr)} – ${formatInr(proj.budgetMaxInr)}`
                  })
                ]),
                el("div", { class: "h3", text: proj.title }),
                el("div", { class: "muted", text: `${proj.cityHub}${proj.remoteAllowed ? " • Remote OK" : ""}` }),
                proj.status !== "COMPLETED"
                  ? el("div", { class: "row wrap" }, [
                      proj.status === "OPEN"
                        ? el("button", {
                            class: "btn",
                            text: "Mark Shortlisting",
                            onClick: () => window.__actions.updateProjectStatus(proj.id, "SHORTLISTING")
                          })
                        : null,
                      el("button", {
                        class: "btn",
                        text: "Mark Completed",
                        onClick: () => window.__actions.updateProjectStatus(proj.id, "COMPLETED")
                      })
                    ])
                  : null
              ])
            ))
    ])
  ]);
}

function renderPostProjectModal() {
  const form = {
    title: "",
    description: "",
    budgetMinInr: "15000",
    budgetMaxInr: "45000",
    cityHub: state.userAccount?.cityHub || HUBS[0].name,
    remoteAllowed: true,
    requiredSkills: new Set(["AI_VIDEO"])
  };

  return el("div", { class: "modal-backdrop" }, [
    el("div", { class: "modal-card" }, [
      el("div", { class: "row between" }, [
        el("h2", { class: "h2", text: "Post New AI Project" }),
        el("button", {
          class: "btn",
          text: "Close",
          onClick: () => {
            state.modal = null;
            render();
          }
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Project Title" }),
        el("input", {
          class: "input",
          placeholder: "e.g., AI Product Launch Video Reel",
          onInput: (e) => (form.title = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Description (min 10 chars)" }),
        el("textarea", {
          class: "textarea",
          placeholder: "Describe deliverables, timeline, and tools...",
          onInput: (e) => (form.description = e.target.value)
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Category / Required Skills" }),
        el(
          "div",
          { class: "chips" },
          SKILLS.map((s) =>
            el("button", {
              class: `chip ${form.requiredSkills.has(s.id) ? "selected-teal" : ""}`,
              text: s.label,
              onClick: (e) => {
                if (form.requiredSkills.has(s.id) && form.requiredSkills.size > 1) {
                  form.requiredSkills.delete(s.id);
                  e.currentTarget.classList.remove("selected-teal");
                } else if (!form.requiredSkills.has(s.id)) {
                  form.requiredSkills.add(s.id);
                  e.currentTarget.classList.add("selected-teal");
                }
              }
            })
          )
        )
      ]),
      el("div", { class: "row wrap" }, [
        el("div", { class: "field-group", style: "flex:1;" }, [
          el("label", { class: "field-label", text: "Min Budget (₹)" }),
          el("input", {
            class: "input",
            type: "number",
            value: form.budgetMinInr,
            onInput: (e) => (form.budgetMinInr = e.target.value)
          })
        ]),
        el("div", { class: "field-group", style: "flex:1;" }, [
          el("label", { class: "field-label", text: "Max Budget (₹)" }),
          el("input", {
            class: "input",
            type: "number",
            value: form.budgetMaxInr,
            onInput: (e) => (form.budgetMaxInr = e.target.value)
          })
        ])
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "India Tech Hub" }),
        el(
          "select",
          {
            class: "select",
            value: form.cityHub,
            onChange: (e) => (form.cityHub = e.target.value)
          },
          HUBS.map((h) => el("option", { value: h.name, text: h.name }))
        )
      ]),
      el("label", { class: "row", style: "cursor:pointer;" }, [
        el("input", {
          type: "checkbox",
          checked: form.remoteAllowed,
          onChange: (e) => (form.remoteAllowed = e.target.checked)
        }),
        el("span", { text: "Remote collaboration allowed across India" })
      ]),
      el("button", {
        class: "btn primary full",
        text: "Publish Paid Project",
        onClick: () => {
          if (form.title.trim().length < 4) return showToast("Title must be at least 4 characters.");
          if (form.description.trim().length < 10) return showToast("Description must be at least 10 characters.");
          window.__actions.createProject({ ...form, requiredSkills: Array.from(form.requiredSkills) });
        }
      })
    ])
  ]);
}

function renderPitchModal(modal) {
  let introText = "";
  let attachPortfolio = Boolean(state.myCreatorProfile?.skills?.length);

  return el("div", { class: "modal-backdrop" }, [
    el("div", { class: "modal-card" }, [
      el("div", { class: "row between" }, [
        el("h2", { class: "h2", text: modal.project ? `Apply: ${modal.project.title}` : `DM ${modal.partnerName}` }),
        el("button", {
          class: "btn",
          text: "Close",
          onClick: () => {
            state.modal = null;
            render();
          }
        })
      ]),
      el("div", { class: "field-group" }, [
        el("label", { class: "field-label", text: "Your Pitch Message" }),
        el("textarea", {
          class: "textarea",
          placeholder: "Share relevant AI work, turnaround time, and quote...",
          onInput: (e) => (introText = e.target.value)
        })
      ]),
      state.myCreatorProfile?.skills?.length
        ? el("label", { class: "row", style: "cursor:pointer;" }, [
            el("input", {
              type: "checkbox",
              checked: attachPortfolio,
              onChange: (e) => (attachPortfolio = e.target.checked)
            }),
            el("span", { text: "Attach my Creator Portfolio Card" })
          ])
        : el("div", {
            class: "banner-info",
            text: "Tip: Upload your skills on the Profile tab to attach your Portfolio Card to applications."
          }),
      el("button", {
        class: "btn primary full",
        text: "Send Pitch & Open DM",
        onClick: () =>
          window.__actions.startDmPitch({
            partnerUid: modal.partnerUid,
            partnerName: modal.partnerName,
            project: modal.project,
            introText,
            attachPortfolio
          })
      })
    ])
  ]);
}
