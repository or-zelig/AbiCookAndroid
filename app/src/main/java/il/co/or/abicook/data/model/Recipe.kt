package il.co.or.abicook.data.model

data class Recipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val ingredientsSummary: String = "",
    val stepsSummary: String = "",

    // Feed fields
    val primaryCategory: String = "",          // למשל "Pasta"
    val categories: List<String> = emptyList(),// אופציונלי לעתיד
    val prepTimeMin: Int = 0,
    val cookTimeMin: Int = 0,

    // Image
    val imageUrl: String? = null,              // cover image

    // Meta
    val createdAtMillis: Long = 0L,
    val authorId: String = "",
    val authorName: String = "",

    // Social
    val likes: Int = 0,
    val commentsCount: Int = 0
)
