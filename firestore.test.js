const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nurse_123";
const BOB_UID = "bob_nurse_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts) {
  return {
    userId: uid,
    displayName: "Jaz",
    role: "B.Sc. Nursing",
    targetYear: "2026",
    focusSubject: "MSN",
    activeExamId: "aiims_norcet",
    themePreference: "LIGHT",
    negativeMarking: true,
    useTimeLimit: true,
    secondsPerQuestion: 54,
    keepScreenOn: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validProgressPayload(uid, qId, ts, includeOutcome = true) {
  const payload = {
    userId: uid,
    questionId: qId,
    bookmarked: true,
    attemptsCount: 3,
    correctCount: 2,
    wrongCount: 1,
    clientUpdatedAtMs: Date.now() - 5000,
    createdAt: ts,
    updatedAt: ts,
  };
  if (includeOutcome) {
    payload.lastOutcome = "right";
  }
  return payload;
}

function validMockPayload(uid, ts) {
  return {
    userId: uid,
    title: "NORCET Stage I Prelims (100Q)",
    examId: "aiims_norcet",
    totalQuestions: 100,
    correctCount: 78,
    wrongCount: 12,
    skippedCount: 10,
    netScore: 74.04,
    accuracyPercent: 86,
    completedAtMs: Date.now() - 5000,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCustomQuestionPayload(uid, ts) {
  return {
    userId: uid,
    category: "pyq",
    shiftYear: "NORCET-9",
    subject: "MSN",
    difficulty: "Hard",
    questionText: "Which electrolyte abnormality causes tall peaked T waves?",
    options: ["Hypokalemia", "Hyperkalemia", "Hyponatremia", "Hypocalcemia"],
    correctIndex: 1,
    explanation: "Hyperkalemia produces peaked T waves and widened QRS.",
    sourceStatus: "unverified",
    sourceNote: "",
    topicId: "msn_general",
    sourceType: "memory_based",
    examYear: "NORCET-9",
    examShift: "",
    examIds: ["aiims_norcet"],
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("progress").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("mockHistory").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customQuestions").get()
  );
});

test("User profile: owner create, partial update, get succeed; list, shadow field, immutable mutation & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid Kotlin-mirrored create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing /users is always forbidden (no user directory scraping)
  await assertFails(aliceDb.collection("users").get());

  // Valid partial update preserving createdAt & userId (including onboarding & daily goal streak)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Jaz Saleem",
      activeExamId: "rrb_nursing",
      themePreference: "DARK",
      onboardingCompleted: true,
      dailyGoalQuestions: 50,
      currentStreakDays: 4,
      longestStreakDays: 7,
      lastGoalMetDate: "2026-11-22",
      todayStudyDate: "2026-11-22",
      todayAnsweredCount: 52,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject out-of-bounds dailyGoalQuestions (<5 or >200) and negative streak
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyGoalQuestions: 3,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyGoalQuestions: 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentStreakDays: -1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow field (Red Team Shadow Update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject value poisoning (secondsPerQuestion out of bounds, invalid theme, oversized name)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      secondsPerQuestion: 999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themePreference: "NEON",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "X".repeat(60),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Progress subcollection: owner create/update/list (including empty & optional lastOutcome omitted) succeed; spoofing & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").get()
  );

  // Create with optional lastOutcome omitted (bookmarked-only question)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("N_1")
      .set(validProgressPayload(ALICE_UID, "N_1", created, false))
  );

  // Create with lastOutcome present
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("42")
      .set(validProgressPayload(ALICE_UID, "42", created, true))
  );

  // Partial update mirroring Kotlin recordAttempt / toggleBookmark
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("42")
      .update({
        bookmarked: false,
        attemptsCount: 4,
        correctCount: 3,
        wrongCount: 1,
        lastOutcome: "right",
        clientUpdatedAtMs: Date.now() - 1000,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject mismatched questionId vs doc ID
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("99")
      .set(validProgressPayload(ALICE_UID, "100", created, true))
  );

  // Reject redundant @DocumentId `id` field inside body
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("55")
      .set({
        id: "55",
        ...validProgressPayload(ALICE_UID, "55", created, true),
      })
  );

  // Reject invalid lastOutcome enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progress")
      .doc("42")
      .update({
        lastOutcome: "skipped",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Cross-user denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("progress").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("progress").doc("42").get()
  );
});

test("MockHistory & CustomQuestions subcollections: owner CRUD & list succeed; invalid bounds & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollections list cleanly
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mockHistory").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customQuestions").get()
  );

  // MockHistory create & ordered query
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mockHistory")
      .doc("mock_1700000000000_aiims_norcet")
      .set(validMockPayload(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mockHistory")
      .orderBy("completedAtMs", "desc")
      .limit(25)
      .get()
  );

  // Reject invalid accuracyPercent (> 100)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mockHistory")
      .doc("mock_bad")
      .set({ ...validMockPayload(ALICE_UID, created), accuracyPercent: 150 })
  );

  // CustomQuestion create, update, delete
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .set(validCustomQuestionPayload(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .update({
        difficulty: "Medium",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .delete()
  );

  // Reject out-of-bounds correctIndex
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customQuestions")
      .doc("C_BAD")
      .set({ ...validCustomQuestionPayload(ALICE_UID, created), correctIndex: 9 })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("mockHistory").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customQuestions").get()
  );
});
