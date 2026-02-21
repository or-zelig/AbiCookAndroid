package il.co.or.abicook.data.remote.themealdb

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    @GET("search.php")
    suspend fun searchMeals(
        @Query("s") query: String
    ): TheMealDbSearchResponse
}