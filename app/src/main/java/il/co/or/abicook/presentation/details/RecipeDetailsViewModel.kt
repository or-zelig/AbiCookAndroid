package il.co.or.abicook.presentation.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import il.co.or.abicook.data.repository.FirestoreRecipeDetailsRepository
import il.co.or.abicook.domain.model.RecipePost
import kotlinx.coroutines.launch

data class RecipeDetailsState(
    val loading: Boolean = true,
    val recipe: RecipePost? = null,
    val error: String? = null
)

class RecipeDetailsViewModel(
    private val repo: FirestoreRecipeDetailsRepository
) : ViewModel() {

    private val _state = MutableLiveData(RecipeDetailsState())
    val state: LiveData<RecipeDetailsState> = _state

    private var reg: ListenerRegistration? = null
    private var currentId: String? = null

    fun start(recipeId: String) {
        if (currentId == recipeId && reg != null) return
        currentId = recipeId

        _state.value = RecipeDetailsState(loading = true)

        reg?.remove()
        reg = repo.observeRecipe(
            recipeId = recipeId,
            onUpdate = { recipe ->
                _state.postValue(RecipeDetailsState(loading = false, recipe = recipe))
            },
            onError = { msg ->
                _state.postValue(RecipeDetailsState(loading = false, error = msg))
            }
        )
    }

    fun toggleLike() {
        val id = currentId ?: return
        viewModelScope.launch {
            try {
                repo.toggleLike(id)
            } catch (e: Exception) {
                _state.postValue(_state.value?.copy(error = e.message ?: "Like failed"))
            }
        }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}
