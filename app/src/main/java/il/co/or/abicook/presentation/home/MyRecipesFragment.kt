package il.co.or.abicook.presentation.home

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.R
import il.co.or.abicook.domain.repository.FeedSort
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {

    private val vm: MyRecipesViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val panelFilters = view.findViewById<View>(R.id.panelFilters)
        val panelResults = view.findViewById<View>(R.id.panelResults)

        val tvUser = view.findViewById<TextView>(R.id.tvUser)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val rgSort = view.findViewById<RadioGroup>(R.id.rgSort)
        val btnShow = view.findViewById<MaterialButton>(R.id.btnShow)

        val btnEditFilters = view.findViewById<MaterialButton>(R.id.btnEditFilters)
        val rv = view.findViewById<RecyclerView>(R.id.rvMyRecipes)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        bindUsername(tvUser)

        val categoriesArr = resources.getStringArray(R.array.recipe_categories)
        chipGroup.removeAllViews()
        categoriesArr.forEach { c ->
            chipGroup.addView(
                Chip(requireContext()).apply {
                    text = c
                    isCheckable = true
                }
            )
        }

        val adapter = RecipePostAdapter { post ->
            val bundle = androidx.core.os.bundleOf(
                "recipeId" to post.id,
                "fromMyFeed" to true // ✅ חשוב כדי להציג EDIT ב-Details
            )
            findNavController().navigate(R.id.action_global_recipeDetailsFragment, bundle)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collect { s ->
                progress.isVisible = s.isLoading || s.isRefreshing

                if (s.notLoggedIn) {
                    chipGroup.clearCheck()
                    rgSort.check(R.id.rbNewest)
                }

                when {
                    s.notLoggedIn -> {
                        tvEmpty.isVisible = true
                        tvEmpty.text = "Please login"
                    }
                    s.error != null -> {
                        tvEmpty.isVisible = true
                        tvEmpty.text = s.error
                    }
                    s.recipes.isEmpty() && !(s.isLoading || s.isRefreshing) -> {
                        tvEmpty.isVisible = true
                        tvEmpty.text = "No recipes yet"
                    }
                    else -> tvEmpty.isVisible = false
                }

                adapter.submitList(s.recipes)
            }
        }

        val hasResults = vm.uiState.value.recipes.isNotEmpty()
        panelFilters.isVisible = !hasResults
        panelResults.isVisible = hasResults

        btnEditFilters.setOnClickListener {
            panelResults.isVisible = false
            panelFilters.isVisible = true
        }

        btnShow.setOnClickListener {
            val selectedCategories = (0 until chipGroup.childCount)
                .map { chipGroup.getChildAt(it) }
                .filterIsInstance<Chip>()
                .filter { it.isChecked }
                .map { it.text.toString() }

            val sortOption = when (rgSort.checkedRadioButtonId) {
                R.id.rbMostLiked -> FeedSort.MOST_LIKED
                else -> FeedSort.NEWEST
            }

            vm.loadMyRecipes(categories = selectedCategories, sort = sortOption)

            panelFilters.isVisible = false
            panelResults.isVisible = true
        }

        if (savedInstanceState == null && vm.uiState.value.recipes.isEmpty()) {
            vm.loadMyRecipes(categories = emptyList(), sort = FeedSort.NEWEST)
        }
    }

    private fun bindUsername(tv: TextView) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            tv.text = "My recipes"
            return
        }

        val uid = user.uid

        viewLifecycleOwner.lifecycleScope.launch {
            val snap = try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
            } catch (_: Exception) {
                null
            }

            val username = snap?.getString("username")?.trim()?.takeIf { it.isNotBlank() }
            tv.text = "My recipes: ${username ?: (user.displayName ?: user.email ?: "User")}"
        }
    }
}