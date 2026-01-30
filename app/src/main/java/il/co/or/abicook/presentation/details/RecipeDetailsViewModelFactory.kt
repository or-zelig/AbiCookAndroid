package il.co.or.abicook.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import il.co.or.abicook.data.repository.FirestoreRecipeDetailsRepository

class RecipeDetailsViewModelFactory(
    private val repo: FirestoreRecipeDetailsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecipeDetailsViewModel(repo) as T
    }
}
