package il.co.or.abicook.presentation.home

import androidx.lifecycle.ViewModel
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FeedFilterState(
    val categories: Set<String> = emptySet(),
    val sort: FeedSort = FeedSort.NEWEST,
    val limitTotalTime: Boolean = false,
    val maxTotalTimeMin: Int? = null
)

class FeedFilterViewModel : ViewModel() {

    private val _state = MutableStateFlow(FeedFilterState())
    val state: StateFlow<FeedFilterState> = _state.asStateFlow()

    fun setState(newState: FeedFilterState) {
        _state.value = newState
    }

    fun clear() {
        _state.value = FeedFilterState()
    }
}
