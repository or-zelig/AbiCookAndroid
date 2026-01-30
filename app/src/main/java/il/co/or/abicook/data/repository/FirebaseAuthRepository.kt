package il.co.or.abicook.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<Unit> {
        return try {
            // 1) Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password).await()
            val user = auth.currentUser ?: return Result.failure(Exception("No user after signUp"))
            val uid = user.uid

            // 2) Set displayName in Auth (so later we can read user.displayName)
            val profile = UserProfileChangeRequest.Builder()
                .setDisplayName(username.trim())
                .build()
            user.updateProfile(profile).await()

            // 3) Save profile in Firestore (users/{uid})
            val userData = mapOf(
                "username" to username.trim(),
                "email" to email.trim(),
                "createdAtMillis" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(userData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun logout() {
        auth.signOut()
    }
}
