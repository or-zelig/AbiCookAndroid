package il.co.or.abicook.data.mapper

import il.co.or.abicook.data.model.Recipe
import il.co.or.abicook.domain.model.RecipePost

object RecipeMapper {

    fun Recipe.toPost(currentUserId: String? = null): RecipePost {
        return RecipePost(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,

            authorId = authorId,
            authorName = authorName.ifBlank { "Unknown" },

            createdAtMillis = createdAtMillis,

            likes = likes.toLong(),
            commentsCount = commentsCount.toLong(),
            isLikedByMe = false,

            ingredientsSummary = ingredientsSummary,
            stepsSummary = stepsSummary,

            primaryCategory = primaryCategory,
            categories = categories,
            prepTimeMin = prepTimeMin,
            cookTimeMin = cookTimeMin
        )
    }
}
