package il.co.or.abicook.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import il.co.or.abicook.data.mapper.RecipeMapper.toPost
import il.co.or.abicook.data.model.Recipe
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.repository.FeedRepository
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.tasks.await

class FirestoreFeedRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FeedRepository {

    override suspend fun getFeed(
        categories: List<String>,
        sort: FeedSort
    ): List<RecipePost> {

        var q: Query = firestore.collection("recipes")

        if (categories.isNotEmpty()) {
            q = q.whereArrayContainsAny("categories", categories)
        }

        q = when (sort) {
            FeedSort.NEWEST -> q.orderBy("createdAtMillis", Query.Direction.DESCENDING)
            FeedSort.MOST_LIKED -> q.orderBy("likes", Query.Direction.DESCENDING)
        }

        val snap = q.limit(50).get().await()

        val recipes = snap.documents.mapNotNull { doc ->
            doc.toObject(Recipe::class.java)?.copy(id = doc.id)
        }

        return recipes.map { it.toPost() }
    }
}
