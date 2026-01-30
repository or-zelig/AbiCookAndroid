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
}
