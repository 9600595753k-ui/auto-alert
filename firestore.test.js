const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');

test('firestore.rules exists and contains zero-trust rules', () => {
  const content = fs.readFileSync('firestore.rules', 'utf8');
  assert.ok(content.includes("rules_version = '2';"));
  assert.ok(content.includes("match /{document=**} {"));
  assert.ok(content.includes("allow read, write: if false;"));
  assert.ok(content.includes("function isOwner(userId)"));
  assert.ok(content.includes("match /users/{userId}"));
  assert.ok(content.includes("match /transactions/{transactionId}"));
  assert.ok(content.includes("match /recurring/{recurringId}"));
  assert.ok(content.includes("match /budgets/{budgetId}"));
});
