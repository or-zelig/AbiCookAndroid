package il.co.or.abicook.presentation.create

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import il.co.or.abicook.R

class IngredientsAdapter(
    private val onChange: (index: Int, field: IngredientField, value: String) -> Unit,
    private val onRemove: (index: Int) -> Unit
) : ListAdapter<IngredientDraft, IngredientsAdapter.VH>(Diff) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    object Diff : DiffUtil.ItemCallback<IngredientDraft>() {
        override fun areItemsTheSame(oldItem: IngredientDraft, newItem: IngredientDraft) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: IngredientDraft, newItem: IngredientDraft) =
            oldItem == newItem
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val etName: TextInputEditText = v.findViewById(R.id.etIngredientName)
        private val etAmount: TextInputEditText = v.findViewById(R.id.etIngredientAmount)
        private val etUnit: MaterialAutoCompleteTextView = v.findViewById(R.id.etIngredientUnit)
        private val btnRemove: MaterialButton = v.findViewById(R.id.btnRemoveIngredient)

        private var initialized = false
        private var suppress = false

        fun bind(item: IngredientDraft) {
            // אל תעשה setText בזמן שהמשתמש מקליד (זה מה שגורם לקפיצת cursor)
            suppress = true
            if (!etName.hasFocus() && etName.text?.toString() != item.name) etName.setText(item.name)
            if (!etAmount.hasFocus() && etAmount.text?.toString() != item.amount) etAmount.setText(item.amount)
            if (!etUnit.hasFocus() && etUnit.text?.toString() != item.unit) etUnit.setText(item.unit, false)
            suppress = false

            if (!initialized) {
                initialized = true

                // Units dropdown
                val units = itemView.resources.getStringArray(R.array.ingredient_units).toList()
                etUnit.setAdapter(
                    ArrayAdapter(itemView.context, android.R.layout.simple_list_item_1, units)
                )
                etUnit.setOnClickListener { etUnit.showDropDown() }

                etName.doAfterTextChanged {
                    if (suppress) return@doAfterTextChanged
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) onChange(pos, IngredientField.NAME, it?.toString().orEmpty())
                }

                etAmount.doAfterTextChanged {
                    if (suppress) return@doAfterTextChanged
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) onChange(pos, IngredientField.AMOUNT, it?.toString().orEmpty())
                }

                etUnit.doAfterTextChanged {
                    if (suppress) return@doAfterTextChanged
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) onChange(pos, IngredientField.UNIT, it?.toString().orEmpty())
                }

                btnRemove.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) onRemove(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_create_ingredient, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
