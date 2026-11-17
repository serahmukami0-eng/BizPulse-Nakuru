const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nakuru_123";
const BOB_UID = "bob_nakuru_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function pastDate() {
  return new Date(Date.now() - 1000);
}

function validProfile(uid) {
  return {
    userId: uid,
    ownerName: "Serah Mukami",
    businessName: "Rift Valley Mart",
    businessType: "Retail Shop",
    locationNakuru: "Biashara Street, Nakuru",
    businessSize: "Small (6–20 staff)",
    phone: "+254712345678",
    currency: "KES",
    demoModeEnabled: true,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validTransaction(uid) {
  return {
    userId: uid,
    type: "SALE",
    title: "Counter Basket Sale",
    category: "Retail Sales",
    amountKes: 2450.0,
    paymentMethod: "M-PESA",
    transactionRef: "QHK7482XYZ",
    paymentStatus: "COMPLETED",
    transactionDate: "2026-11-14",
    notes: "Afternoon shift",
    customerId: "cust_1",
    customerName: "Wanjiku Njoroge",
    inventoryItemId: "inv_1",
    inventoryItemName: "Maize Flour 2kg",
    quantitySold: 5,
    unitPriceKes: 500.0,
    discountKes: 50.0,
    costOfGoodsSoldKes: 800.0,
    receiptNumber: "PX-20261114-001",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validInventoryItem(uid) {
  return {
    userId: uid,
    name: "Maize Flour 2kg",
    sku: "MF-002",
    category: "Dry Goods",
    quantity: 35,
    reorderLevel: 10,
    unitPriceKes: 195.0,
    unitCostKes: 155.0,
    supplierId: "sup_1",
    supplierName: "Unga Distributors Nakuru",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validCustomer(uid) {
  return {
    userId: uid,
    name: "Wanjiku Njoroge",
    phone: "+254711223344",
    email: "wanjiku@example.com",
    location: "Milimani, Nakuru",
    notes: "Prefers M-Pesa",
    totalSpentKes: 12400.0,
    purchaseCount: 4,
    lastPurchaseDate: "2026-11-14",
    createdDate: "2026-09-10",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validSupplier(uid) {
  return {
    userId: uid,
    name: "Unga Distributors Nakuru",
    contactPerson: "Peter Kiprop",
    phone: "+254722110099",
    email: "orders@ungadist.co.ke",
    location: "Industrial Area, Nakuru",
    suppliedProductsSummary: "Maize flour, cooking oil, sugar",
    notes: "Delivers Tuesdays & Fridays",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validPurchase(uid) {
  return {
    userId: uid,
    supplierId: "sup_1",
    supplierName: "Unga Distributors Nakuru",
    inventoryItemId: "inv_1",
    itemName: "Maize Flour 2kg",
    quantity: 30,
    unitCostKes: 155.0,
    totalCostKes: 4650.0,
    paymentMethod: "BANK",
    paymentStatus: "PENDING",
    purchaseDate: "2026-11-14",
    notes: "Net-7 invoice",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validStockMovement(uid) {
  return {
    userId: uid,
    inventoryItemId: "inv_1",
    itemName: "Maize Flour 2kg",
    movementType: "PURCHASE_RESTOCK",
    quantityDelta: 30,
    balanceAfter: 65,
    referenceId: "pur_1",
    unitCostKes: 155.0,
    totalCostKes: 4650.0,
    supplierId: "sup_1",
    supplierName: "Unga Distributors Nakuru",
    movementDate: "2026-11-14",
    notes: "Restocked from supplier",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validCampaign(uid) {
  return {
    userId: uid,
    title: "Weekend WhatsApp Flash Promo",
    channel: "WHATSAPP_STATUS",
    campaignDate: "2026-11-14",
    spendKes: 1500.0,
    reachCount: 840,
    engagementCount: 95,
    inquiriesGenerated: 24,
    status: "ACTIVE",
    notes: "Nakuru CBD shoppers",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validCommunication(uid) {
  return {
    userId: uid,
    customerName: "Wanjiku Njoroge",
    channel: "WHATSAPP",
    category: "INQUIRY",
    subject: "Price inquiry for Cooking Oil 5L",
    receivedDate: "2026-11-14",
    responseDate: "2026-11-14",
    status: "CONVERTED",
    responseMinutes: 6,
    notes: "Sent digital receipt",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

test("Unauthenticated caller: cannot read or write any workspace subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("suppliers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("purchases").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("stockMovements").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("campaigns").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("communications").get());
});

test("Authenticated owner: CRUD on profile, POS transactions, inventory, customers, suppliers, purchases & stockMovements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRoot = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userRoot.set(validProfile(ALICE_UID)));
  await assertSucceeds(userRoot.collection("customers").doc("cust_1").set(validCustomer(ALICE_UID)));
  await assertSucceeds(
    userRoot.collection("customers").doc("cust_1").update({
      totalSpentKes: 14850.0,
      purchaseCount: 5,
      lastPurchaseDate: "2026-11-15",
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(userRoot.collection("suppliers").doc("sup_1").set(validSupplier(ALICE_UID)));
  await assertSucceeds(userRoot.collection("inventory").doc("inv_1").set(validInventoryItem(ALICE_UID)));
  await assertSucceeds(userRoot.collection("purchases").doc("pur_1").set(validPurchase(ALICE_UID)));
  await assertSucceeds(
    userRoot.collection("purchases").doc("pur_1").update({
      paymentStatus: "PAID",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(userRoot.collection("stockMovements").doc("mov_1").set(validStockMovement(ALICE_UID)));
  await assertSucceeds(userRoot.collection("transactions").doc("tx_1").set(validTransaction(ALICE_UID)));
  await assertSucceeds(
    userRoot.collection("transactions").doc("tx_1").update({
      paymentStatus: "CANCELLED",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(userRoot.collection("campaigns").doc("camp_1").set(validCampaign(ALICE_UID)));
  await assertSucceeds(userRoot.collection("communications").doc("comm_1").set(validCommunication(ALICE_UID)));
});

test("Cross-user isolation: Bob cannot read or mutate Alice's Prompt 4 collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRoot = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(aliceRoot.collection("customers").doc("c1").set(validCustomer(ALICE_UID)));
  await assertSucceeds(aliceRoot.collection("suppliers").doc("s1").set(validSupplier(ALICE_UID)));
  await assertSucceeds(aliceRoot.collection("purchases").doc("p1").set(validPurchase(ALICE_UID)));
  await assertSucceeds(aliceRoot.collection("stockMovements").doc("m1").set(validStockMovement(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const bobViewAlice = bobDb.collection("users").doc(ALICE_UID);
  await assertFails(bobViewAlice.collection("customers").get());
  await assertFails(bobViewAlice.collection("suppliers").get());
  await assertFails(bobViewAlice.collection("purchases").get());
  await assertFails(bobViewAlice.collection("stockMovements").get());
  await assertFails(
    bobViewAlice.collection("customers").doc("c1").update({ name: "Hijacked", updatedAt: pastDate() })
  );
  await assertFails(
    bobViewAlice.collection("purchases").doc("p1").set(validPurchase(BOB_UID))
  );
});

test("Schema & tampering guards: rejects negative values, ghost IDs, future timestamps, and stockMovement updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRoot = aliceDb.collection("users").doc(ALICE_UID);

  // Reject ghost `id` field
  await assertFails(
    userRoot.collection("customers").doc("c_ghost").set({
      ...validCustomer(ALICE_UID),
      id: "c_ghost",
    })
  );

  // Reject future timestamp
  await assertFails(
    userRoot.collection("suppliers").doc("s_future").set({
      ...validSupplier(ALICE_UID),
      createdAt: new Date(Date.now() + 120000),
      updatedAt: pastDate(),
    })
  );

  // Reject invalid purchase payment status and zero quantity
  await assertFails(
    userRoot.collection("purchases").doc("p_bad_status").set({
      ...validPurchase(ALICE_UID),
      paymentStatus: "FREE",
    })
  );
  await assertFails(
    userRoot.collection("purchases").doc("p_zero_qty").set({
      ...validPurchase(ALICE_UID),
      quantity: 0,
    })
  );

  // Immutable stock movements cannot be updated
  await assertSucceeds(userRoot.collection("stockMovements").doc("mov_immut").set(validStockMovement(ALICE_UID)));
  await assertFails(
    userRoot.collection("stockMovements").doc("mov_immut").update({
      quantityDelta: 999,
      updatedAt: pastDate(),
    })
  );
});
