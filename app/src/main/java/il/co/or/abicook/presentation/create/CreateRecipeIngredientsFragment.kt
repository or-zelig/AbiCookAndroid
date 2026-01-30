package il.co.or.abicook.presentation.create

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import il.co.or.abicook.R
import kotlinx.coroutines.launch

class CreateRecipeIngredientsFragment : Fragment(R.layout.fragment_create_recipe_ingredients) {

    private val vm: CreateRecipeWizardViewModel by activityViewModels()
    private lateinit var adapter: IngredientsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val rv = view.findViewById<RecyclerView>(R.id.rvIngredients)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddIngredient)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNextSteps)

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = IngredientsAdapter(
            onChange = { index, field, value ->
                vm.updateIngredient(index, field, value)
            },
            onRemove = { index ->
                vm.removeIngredient(index)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.itemAnimator = null

        btnAdd.setOnClickListener {
            vm.addIngredient()
            rv.post { if (adapter.itemCount > 0) rv.scrollToPosition(adapter.itemCount - 1) }
        }

        btnNext.setOnClickListener {
            if (!vm.validateStep2()) return@setOnClickListener
            findNavController().navigate(R.id.action_ingredients_to_steps)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    adapter.submitList(s.draft.ingredients)
                }
            }
        }
    }
}
