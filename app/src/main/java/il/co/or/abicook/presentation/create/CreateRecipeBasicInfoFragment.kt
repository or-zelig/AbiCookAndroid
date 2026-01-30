package il.co.or.abicook.presentation.create

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import il.co.or.abicook.R

class CreateRecipeBasicInfoFragment : Fragment(R.layout.fragment_create_recipe_basic_info) {

    // חשוב: activityViewModels כדי שהדראפט ישמר בין 3 המסכים
    private val vm: CreateRecipeWizardViewModel by activityViewModels()

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.setCoverUri(uri)
            view?.findViewById<ImageView>(R.id.ivCover)?.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val etTitle = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etPrep = view.findViewById<TextInputEditText>(R.id.etPrepTime)
        val etCook = view.findViewById<TextInputEditText>(R.id.etCookTime)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val ivCover = view.findViewById<ImageView>(R.id.ivCover)

        val btnPickCover = view.findViewById<MaterialButton>(R.id.btnPickCover)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)

        // Build chips
        chipGroup.removeAllViews()
        val categories = resources.getStringArray(R.array.recipe_categories)
        for (label in categories) {
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
            }
            chipGroup.addView(chip)
        }

        // Restore draft -> UI
        val d = vm.state.value.draft
        etTitle.setText(d.title)
        etDescription.setText(d.description)
        etPrep.setText(d.prepTimeMin.toString())
        etCook.setText(d.cookTimeMin.toString())

        // Restore selected categories
        for (i in 0 until chipGroup.childCount) {
            val c = chipGroup.getChildAt(i) as? Chip ?: continue
            c.isChecked = d.categories.contains(c.text.toString())
        }

        // Restore cover
        d.coverUri?.let { uriStr ->
            ivCover.setImageURI(Uri.parse(uriStr))
        }

        btnPickCover.setOnClickListener { pickCover.launch("image/*") }

        btnNext.setOnClickListener {
            // Collect selected categories (בלי children extension)
            val selected = mutableSetOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val c = chipGroup.getChildAt(i) as? Chip ?: continue
                if (c.isChecked) selected.add(c.text.toString())
            }

            val prep = etPrep.text?.toString()?.trim()?.toIntOrNull() ?: 0
            val cook = etCook.text?.toString()?.trim()?.toIntOrNull() ?: 0

            vm.setBasicInfo(
                title = etTitle.text?.toString().orEmpty(),
                description = etDescription.text?.toString().orEmpty(),
                prepTimeMin = prep,
                cookTimeMin = cook,
                categories = selected
            )

            if (!vm.validateStep1()) {
                Toast.makeText(requireContext(), vm.state.value.errorMessage ?: "Invalid", Toast.LENGTH_SHORT).show()
                vm.clearError()
                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_basicInfo_to_ingredients)
        }
    }
}
