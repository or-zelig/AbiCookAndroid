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

class RecipeStepsPagerAdapter : RecyclerView.Adapter<RecipeStepsPagerAdapter.VH>() {

    private var items: List<RecipeStep> = emptyList()

    fun submitList(list: List<RecipeStep>) {
        items = list
        notifyDataSetChanged()
    }

    fun getCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_step_pager, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv = itemView.findViewById<TextView>(R.id.tvStepText)
        private val iv = itemView.findViewById<ImageView>(R.id.ivStepImage)

        fun bind(step: RecipeStep, index: Int) {
            tv.text = "${index + 1}. ${step.text.ifBlank { "(step)" }}"

            val url = step.imageUrl
            Glide.with(iv)
                .load(url)
                .placeholder(R.drawable.ic_launcher_background) // ✅ דיפולטית
                .error(R.drawable.ic_launcher_background)
                .into(iv)
        }
    }
}
