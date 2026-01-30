package il.co.or.abicook.presentation.details

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import il.co.or.abicook.R
import il.co.or.abicook.data.repository.FirestoreRecipeDetailsRepository
import il.co.or.abicook.domain.model.RecipePost
import androidx.viewpager2.widget.ViewPager2

class RecipeDetailsFragment : Fragment(R.layout.fragment_recipe_details) {

    private val vm: RecipeDetailsViewModel by viewModels {
        RecipeDetailsViewModelFactory(FirestoreRecipeDetailsRepository())
    }

    // ✅ moved to fragment scope
    private lateinit var rvSteps: RecyclerView;
    private val stepsAdapter = RecipeStepsAdapter();

    private lateinit var stepsPagerContainer: View
    private lateinit var vpSteps: ViewPager2
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private val pagerAdapter = RecipeStepsPagerAdapter()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = requireArguments().getString("recipeId")
            ?: run {
                Toast.makeText(requireContext(), "Missing recipeId", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return
            }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val ivRecipe = view.findViewById<ImageView>(R.id.ivRecipe)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvAuthor = view.findViewById<TextView>(R.id.tvAuthor)
        val tvTimes = view.findViewById<TextView>(R.id.tvTimes)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val btnLike = view.findViewById<MaterialButton>(R.id.btnLike)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val tvIngredientsValue = view.findViewById<TextView>(R.id.tvIngredientsValue)
        val tvStepsValue = view.findViewById<TextView>(R.id.tvStepsValue)
        vpSteps = view.findViewById(R.id.vpSteps)
        btnPrev = view.findViewById(R.id.btnPrevStep)
        btnNext = view.findViewById(R.id.btnNextStep)
        stepsPagerContainer = view.findViewById(R.id.stepsPagerContainer)

        vpSteps.adapter = pagerAdapter
        vpSteps.isUserInputEnabled = false


        // ✅ use fragment property
        rvSteps = view.findViewById(R.id.rvSteps)
        rvSteps.layoutManager = LinearLayoutManager(requireContext())
        rvSteps.isNestedScrollingEnabled = false
        rvSteps.adapter = stepsAdapter

        btnLike.setOnClickListener { vm.toggleLike() }
        btnPrev.setOnClickListener {
            val i = vpSteps.currentItem
            if (i > 0) vpSteps.currentItem = i - 1
        }
        btnNext.setOnClickListener {
            val i = vpSteps.currentItem
            val total = pagerAdapter.getCount()
            if (i < total - 1) vpSteps.currentItem = i + 1
        }

        vpSteps.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateArrowState(pagerAdapter.getCount(), position)
            }
        })


        vm.state.observe(viewLifecycleOwner) { s ->
            s.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
            val r = s.recipe ?: return@observe

            bindRecipe(
                r = r,
                ivRecipe = ivRecipe,
                tvTitle = tvTitle,
                tvAuthor = tvAuthor,
                tvTimes = tvTimes,
                chipGroup = chipGroup,
                btnLike = btnLike,
                tvDescription = tvDescription,
                tvIngredientsValue = tvIngredientsValue,
                tvStepsValue = tvStepsValue
            )
        }

        vm.start(recipeId)
    }

    private fun updateArrowState(total: Int, index: Int) {
        if (total <= 1) {
            btnPrev.visibility = View.INVISIBLE
            btnNext.visibility = View.INVISIBLE
            return
        }
        btnPrev.visibility = if (index == 0) View.INVISIBLE else View.VISIBLE
        btnNext.visibility = if (index == total - 1) View.INVISIBLE else View.VISIBLE
    }


    private fun bindRecipe(
        r: RecipePost,
        ivRecipe: ImageView,
        tvTitle: TextView,
        tvAuthor: TextView,
        tvTimes: TextView,
        chipGroup: ChipGroup,
        btnLike: MaterialButton,
        tvDescription: TextView,
        tvIngredientsValue: TextView,
        tvStepsValue: TextView
    ) {
        tvTitle.text = r.title
        tvAuthor.text = "by ${r.authorName}"

        val total = (r.prepTimeMin + r.cookTimeMin)
        tvTimes.text = "Prep: ${r.prepTimeMin} • Cook: ${r.cookTimeMin} • Total: $total"

        chipGroup.removeAllViews()
        val cats = if (r.categories.isNotEmpty()) r.categories else listOf(r.primaryCategory).filter { it.isNotBlank() }
        for (c in cats.distinct()) {
            chipGroup.addView(
                Chip(requireContext()).apply {
                    text = c
                    isCheckable = false
                }
            )
        }

        val likeText = if (r.isLikedByMe) "UNLIKE" else "LIKE"
        btnLike.text = "$likeText (${r.likes})"

        tvDescription.text = r.description
        tvIngredientsValue.text = r.ingredientsSummary

        Glide.with(ivRecipe)
            .load(r.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(ivRecipe)

        if (r.steps.isNotEmpty()) {
            tvStepsValue.visibility = View.GONE
            stepsPagerContainer.visibility = View.VISIBLE

            pagerAdapter.submitList(r.steps)
            vpSteps.setCurrentItem(0, false)
            updateArrowState(r.steps.size, 0)
        } else {
            stepsPagerContainer.visibility = View.GONE
            tvStepsValue.visibility = View.VISIBLE
            tvStepsValue.text = r.stepsSummary
        }

    }
}
