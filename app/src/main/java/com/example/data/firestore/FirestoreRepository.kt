package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.RecurringPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(user: FirebaseUser) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: "User"),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            userDoc.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to save user profile: ${e.message}", e)
        }
    }

    suspend fun syncTransaction(tx: TransactionEntity) {
        val uid = requireUserId()
        val txId = if (tx.fingerprint.isNotBlank()) tx.fingerprint else "tx_${tx.id}_${tx.timestamp}"
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fingerprint" to tx.fingerprint,
            "amountPaise" to tx.amountPaise,
            "type" to tx.type,
            "status" to tx.status,
            "merchant" to tx.merchant,
            "category" to tx.category,
            "bankName" to tx.bankName,
            "accountMasked" to tx.accountMasked,
            "paymentMode" to tx.paymentMode,
            "isExcluded" to tx.isExcluded,
            "isRecurring" to tx.isRecurring,
            "createdAt" to FieldValue.serverTimestamp()
        )
        tx.referenceNumber?.let { payload["referenceNumber"] = it }
        tx.notes?.let { payload["notes"] = it }

        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to sync transaction to cloud: ${e.message}", e)
        }
    }

    suspend fun syncRecurring(recurring: RecurringPaymentEntity) {
        val uid = requireUserId()
        val recId = "rec_${recurring.id}_${recurring.merchantName.hashCode()}"
        val docRef = db.collection("users").document(uid).collection("recurring").document(recId)

        val payload = mapOf(
            "userId" to uid,
            "merchantName" to recurring.merchantName,
            "category" to recurring.category,
            "expectedAmountPaise" to recurring.expectedAmountPaise,
            "frequency" to recurring.frequency,
            "status" to recurring.status,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to sync recurring payment: ${e.message}", e)
        }
    }

    suspend fun syncBudget(budget: BudgetEntity) {
        val uid = requireUserId()
        val budgetId = "budget_${budget.category.hashCode()}"
        val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)

        val payload = mapOf(
            "userId" to uid,
            "category" to budget.category,
            "monthlyLimitPaise" to budget.monthlyLimitPaise
        )

        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to sync budget: ${e.message}", e)
        }
    }

    suspend fun syncAllLocalToCloud(
        localTransactions: List<TransactionEntity>,
        localRecurring: List<RecurringPaymentEntity>,
        localBudgets: List<BudgetEntity>
    ) {
        for (tx in localTransactions) {
            syncTransaction(tx)
        }
        for (rec in localRecurring) {
            syncRecurring(rec)
        }
        for (b in localBudgets) {
            syncBudget(b)
        }
    }
}
