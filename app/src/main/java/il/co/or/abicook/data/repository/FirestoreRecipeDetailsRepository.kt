package il.co.or.abicook.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import il.co.or.abicook.domain.model.RecipePost
import kotlinx.coroutines.tasks.await

class FirestoreRecipeDetailsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun observeRecipe(
        recipeId: String,
        onUpdate: (RecipePost) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        val recipeRef = db.collection("recipes").document(recipeId)
        val uid = auth.currentUser?.uid

        var latestRecipe: RecipePost? = null
        var isLikedByMe = false

        fun emitIfReady() {
            val r = latestRecipe ?: return
            onUpdate(r.copy(isLikedByMe = isLikedByMe))
        }

        val recipeReg = recipeRef.addSnapshotListener { snap, e ->
            if (e != null) {
                onError(e.message ?: "Failed to load recipe")
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                onError("Recipe not found")
                return@addSnapshotListener
            }

            val obj = snap.toObject(RecipePost::class.java)
            if (obj == null) {
                onError("Recipe parse error")
                return@addSnapshotListener
            }

            latestRecipe = obj.copy(id = snap.id)
            emitIfReady()
        }

        if (uid == null) return recipeReg

        val likeReg = recipeRef.collection("likes").document(uid).addSnapshotListener { likeSnap, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            isLikedByMe = likeSnap?.exists() == true
            emitIfReady()
        }

        return ListenerRegistration {
            recipeReg.remove()
            likeReg.remove()
        }
    }

    suspend fun toggleLike(recipeId: String) {
        val uid = auth.currentUser?.uid ?: return
        val recipeRef = db.collection("recipes").document(recipeId)
        val likeRef = recipeRef.collection("likes").document(uid)

        db.runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            if (likeSnap.exists()) {
                tx.delete(likeRef)
                tx.update(recipeRef, "likes", FieldValue.increment(-1))
            } else {
                tx.set(likeRef, mapOf("createdAtMillis" to System.currentTimeMillis()))
                tx.update(recipeRef, "likes", FieldValue.increment(1))
            }
        }.await()
    }
}
