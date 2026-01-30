package il.co.or.abicook.presentation.create

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class CreateRecipeStepsFragment : Fragment(R.layout.fragment_create_recipe_steps) {

    private val vm: CreateRecipeWizardViewModel by activityViewModels()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: StepsAdapter

    private var pendingPickIndex: Int? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val index = pendingPickIndex
        pendingPickIndex = null
        if (index != null && uri != null) {
            vm.setStepImage(index, uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddStep)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnPublish)

        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        rv = view.findViewById(R.id.rvSteps)

        adapter = StepsAdapter(
            onTextChanged = { index, text -> vm.updateStepText(index, text) },
            onPickImage = { index ->
                pendingPickIndex = index
                pickImage.launch("image/*")
            },
            onRemoveImage = { index -> vm.clearStepImage(index) },
            onRemoveStep = { index -> vm.removeStep(index) }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.itemAnimator = null

        btnAdd.setOnClickListener {
            vm.addStep()
            rv.post {
                if (adapter.itemCount > 0) rv.scrollToPosition(adapter.itemCount - 1)
            }
        }

        btnPublish.setOnClickListener { vm.publishRecipe() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    adapter.submitList(s.draft.steps)

                    btnPublish.isEnabled = !s.isPublishing
                    btnAdd.isEnabled = !s.isPublishing

                    s.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        vm.clearError()
                    }

                    if (s.publishSuccess) {
                        Toast.makeText(requireContext(), "Published!", Toast.LENGTH_SHORT).show()
                        vm.resetAfterPublish()
                        findNavController().popBackStack(R.id.createRecipeBasicInfoFragment, true)
                    }
                }
            }
        }
    }
}
