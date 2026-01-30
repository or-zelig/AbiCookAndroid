package il.co.or.abicook.presentation.create

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import il.co.or.abicook.R

class StepsAdapter(
    private val onTextChanged: (index: Int, value: String) -> Unit,
    private val onPickImage: (index: Int) -> Unit,
    private val onRemoveImage: (index: Int) -> Unit,
    private val onRemoveStep: (index: Int) -> Unit
) : ListAdapter<StepDraft, StepsAdapter.VH>(Diff) {

    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    object Diff : DiffUtil.ItemCallback<StepDraft>() {
        override fun areItemsTheSame(old: StepDraft, new: StepDraft) = old.id == new.id
        override fun areContentsTheSame(old: StepDraft, new: StepDraft) = old == new
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_create_step_with_image, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val etStep: TextInputEditText = itemView.findViewById(R.id.etStepText)
        private val img: ImageView = itemView.findViewById(R.id.imgStep)

        private val btnPick: MaterialButton = itemView.findViewById(R.id.btnPickImage)
        private val btnRemoveImg: MaterialButton = itemView.findViewById(R.id.btnRemoveImage)
        private val btnRemoveStep: MaterialButton = itemView.findViewById(R.id.btnRemoveStep)

        private var watcher: TextWatcher? = null
        private var isBinding = false

        fun bind(item: StepDraft) {
            // ---- text (no cursor jump) ----
            watcher?.let { etStep.removeTextChangedListener(it) }

            val newText = item.text
            val currentText = etStep.text?.toString().orEmpty()

            if (currentText != newText) {
                val hadFocus = etStep.hasFocus()
                val sel = etStep.selectionStart.coerceAtLeast(0)

                isBinding = true
                etStep.setText(newText)
                isBinding = false

                if (hadFocus) {
                    etStep.setSelection(sel.coerceAtMost(newText.length))
                } else {
                    etStep.setSelection(newText.length)
                }
            }

            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isBinding) return
                    val pos = adapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    onTextChanged(pos, s?.toString().orEmpty())
                }
            }
            etStep.addTextChangedListener(watcher)

            // ---- image ----
            val uri = item.localImageUri?.let { Uri.parse(it) }
            if (uri != null) {
                img.visibility = View.VISIBLE
                img.setImageURI(uri)
                btnRemoveImg.visibility = View.VISIBLE
            } else {
                img.visibility = View.GONE
                btnRemoveImg.visibility = View.GONE
            }

            btnPick.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onPickImage(pos)
            }
            btnRemoveImg.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveImage(pos)
            }
            btnRemoveStep.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveStep(pos)
            }
        }
    }
}
