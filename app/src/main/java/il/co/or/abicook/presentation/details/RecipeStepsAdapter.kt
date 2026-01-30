package il.co.or.abicook.presentation.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import il.co.or.abicook.R
import il.co.or.abicook.domain.model.RecipeStep

class RecipeStepsAdapter : RecyclerView.Adapter<RecipeStepsAdapter.VH>() {

    private var items: List<RecipeStep> = emptyList()

    fun submitList(list: List<RecipeStep>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_step, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStep = itemView.findViewById<TextView>(R.id.tvStepText)
        private val iv = itemView.findViewById<ImageView>(R.id.ivStepImage)

        fun bind(step: RecipeStep, index: Int) {
            tvStep.text = "${index + 1}. ${step.text}"

            val url = step.imageUrl
            if (url.isNullOrBlank()) {
                iv.visibility = View.GONE
            } else {
                iv.visibility = View.VISIBLE
                Glide.with(iv)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(iv)
            }
        }
    }
}
