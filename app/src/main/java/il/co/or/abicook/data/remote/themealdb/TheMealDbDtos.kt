package il.co.or.abicook.data.remote.themealdb

data class TheMealDbSearchResponse(
    val meals: List<TheMealDbMealDto>?
)

data class TheMealDbMealDto(
    val idMeal: String,
    val strMealThumb: String?
)