package il.co.or.abicook.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import il.co.or.abicook.domain.repository.FeedRepository

class HomeFeedViewModelFactory(
    private val feedRepository: FeedRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeFeedViewModel(feedRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
