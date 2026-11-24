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

  // Valid partial update preserving createdAt & userId (including onboarding, phoneNumber & daily goal streak)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Jaz Saleem",
      phoneNumber: "+91 98765 43210",
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

  // Valid single-device session claim update (Action 2)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeSessionId: "550e8400-e29b-41d4-a716-446655440000",
      activeDeviceName: "OnePlus CPH2447",
      sessionUpdatedAt: new Date(Date.now() - 900),
      updatedAt: new Date(Date.now() - 900),
    })
  );

  // Reject oversized phoneNumber (>20 chars) and future sessionUpdatedAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneNumber: "+91 98765 43210 999999",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeSessionId: "550e8400-e29b-41d4-a716-446655440001",
      activeDeviceName: "Future Phone",
      sessionUpdatedAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mixing profile edit with session takeover in a single update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Mixed Mutation",
      activeSessionId: "550e8400-e29b-41d4-a716-446655440002",
      updatedAt: new Date(Date.now() - 500),
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

  // Non-admin student CANNOT modify core customQuestions bank
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customQuestions")
      .doc("C_STUDENT_BLOCKED")
      .set(validCustomQuestionPayload(ALICE_UID, created))
  );

  // Owner Admin CAN create, update, and delete customQuestions on own admin account
  const adminDb = adminContext().firestore();
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .set(validCustomQuestionPayload(ADMIN_UID, created))
  );
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .update({
        difficulty: "Medium",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("customQuestions")
      .doc("C_1001")
      .delete()
  );

  // Reject out-of-bounds correctIndex even for admin
  await assertFails(
    adminDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("customQuestions")
      .doc("C_BAD")
      .set({ ...validCustomQuestionPayload(ADMIN_UID, created), correctIndex: 9 })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("mockHistory").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customQuestions").get()
  );
});

