package il.co.or.abicook.presentation.create

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import il.co.or.abicook.R
import kotlinx.coroutines.launch

class CreateRecipeBasicInfoFragment : Fragment(R.layout.fragment_create_recipe_basic_info) {

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

        val editRecipeId = arguments?.getString("editRecipeId")

        // תפעול מצב (רק פעם אחת)
        if (savedInstanceState == null) {
            if (!editRecipeId.isNullOrBlank()) vm.startEdit(editRecipeId)
            else vm.ensureFreshForNew()
        }

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
            chipGroup.addView(
                Chip(requireContext()).apply {
                    text = label
                    isCheckable = true
                }
            )
        }

        btnPickCover.setOnClickListener { pickCover.launch("image/*") }

        // ✅ הכי חשוב: להאזין ל-state ולהזין UI כל שינוי
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->

                    // אם טוענים מתכון לעריכה - אפשר לנעול כפתורים/להציג מצב
                    btnNext.isEnabled = !s.isLoadingExisting
                    btnPickCover.isEnabled = !s.isLoadingExisting

                    val d = s.draft

                    // עדכון שדות
                    if (etTitle.text?.toString() != d.title) etTitle.setText(d.title)
                    if (etDescription.text?.toString() != d.description) etDescription.setText(d.description)
                    if (etPrep.text?.toString() != d.prepTimeMin.toString()) etPrep.setText(d.prepTimeMin.toString())
                    if (etCook.text?.toString() != d.cookTimeMin.toString()) etCook.setText(d.cookTimeMin.toString())

                    // categories
                    for (i in 0 until chipGroup.childCount) {
                        val c = chipGroup.getChildAt(i) as? Chip ?: continue
                        val shouldBeChecked = d.categories.contains(c.text.toString())
                        if (c.isChecked != shouldBeChecked) c.isChecked = shouldBeChecked
                    }

                    // cover:
                    // אם יש coverUri מקומי -> להציג אותו
                    // אחרת אם יש coverImageUrl (ב-edit) -> להציג עם Glide
                    val localUri = d.coverUri?.trim()
                    val url = d.coverImageUrl?.trim()

                    when {
                        !localUri.isNullOrBlank() -> {
                            ivCover.setImageURI(Uri.parse(localUri))
                        }
                        !url.isNullOrBlank() -> {
                            Glide.with(ivCover)
                                .load(url)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .into(ivCover)
                        }
                        else -> {
                            Glide.with(ivCover).clear(ivCover)
                            ivCover.setImageResource(R.drawable.ic_launcher_background)
                        }
                    }

                    // errors
                    s.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        vm.clearError()
                    }
                }
            }
        }

        btnNext.setOnClickListener {
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