package il.co.or.abicook.data.remote.themealdb

class LoginBackgroundRepository {

    suspend fun loadImages(): List<String> {
        val response = TheMealDbClient.api.searchMeals("chicken")
        return response.meals
            ?.mapNotNull { it.strMealThumb }
            ?: emptyList()
    }
}