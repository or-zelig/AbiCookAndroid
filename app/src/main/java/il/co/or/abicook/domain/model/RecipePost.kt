package il.co.or.abicook.domain.model

data class RecipeStep(
    val text: String = "",
    val imageUrl: String? = null
)

data class RecipePost(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null, // cover

    val authorId: String = "",
    val authorName: String = "",

    val createdAtMillis: Long = 0L,

    val likes: Long = 0L,
    val commentsCount: Long = 0L,
    val isLikedByMe: Boolean = false,

    val ingredientsSummary: String = "",
    val stepsSummary: String = "",

    val steps: List<RecipeStep> = emptyList(),

    val primaryCategory: String = "",
    val categories: List<String> = emptyList(),
    val prepTimeMin: Int = 0,
    val cookTimeMin: Int = 0
)
