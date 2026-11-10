package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE,
    GET,
    LIST,
    UPDATE,
    DELETE
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    if (exception is FirebaseFirestoreException) {
        when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                Log.e("FirestoreError", "Permission Denied: [Operation: $operation, Path: $path]. Verify user authentication and security rules.", exception)
            }
            FirebaseFirestoreException.Code.NOT_FOUND -> {
                Log.w("FirestoreError", "Document Not Found: [Operation: $operation, Path: $path].", exception)
            }
            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                Log.w("FirestoreError", "Firestore Service Unavailable: Check network connectivity.", exception)
            }
            else -> {
                Log.e("FirestoreError", "Firestore Error [Code: ${exception.code}, Operation: $operation, Path: $path]: ${exception.message}", exception)
            }
        }
    } else {
        Log.e("FirestoreError", "Unexpected Exception in Firestore operation [Operation: $operation, Path: $path]: ${exception.message}", exception)
    }
}