const ADMIN_UID = "admin_owner_001";
const ADMIN_EMAIL = "jazsaleem2004@gmail.com";
const DEVICE_HASH_1 = "a".repeat(64);
const DEVICE_HASH_2 = "b".repeat(64);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function unverifiedAdminContext() {
  return testEnv.authenticatedContext("spoof_admin", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function validSubscriptionConfigPayload(ts) {
  return {
    singleExam1M: 99,
    singleExam3M: 249,
    singleExam6M: 449,
    singleExam1Y: 799,
    allExam1M: 199,
    allExam3M: 499,
    allExam6M: 899,
    allExam1Y: 1499,
    freeDailyQuestionLimit: 15,
    trialDays: 3,
    supportWhatsAppNumber: "+919876543210",
    supportEmail: "jazsaleem2004@gmail.com",
    catalogVersion: 1,
    updatedAt: ts,
  };
}

function validFreeEntitlement(uid, ts) {
  return {
    userId: uid,
    email: "alice@example.com",
    displayName: "Alice Nurse",
    planTier: "free",
    unlockedExamIds: [],
    status: "active",
    trialUsed: false,
    trialDeviceHash: "",
    lastCodeId: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validAccessCodePayload(code, ts, tier = "single_exam", exams = ["aiims_norcet"], tenureDays = 90) {
  return {
    code,
    planTier: tier,
    unlockedExamIds: exams,
    tenureDays,
    status: "active",
    buyerNote: "UPI ref 12345",
    createdByEmail: ADMIN_EMAIL,
    createdAt: ts,
  };
}

test("AppConfig & Admin RBAC: verified admin can write config & list users; spoofed unverified admin and students are rejected", async () => {
  const adminDb = adminContext().firestore();
  const spoofDb = unverifiedAdminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Admin writes subscriptionConfig
  await assertSucceeds(
    adminDb.collection("appConfig").doc("subscriptionConfig").set(validSubscriptionConfigPayload(now))
  );
  // Signed-in student can get subscriptionConfig but cannot list or modify
  await assertSucceeds(aliceDb.collection("appConfig").doc("subscriptionConfig").get());
  await assertFails(aliceDb.collection("appConfig").get());
  await assertFails(
    aliceDb.collection("appConfig").doc("subscriptionConfig").update({ allExam1M: 1, updatedAt: now })
  );

  // Spoofed unverified admin email CANNOT write config or list users
  await assertFails(
    spoofDb.collection("appConfig").doc("subscriptionConfig").update({ allExam1M: 0, updatedAt: now })
  );
  await assertFails(spoofDb.collection("users").get());

  // Verified admin CAN list /users
  await assertSucceeds(adminDb.collection("users").get());
});

test("Entitlements & 3-Day Device-Locked Trial: atomic claim succeeds once; second account on same phone fails; admin reset allows retry", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const created = new Date(Date.now() - 10000);
  const now = new Date(Date.now() - 2000);
  const threeDaysLater = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000);

  // Alice bootstraps free entitlement
  await assertSucceeds(
    aliceDb.collection("entitlements").doc(ALICE_UID).set(validFreeEntitlement(ALICE_UID, created))
  );

  // Alice CANNOT self-upgrade directly to all_exam without a valid access code
  await assertFails(
    aliceDb.collection("entitlements").doc(ALICE_UID).update({
      planTier: "all_exam",
      unlockedExamIds: ["all"],
      expiresAt: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
      updatedAt: now,
    })
  );

  // Alice claims 3-day trial atomically with deviceTrialLocks/{DEVICE_HASH_1}
  const trialBatch = aliceDb.batch();
  trialBatch.set(aliceDb.collection("deviceTrialLocks").doc(DEVICE_HASH_1), {
    deviceHash: DEVICE_HASH_1,
    shortDeviceCode: "CE-AAAA1111",
    ownerUid: ALICE_UID,
    maskedEmail: "a***@example.com",
    deviceName: "Pixel 9",
    claimedAt: now,
  });
  trialBatch.update(aliceDb.collection("entitlements").doc(ALICE_UID), {
    planTier: "trial",
    unlockedExamIds: ["all"],
    status: "active",
    startsAt: now,
    expiresAt: threeDaysLater,
    trialUsed: true,
    trialDeviceHash: DEVICE_HASH_1,
    updatedAt: now,
  });
  await assertSucceeds(trialBatch.commit());

  // Bob on the SAME device CANNOT claim trial (device lock exists)
  await assertSucceeds(
    bobDb.collection("entitlements").doc(BOB_UID).set(validFreeEntitlement(BOB_UID, created))
  );
  const bobCheatBatch = bobDb.batch();
  bobCheatBatch.set(bobDb.collection("deviceTrialLocks").doc(DEVICE_HASH_1), {
    deviceHash: DEVICE_HASH_1,
    shortDeviceCode: "CE-AAAA1111",
    ownerUid: BOB_UID,
    maskedEmail: "b***@example.com",
    deviceName: "Pixel 9",
    claimedAt: now,
  });
  bobCheatBatch.update(bobDb.collection("entitlements").doc(BOB_UID), {
    planTier: "trial",
    unlockedExamIds: ["all"],
    status: "active",
    startsAt: now,
    expiresAt: threeDaysLater,
    trialUsed: true,
    trialDeviceHash: DEVICE_HASH_1,
    updatedAt: now,
  });
  await assertFails(bobCheatBatch.commit());

  // Student CANNOT delete device lock, but Admin CAN
  await assertFails(bobDb.collection("deviceTrialLocks").doc(DEVICE_HASH_1).delete());
  await assertSucceeds(adminDb.collection("deviceTrialLocks").doc(DEVICE_HASH_1).delete());

  // Admin can suspend Alice's entitlement back to suspended
  await assertSucceeds(
    adminDb.collection("entitlements").doc(ALICE_UID).update({
      status: "suspended",
      updatedAt: now,
    })
  );
});

test("Access Codes: admin generates; student redeems atomically; reuse or inflated expiry fails", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const created = new Date(Date.now() - 10000);
  const now = new Date(Date.now() - 2000);
  const codeId = "CE-NOR-3M-ABCD-EFGH";

  // Admin creates single-use 90-day NORCET code
  await assertSucceeds(
    adminDb.collection("accessCodes").doc(codeId).set(
      validAccessCodePayload(codeId, created, "single_exam", ["aiims_norcet"], 90)
    )
  );
  // Student cannot list all codes, but can get a specific code by ID
  await assertFails(aliceDb.collection("accessCodes").get());
  await assertSucceeds(aliceDb.collection("accessCodes").doc(codeId).get());

  // Alice bootstraps free entitlement
  await assertSucceeds(
    aliceDb.collection("entitlements").doc(ALICE_UID).set(validFreeEntitlement(ALICE_UID, created))
  );

  // Reject inflated expiry (+365d on a 90d code)
  const badBatch = aliceDb.batch();
  badBatch.update(aliceDb.collection("accessCodes").doc(codeId), {
    status: "redeemed",
    redeemedByUid: ALICE_UID,
    redeemedByEmail: "alice@example.com",
    redeemedAt: now,
  });
  badBatch.update(aliceDb.collection("entitlements").doc(ALICE_UID), {
    planTier: "single_exam",
    unlockedExamIds: ["aiims_norcet"],
    status: "active",
    startsAt: now,
    expiresAt: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
    lastCodeId: codeId,
    updatedAt: now,
  });
  await assertFails(badBatch.commit());

  // Valid atomic redemption succeeds
  const validExpiry = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000);
  const goodBatch = aliceDb.batch();
  goodBatch.update(aliceDb.collection("accessCodes").doc(codeId), {
    status: "redeemed",
    redeemedByUid: ALICE_UID,
    redeemedByEmail: "alice@example.com",
    redeemedAt: now,
  });
  goodBatch.update(aliceDb.collection("entitlements").doc(ALICE_UID), {
    planTier: "single_exam",
    unlockedExamIds: ["aiims_norcet"],
    status: "active",
    startsAt: now,
    expiresAt: validExpiry,
    lastCodeId: codeId,
    updatedAt: now,
  });
  await assertSucceeds(goodBatch.commit());

  // Bob CANNOT reuse the already redeemed code
  await assertSucceeds(
    bobDb.collection("entitlements").doc(BOB_UID).set(validFreeEntitlement(BOB_UID, created))
  );
  const reuseBatch = bobDb.batch();
  reuseBatch.update(bobDb.collection("accessCodes").doc(codeId), {
    status: "redeemed",
    redeemedByUid: BOB_UID,
    redeemedByEmail: "bob@example.com",
    redeemedAt: now,
  });
  reuseBatch.update(bobDb.collection("entitlements").doc(BOB_UID), {
    planTier: "single_exam",
    unlockedExamIds: ["aiims_norcet"],
    status: "active",
    startsAt: now,
    expiresAt: validExpiry,
    lastCodeId: codeId,
    updatedAt: now,
  });
  await assertFails(reuseBatch.commit());
});

