package il.co.or.abicook.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import il.co.or.abicook.R
import il.co.or.abicook.domain.model.RecipePost
import com.bumptech.glide.Glide

class RecipePostAdapter(
    private val onItemClick: ((RecipePost) -> Unit)? = null
) : ListAdapter<RecipePost, RecipePostAdapter.PostViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RecipePost>() {
        override fun areItemsTheSame(oldItem: RecipePost, newItem: RecipePost): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecipePost, newItem: RecipePost): Boolean =
            oldItem == newItem
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)

        fun bind(item: RecipePost) {

            val url = item.imageUrl
            if (url.isNullOrBlank()) {
                Glide.with(ivImage).clear(ivImage)
                ivImage.setImageResource(R.drawable.ic_launcher_background)
            } else {
                Glide.with(ivImage)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivImage)
            }

            tvTitle.text = item.title
            tvAuthor.text = "by ${item.authorName}"
            tvMeta.text = "${item.likes} likes • ${item.commentsCount} comments"

            itemView.setOnClickListener { onItemClick?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
