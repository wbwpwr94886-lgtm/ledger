package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.util.handleFirestoreError
import com.example.util.OperationType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun getCurrentUser() = auth.currentUser

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google أولاً لمزامنة البيانات سحابياً.")
    }

    /**
     * Uploads the full database JSON payload into user's isolated Cloud Firestore document
     */
    suspend fun uploadBackupToFirestore(
        jsonBackup: String,
        accountsCount: Int,
        productsCount: Int,
        debtsCount: Int,
        invoicesCount: Int
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("backups")
            .document()

        val payload = mapOf(
            "userId" to uid,
            "backupData" to jsonBackup,
            "accountsCount" to accountsCount,
            "productsCount" to productsCount,
            "debtsCount" to debtsCount,
            "invoicesCount" to invoicesCount,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Fetches the latest backup document from Cloud Firestore for the authenticated user
     */
    suspend fun getLatestBackupFromFirestore(): String? {
        val uid = requireUserId()
        val query = db.collection("users")
            .document(uid)
            .collection("backups")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)

        try {
            val snapshot = query.get().await()
            if (snapshot.isEmpty) return null
            val doc = snapshot.documents.first()
            return doc.getString("backupData")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid/backups")
            throw e
        }
    }
}
