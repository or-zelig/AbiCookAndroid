package il.co.or.abicook.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.tasks.await

data class MyRecipesResult(
    val recipes: List<RecipePost>,
    val usedLocalFallback: Boolean
)

class FirestoreMyRecipesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * IMPORTANT:
     * We avoid any orderBy / whereArrayContainsAny in MyRecipes queries to prevent composite-index errors.
     * We still fetch REAL data from Firestore (server), then filter/sort locally.
     */
    suspend fun getMyRecipes(
        userId: String,
        categories: List<String>,
        sort: FeedSort,
        limit: Long = 50L
    ): MyRecipesResult {

        // 1) Fetch from server using only equality (no composite index required)
        val snap = db.collection("recipes")
            .whereEqualTo("authorId", userId)
            .get()
            .await()

        var posts = snap.documents.mapNotNull { doc ->
            doc.toObject(RecipePost::class.java)?.copy(id = doc.id)
        }

        Log.d("MY_RECIPES", "server(authorId) uid=$userId docs=${posts.size}")

        // 2) If your old data used "userId" instead of "authorId", fallback:
        if (posts.isEmpty()) {
            val snap2 = db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            posts = snap2.documents.mapNotNull { doc ->
                doc.toObject(RecipePost::class.java)?.copy(id = doc.id)
            }

            Log.d("MY_RECIPES", "server(userId) uid=$userId docs=${posts.size}")
        }

        // 3) Filter categories locally (no index)
        val filtered = if (categories.isEmpty()) {
            posts
        } else {
            val wanted = categories.toSet()
            posts.filter { p ->
                p.primaryCategory in wanted || p.categories.any { it in wanted }
            }
        }

        // 4) Sort locally (no index)
        val sorted = when (sort) {
            FeedSort.NEWEST ->
                filtered.sortedByDescending { it.createdAtMillis }

            FeedSort.MOST_LIKED ->
                filtered.sortedWith(
                    compareByDescending<RecipePost> { it.likes }
                        .thenByDescending { it.createdAtMillis }
                )
        }

        return MyRecipesResult(sorted.take(limit.toInt()), usedLocalFallback = true)
    }
}
