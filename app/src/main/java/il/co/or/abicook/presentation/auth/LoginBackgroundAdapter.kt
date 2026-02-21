package il.co.or.abicook.presentation.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import il.co.or.abicook.R

class LoginBackgroundAdapter(
    private val images: List<String>
) : RecyclerView.Adapter<LoginBackgroundAdapter.ImageVH>() {

    inner class ImageVH(val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
        val image = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background_image, parent, false) as ImageView
        return ImageVH(image)
    }

    override fun onBindViewHolder(holder: ImageVH, position: Int) {
        Glide.with(holder.itemView.context)
            .load(images[position])
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount() = images.size
}