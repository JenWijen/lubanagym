package com.duta.lubanagym.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Auth Methods
    suspend fun signUp(email: String, password: String) = try {
        auth.createUserWithEmailAndPassword(email, password).await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun signIn(email: String, password: String) = try {
        auth.signInWithEmailAndPassword(email, password).await()
    } catch (e: Exception) {
        throw e
    }

    // NEW: Google Sign-In method
    suspend fun signInWithGoogle(credential: AuthCredential) = try {
        auth.signInWithCredential(credential).await()
    } catch (e: Exception) {
        throw e
    }

    fun signOut() = auth.signOut()
    fun getCurrentUser() = auth.currentUser

    // Firestore Methods (unchanged)
    suspend fun addDocument(collection: String, data: Map<String, Any>) = try {
        firestore.collection(collection).add(data).await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun addDocumentWithId(collection: String, documentId: String, data: Map<String, Any>) = try {
        firestore.collection(collection).document(documentId).set(data).await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun updateDocument(collection: String, documentId: String, data: Map<String, Any>) = try {
        firestore.collection(collection).document(documentId).update(data).await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun deleteDocument(collection: String, documentId: String) = try {
        firestore.collection(collection).document(documentId).delete().await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun getDocument(collection: String, documentId: String) = try {
        firestore.collection(collection).document(documentId).get().await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun getCollection(collection: String) = try {
        firestore.collection(collection).get().await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun getCollectionWhere(collection: String, field: String, value: Any) = try {
        firestore.collection(collection).whereEqualTo(field, value).get().await()
    } catch (e: Exception) {
        throw e
    }

    // Storage Methods (unchanged)
    fun getStorageReference(path: String) = storage.reference.child(path)

    suspend fun uploadFile(path: String, data: ByteArray) = try {
        storage.reference.child(path).putBytes(data).await()
    } catch (e: Exception) {
        throw e
    }

    suspend fun getDownloadUrl(path: String) = try {
        storage.reference.child(path).downloadUrl.await()
    } catch (e: Exception) {
        throw e
    }
}