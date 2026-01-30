package il.co.or.abicook.domain.repository

import il.co.or.abicook.domain.model.RecipePost
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    suspend fun getFeed(
        categories: List<String>,
        sort: FeedSort
    ): List<RecipePost>
}