test("QuestionPacks & ClockSync: admin publishes OTA packs; students read published packs only and ping own clockSync", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Admin creates a published pack and a draft pack
  await assertSucceeds(
    adminDb.collection("questionPacks").doc("pack_norcet_10").set({
      packId: "pack_norcet_10",
      title: "NORCET-10 High-Yield Pack",
      examId: "aiims_norcet",
      stage: "mains",
      subject: "MSN",
      questionCount: 1,
      isPremiumOnly: true,
      status: "published",
      version: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("questionPacks").doc("pack_norcet_10").collection("questions").doc("Q_1").set({
      packId: "pack_norcet_10",
      packStatus: "published",
      examId: "aiims_norcet",
      category: "pyq",
      shiftYear: "NORCET-10 Mains",
      subject: "MSN",
      difficulty: "Hard",
      questionText: "First-line antidote for magnesium sulfate toxicity?",
      options: ["Calcium gluconate", "Potassium chloride", "Protamine sulfate", "Naloxone"],
      correctIndex: 0,
      explanation: "10% Calcium gluconate IV reverses magnesium toxicity.",
      sourceStatus: "verified",
      sourceNote: "ClinEdge Admin Pack",
      topicId: "msn_general",
      sourceType: "pyq",
      examYear: "2026",
      examShift: "Mains",
      sortOrder: 0,
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("questionPacks").doc("pack_draft_1").set({
      packId: "pack_draft_1",
      title: "Draft Internal Pack",
      examId: "pgimer_nursing",
      stage: "all",
      subject: "OBG",
      questionCount: 0,
      isPremiumOnly: true,
      status: "draft",
      version: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student filtered list on published packs and published questions succeeds
  await assertSucceeds(
    aliceDb.collection("questionPacks").where("status", "==", "published").get()
  );
  await assertSucceeds(
    aliceDb.collection("questionPacks").doc("pack_norcet_10")
      .collection("questions").where("packStatus", "==", "published").get()
  );

  // Student CANNOT read draft pack or write packs
  await assertFails(aliceDb.collection("questionPacks").doc("pack_draft_1").get());
  await assertFails(aliceDb.collection("questionPacks").get());
  await assertFails(
    aliceDb.collection("questionPacks").doc("pack_norcet_10").update({ title: "Hacked", updatedAt: now })
  );

  // ClockSync: owner read/write succeeds; cross-user fails
  await assertSucceeds(
    aliceDb.collection("clockSync").doc(ALICE_UID).set({ userId: ALICE_UID, serverTime: now })
  );
  await assertSucceeds(aliceDb.collection("clockSync").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("clockSync").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("clockSync").doc(ALICE_UID).set({ userId: ALICE_UID, serverTime: now })
  );
});

test("Doubts: students create/read own doubts and cannot forge faculty replies; admin replies to all", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("owner_admin_uid", {
      email: "jazsaleem2004@gmail.com",
      email_verified: true,
    })
    .firestore();
  const now = new Date(Date.now() - 2000);

  const validDoubt = {
    doubtId: "doubt_1",
    userId: ALICE_UID,
    studentName: "Alice Nurse",
    studentEmail: "alice@example.com",
    examId: "esic_nursing",
    subject: "MSN",
    questionId: "PM_0001",
    questionPreview: "Which electrolyte triggers peaked T waves?",
    doubtText: "Why does calcium gluconate not lower serum potassium directly?",
    status: "open",
    adminReply: "",
    repliedBy: "",
    studentReadReply: true,
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated denied
  await assertFails(unauthDb.collection("doubts").doc("doubt_1").set(validDoubt));
  await assertFails(unauthDb.collection("doubts").get());

  // Student creates own doubt
  await assertSucceeds(aliceDb.collection("doubts").doc("doubt_1").set(validDoubt));

  // Student cannot forge an already-answered doubt on create
  await assertFails(
    aliceDb.collection("doubts").doc("doubt_forged").set({
      ...validDoubt,
      doubtId: "doubt_forged",
      status: "answered",
      adminReply: "Forged reply",
    })
  );

  // Student reads own doubt via filtered list; cannot read Bob's or unfiltered list
  await assertSucceeds(aliceDb.collection("doubts").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("doubts").get());
  await assertFails(bobDb.collection("doubts").doc("doubt_1").get());
  await assertFails(bobDb.collection("doubts").doc("doubt_1").delete());

  // Admin can list all doubts and post official faculty reply
  await assertSucceeds(adminDb.collection("doubts").get());
  await assertSucceeds(
    adminDb.collection("doubts").doc("doubt_1").update({
      status: "answered",
      adminReply: "Calcium gluconate stabilizes the myocardial membrane immediately; insulin+dextrose shifts K+ intracellularly.",
      repliedBy: "ClinEdge Faculty",
      repliedAt: new Date(Date.now() - 1000),
      studentReadReply: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Student can mark reply read, but CANNOT tamper with adminReply
  await assertSucceeds(
    aliceDb.collection("doubts").doc("doubt_1").update({
      studentReadReply: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("doubts").doc("doubt_1").update({
      adminReply: "Tampered reply",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Announcements: admin pushes live news/exam alerts; students can read but cannot write", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("owner_admin_uid", {
      email: "jazsaleem2004@gmail.com",
      email_verified: true,
    })
    .firestore();
  const now = new Date(Date.now() - 2000);

  const alertPayload = {
    announcementId: "alert_norcet_10",
    title: "AIIMS NORCET-10 Registration Live",
    body: "Online registration for AIIMS NORCET-10 Prelims is now open on aiimsexams.ac.in.",
    category: "exam_alert",
    targetExamId: "aiims_norcet",
    priority: "urgent",
    actionUrl: "https://www.aiimsexams.ac.in",
    createdByEmail: "jazsaleem2004@gmail.com",
    createdAt: now,
  };

  // Unauthenticated denied
  await assertFails(unauthDb.collection("announcements").get());

  // Non-admin student cannot push announcements
  await assertFails(aliceDb.collection("announcements").doc("alert_norcet_10").set(alertPayload));

  // Admin pushes announcement
  await assertSucceeds(adminDb.collection("announcements").doc("alert_norcet_10").set(alertPayload));

  // Signed-in student can read & list announcements, but cannot update or delete
  await assertSucceeds(aliceDb.collection("announcements").doc("alert_norcet_10").get());
  await assertSucceeds(aliceDb.collection("announcements").get());
  await assertFails(
    aliceDb.collection("announcements").doc("alert_norcet_10").update({ title: "Hacked" })
  );
  await assertFails(aliceDb.collection("announcements").doc("alert_norcet_10").delete());

  // Admin can delete announcement
  await assertSucceeds(adminDb.collection("announcements").doc("alert_norcet_10").delete());
});

test("DirectNotifications: admin pushes individual alerts; only owner reads/marks read; cross-user & student forge denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("owner_admin_uid", {
      email: "jazsaleem2004@gmail.com",
      email_verified: true,
    })
    .firestore();
  const now = new Date(Date.now() - 2000);

  const notifPayload = {
    notificationId: "notif_1",
    userId: ALICE_UID,
    title: "Faculty replied to your doubt",
    body: "Calcium gluconate stabilizes the cardiac membrane immediately.",
    type: "doubt_reply",
    referenceId: "doubt_1",
    actionUrl: "",
    read: false,
    createdByEmail: "jazsaleem2004@gmail.com",
    createdAt: now,
  };

  const aliceNotifs = (db) => db.collection("users").doc(ALICE_UID).collection("notifications");

  // Unauthenticated denied
  await assertFails(aliceNotifs(unauthDb).get());
  await assertFails(aliceNotifs(unauthDb).doc("notif_1").set(notifPayload));

  // Student cannot forge direct notification for self or others
  await assertFails(aliceNotifs(aliceDb).doc("notif_1").set(notifPayload));
  await assertFails(aliceNotifs(bobDb).doc("notif_1").set(notifPayload));

  // Admin sends individual notification to Alice
  await assertSucceeds(aliceNotifs(adminDb).doc("notif_1").set(notifPayload));

  // Alice can read & list her own notifications; Bob cannot
  await assertSucceeds(aliceNotifs(aliceDb).doc("notif_1").get());
  await assertSucceeds(aliceNotifs(aliceDb).get());
  await assertFails(aliceNotifs(bobDb).doc("notif_1").get());
  await assertFails(aliceNotifs(bobDb).get());

  // Alice can mark read, but cannot tamper with title/body or createdAt
  await assertSucceeds(aliceNotifs(aliceDb).doc("notif_1").update({ read: true }));
  await assertFails(aliceNotifs(aliceDb).doc("notif_1").update({ title: "Tampered", read: true }));
  await assertFails(aliceNotifs(bobDb).doc("notif_1").update({ read: true }));

  // Bob cannot delete Alice's notification; Alice can delete her own
  await assertFails(aliceNotifs(bobDb).doc("notif_1").delete());
  await assertSucceeds(aliceNotifs(aliceDb).doc("notif_1").delete());
});

test("SubscriptionConfig UPI fields: admin can store upiId & upiPayeeName; oversized values rejected", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("appConfig").doc("subscriptionConfig").set({
      ...validSubscriptionConfigPayload(now),
      upiId: "clinedge@okhdfcbank",
      upiPayeeName: "ClinEdge Nursing Prep",
    })
  );
  await assertSucceeds(aliceDb.collection("appConfig").doc("subscriptionConfig").get());

  // Oversized upiId (> 80 chars) fails
  await assertFails(
    adminDb.collection("appConfig").doc("subscriptionConfig").update({
      upiId: "x".repeat(90),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("PaymentRequests: student submits pending UTR, cannot self-approve or read others; admin approves & activates atomically", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  const validRequest = {
    requestId: "pay_req_1",
    userId: ALICE_UID,
    studentName: "Alice Nurse",
    studentEmail: "alice@example.com",
    planTier: "single_exam",
    unlockedExamIds: ["aiims_norcet"],
    tenureDays: 90,
    amountInr: 249,
    utrNumber: "432198765432",
    payerName: "Alice K",
    status: "pending",
    adminNote: "",
    reviewedByEmail: "",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated denied
  await assertFails(unauthDb.collection("paymentRequests").doc("pay_req_1").set(validRequest));
  await assertFails(unauthDb.collection("paymentRequests").get());

  // Student submits pending UPI UTR request
  await assertSucceeds(aliceDb.collection("paymentRequests").doc("pay_req_1").set(validRequest));

  // Student CANNOT forge pre-approved request or too-short UTR
  await assertFails(
    aliceDb.collection("paymentRequests").doc("pay_forged").set({
      ...validRequest,
      requestId: "pay_forged",
      status: "approved",
    })
  );
  await assertFails(
    aliceDb.collection("paymentRequests").doc("pay_short_utr").set({
      ...validRequest,
      requestId: "pay_short_utr",
      utrNumber: "123",
    })
  );

  // Student reads own via filtered list; cannot list all or read Bob's; cannot self-update
  await assertSucceeds(aliceDb.collection("paymentRequests").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("paymentRequests").get());
  await assertFails(bobDb.collection("paymentRequests").doc("pay_req_1").get());
  await assertFails(
    aliceDb.collection("paymentRequests").doc("pay_req_1").update({
      status: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("paymentRequests").doc("pay_req_1").delete());

  // Admin lists all payment requests and approves atomically with entitlement + notification
  await assertSucceeds(adminDb.collection("paymentRequests").get());
  await assertSucceeds(
    aliceDb.collection("entitlements").doc(ALICE_UID).set(validFreeEntitlement(ALICE_UID, now))
  );
  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("paymentRequests").doc("pay_req_1"), {
    status: "approved",
    adminNote: "UTR verified in HDFC statement",
    reviewedByEmail: ADMIN_EMAIL,
    updatedAt: new Date(Date.now() - 1000),
  });
  approveBatch.update(adminDb.collection("entitlements").doc(ALICE_UID), {
    planTier: "single_exam",
    unlockedExamIds: ["aiims_norcet"],
    status: "active",
    startsAt: new Date(Date.now() - 1000),
    expiresAt: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000),
    lastCodeId: "UPI_432198765432",
    updatedAt: new Date(Date.now() - 1000),
  });
  approveBatch.set(
    adminDb.collection("users").doc(ALICE_UID).collection("notifications").doc("pay_approved_1"),
    {
      notificationId: "pay_approved_1",
      userId: ALICE_UID,
      title: "Plan Activated via UPI",
      body: "Your AIIMS NORCET Pro plan (3 Months) is now active!",
      type: "account_notice",
      referenceId: "pay_req_1",
      actionUrl: "",
      read: false,
      createdByEmail: ADMIN_EMAIL,
      createdAt: new Date(Date.now() - 1000),
    }
  );
  await assertSucceeds(approveBatch.commit());
});

test("QuestionReports: student flags MCQ issue, cannot self-resolve or read others; admin resolves", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  const validReport = {
    reportId: "qrep_1",
    userId: ALICE_UID,
    studentName: "Alice Nurse",
    studentEmail: "alice@example.com",
    examId: "aiims_norcet",
    subject: "MSN",
    questionId: "OTA_pack_norcet_10_Q_1",
    questionPreview: "First-line antidote for magnesium sulfate toxicity?",
    reason: "wrong_key",
    note: "Option A should be marked correct per Brunner & Suddarth.",
    status: "open",
    adminResolutionNote: "",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated denied
  await assertFails(unauthDb.collection("questionReports").doc("qrep_1").set(validReport));

  // Student submits open report
  await assertSucceeds(aliceDb.collection("questionReports").doc("qrep_1").set(validReport));

  // Student cannot forge resolved status or invalid reason
  await assertFails(
    aliceDb.collection("questionReports").doc("qrep_forged").set({
      ...validReport,
      reportId: "qrep_forged",
      status: "resolved",
    })
  );
  await assertFails(
    aliceDb.collection("questionReports").doc("qrep_bad_reason").set({
      ...validReport,
      reportId: "qrep_bad_reason",
      reason: "spam_reason",
    })
  );

  // Student reads own via filtered list; Bob denied; student cannot update
  await assertSucceeds(aliceDb.collection("questionReports").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("questionReports").get());
  await assertFails(bobDb.collection("questionReports").doc("qrep_1").get());
  await assertFails(
    aliceDb.collection("questionReports").doc("qrep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin lists all reports and resolves
  await assertSucceeds(adminDb.collection("questionReports").get());
  await assertSucceeds(
    adminDb.collection("questionReports").doc("qrep_1").update({
      status: "resolved",
      adminResolutionNote: "Fixed key in OTA pack v3.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("15. GrandTests & Entries: admin toggle + draft privacy + 1 immutable attempt per student", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = adminContext().firestore();

  const now = new Date(Date.now() - 2000);
  const nowMs = Date.now();

  // Admin can toggle grandTestEnabled in subscriptionConfig
  const configPayload = {
    singleExam1M: 99,
    singleExam3M: 249,
    singleExam6M: 449,
    singleExam1Y: 799,
    allExam1M: 199,
    allExam3M: 499,
    allExam6M: 899,
    allExam1Y: 1499,
    freeDailyQuestionLimit: 15,
    trialDays: 3,
    supportWhatsAppNumber: "+919876543210",
    supportEmail: ADMIN_EMAIL,
    upiId: "clinedge@upi",
    upiPayeeName: "ClinEdge Nursing Prep",
    grandTestEnabled: false,
    catalogVersion: 1,
    updatedAt: now,
  };
  await assertSucceeds(adminDb.collection("appConfig").doc("subscriptionConfig").set(configPayload));
  await assertSucceeds(
    adminDb.collection("appConfig").doc("subscriptionConfig").set({
      ...configPayload,
      grandTestEnabled: true,
    })
  );
  // Non-admin cannot toggle grandTestEnabled
  await assertFails(
    aliceDb.collection("appConfig").doc("subscriptionConfig").update({
      grandTestEnabled: false,
      updatedAt: now,
    })
  );

  // Admin creates a draft test and a live test
  const draftTest = {
    testId: "gt_draft_1",
    title: "All-India NORCET Grand Test #1 (Draft)",
    examId: "aiims_norcet",
    weekLabel: "Week 1",
    questionCount: 50,
    durationMinutes: 45,
    negativeMarks: 0.33,
    status: "draft",
    startsAtMs: nowMs,
    endsAtMs: nowMs + 7 * 86400000,
    createdByEmail: ADMIN_EMAIL,
    createdAt: now,
    updatedAt: now,
  };
  const liveTest = {
    testId: "gt_live_1",
    title: "All-India NORCET Grand Test #2",
    examId: "aiims_norcet",
    weekLabel: "Week 2",
    questionCount: 50,
    durationMinutes: 45,
    negativeMarks: 0.33,
    status: "live",
    startsAtMs: nowMs,
    endsAtMs: nowMs + 7 * 86400000,
    createdByEmail: ADMIN_EMAIL,
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(adminDb.collection("grandTests").doc("gt_draft_1").set(draftTest));
  await assertSucceeds(adminDb.collection("grandTests").doc("gt_live_1").set(liveTest));

  // Students cannot create grandTests or read draft grandTests
  await assertFails(aliceDb.collection("grandTests").doc("gt_hack").set({ ...liveTest, testId: "gt_hack" }));
  await assertFails(unauthDb.collection("grandTests").doc("gt_live_1").get());
  await assertFails(aliceDb.collection("grandTests").doc("gt_draft_1").get());
  await assertFails(aliceDb.collection("grandTests").get()); // unfiltered list denied because draft exists

  // Signed-in students can read live/closed grand tests via status filter
  await assertSucceeds(aliceDb.collection("grandTests").doc("gt_live_1").get());
  await assertSucceeds(
    aliceDb.collection("grandTests").where("status", "in", ["live", "closed"]).get()
  );

  // Alice submits a valid Grand Test entry on gt_live_1
  const aliceEntry = {
    testId: "gt_live_1",
    userId: ALICE_UID,
    studentName: "Alice Nurse",
    examId: "aiims_norcet",
    netScore: 38.0,
    correctCount: 40,
    wrongCount: 6,
    skippedCount: 4,
    totalQuestions: 50,
    accuracyPercent: 87,
    timeTakenSeconds: 1850,
    submittedAt: now,
  };
  await assertFails(unauthDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(ALICE_UID).set(aliceEntry));
  await assertSucceeds(aliceDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(ALICE_UID).set(aliceEntry));

  // Alice cannot update her entry (immutable single attempt)
  await assertFails(
    aliceDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(ALICE_UID).update({
      netScore: 50.0,
    })
  );

  // Bob cannot spoof Alice's entry or submit inconsistent counts
  await assertFails(
    bobDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(ALICE_UID).set({
      ...aliceEntry,
      userId: BOB_UID,
    })
  );
  await assertFails(
    bobDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(BOB_UID).set({
      ...aliceEntry,
      userId: BOB_UID,
      correctCount: 45, // 45 + 6 + 4 != 50
    })
  );

  // Bob cannot submit an entry to a draft test
  await assertFails(
    bobDb.collection("grandTests").doc("gt_draft_1").collection("entries").doc(BOB_UID).set({
      ...aliceEntry,
      testId: "gt_draft_1",
      userId: BOB_UID,
    })
  );

  // Bob can read leaderboard entries for gt_live_1
  await assertSucceeds(bobDb.collection("grandTests").doc("gt_live_1").collection("entries").get());

  // Admin closes gt_live_1; afterwards Bob cannot submit new entry
  await assertSucceeds(
    adminDb.collection("grandTests").doc("gt_live_1").update({
      status: "closed",
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(BOB_UID).set({
      ...aliceEntry,
      userId: BOB_UID,
      studentName: "Bob Nurse",
    })
  );

  // Admin can delete entry and test
  await assertSucceeds(adminDb.collection("grandTests").doc("gt_live_1").collection("entries").doc(ALICE_UID).delete());
  await assertSucceeds(adminDb.collection("grandTests").doc("gt_draft_1").delete());
});



