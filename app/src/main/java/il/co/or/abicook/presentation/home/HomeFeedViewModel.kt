package il.co.or.abicook.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.repository.FeedRepository
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.launch

data class HomeFeedUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val posts: List<RecipePost> = emptyList()
)

class HomeFeedViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeFeedUiState(isLoading = true))
    val uiState: LiveData<HomeFeedUiState> = _uiState

    fun loadFeed(categories: List<String> = emptyList(), sort: FeedSort = FeedSort.NEWEST, maxTotalTimeMin: Int?) {
        viewModelScope.launch {
            _uiState.value = HomeFeedUiState(isLoading = true)
            try {
                val posts = feedRepository.getFeed(categories, sort)

                val filtered = if (maxTotalTimeMin != null) {
                    posts.filter { (it.prepTimeMin + it.cookTimeMin) <= maxTotalTimeMin }
                } else posts

                _uiState.value = HomeFeedUiState(isLoading = false, posts = filtered)
            } catch (e: Exception) {
                _uiState.value = HomeFeedUiState(isLoading = false, error = e.message ?: "Failed to load feed")
            }
        }
    }

}
