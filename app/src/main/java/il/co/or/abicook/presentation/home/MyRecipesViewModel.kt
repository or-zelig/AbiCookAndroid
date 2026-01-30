package il.co.or.abicook.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import il.co.or.abicook.data.repository.FirestoreMyRecipesRepository
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MyRecipesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val recipes: List<RecipePost> = emptyList(),
    val error: String? = null,
    val notLoggedIn: Boolean = false,
    val usedLocalFallback: Boolean = false,
    val lastUpdatedMillis: Long? = null
)

class MyRecipesViewModel(
    private val repo: FirestoreMyRecipesRepository = FirestoreMyRecipesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRecipesUiState())
    val uiState: StateFlow<MyRecipesUiState> = _uiState

    private var lastCategories: List<String> = emptyList()
    private var lastSort: FeedSort = FeedSort.NEWEST

    fun loadMyRecipes(categories: List<String>, sort: FeedSort, force: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.value = MyRecipesUiState(notLoggedIn = true, error = null, recipes = emptyList())
            return
        }

        lastCategories = categories
        lastSort = sort

        val hasCache = _uiState.value.recipes.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            isLoading = !hasCache || force,
            isRefreshing = hasCache && !force,
            error = null,
            notLoggedIn = false
        )

        viewModelScope.launch {
            try {
                val result = repo.getMyRecipes(uid, categories, sort, limit = 50L)
                _uiState.value = MyRecipesUiState(
                    recipes = result.recipes,
                    usedLocalFallback = result.usedLocalFallback,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = MyRecipesUiState(
                    recipes = _uiState.value.recipes,
                    error = e.message ?: "Failed to load recipes"
                )
            }
        }
    }

    fun refresh() {
        loadMyRecipes(lastCategories, lastSort, force = true)
    }

    /**
     * ✅ Clear filters (UI action) + reload
     */
    fun clearFiltersAndReload() {
        lastCategories = emptyList()
        lastSort = FeedSort.NEWEST
        loadMyRecipes(lastCategories, lastSort, force = true)
    }

    /**
     * ✅ Logout: לאפס הכל (פילטר + רשימה + מצב)
     */
    fun resetForLogout() {
        lastCategories = emptyList()
        lastSort = FeedSort.NEWEST
        _uiState.value = MyRecipesUiState(
            recipes = emptyList(),
            error = null,
            notLoggedIn = true,
            isLoading = false,
            isRefreshing = false,
            usedLocalFallback = false,
            lastUpdatedMillis = null
        )
    }
}
