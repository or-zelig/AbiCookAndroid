package il.co.or.abicook.data.remote.themealdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginBackgroundRepository {

    suspend fun loadImages(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = TheMealDbClient.api.searchMeals("chicken")

            response.meals
                ?.mapNotNull { it.strMealThumb }
                ?.distinct()
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}