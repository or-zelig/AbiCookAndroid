package il.co.or.abicook.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.domain.model.RecipePost
import kotlinx.coroutines.tasks.await

class FirestoreRecipeDataRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val recipesCol = firestore.collection("recipes")

    suspend fun createRecipe(post: RecipePost) {
        require(post.id.isNotBlank()) { "RecipePost.id is blank" }
        recipesCol.document(post.id).set(post).await()
    }

    suspend fun getRecipeById(recipeId: String): RecipePost {
        val doc = recipesCol.document(recipeId).get().await()
        val obj = doc.toObject(RecipePost::class.java)
            ?: throw IllegalStateException("Recipe not found")
        return obj.copy(id = doc.id)
    }
}
