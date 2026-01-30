package il.co.or.abicook.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, username: String): Result<Unit>
    fun getCurrentUserId(): String?
    fun logout()
}
