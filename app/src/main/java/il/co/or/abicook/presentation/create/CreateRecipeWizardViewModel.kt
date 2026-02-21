package il.co.or.abicook.presentation.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.data.repository.FirestoreRecipeDataRepository
import il.co.or.abicook.data.repository.StorageRepository
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.model.RecipeStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class IngredientField { NAME, AMOUNT, UNIT }

data class IngredientDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: String = "",
    val unit: String = "g"
)

data class StepDraft(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val localImageUri: String? = null,
    val uploadedImageUrl: String? = null
)

data class CreateRecipeDraft(
    val title: String = "",
    val description: String = "",
    val prepTimeMin: Int = 0,
    val cookTimeMin: Int = 0,
    val primaryCategory: String = "",
    val categories: List<String> = emptyList(),

    val coverUri: String? = null,
    val coverImageUrl: String? = null,

    val ingredients: List<IngredientDraft> = listOf(IngredientDraft()),
    val steps: List<StepDraft> = listOf(StepDraft())
)

data class CreateRecipeWizardState(
    val draft: CreateRecipeDraft = CreateRecipeDraft(),
    val isPublishing: Boolean = false,
    val publishSuccess: Boolean = false,
    val errorMessage: String? = null,

    val editingRecipeId: String? = null,
    val isLoadingExisting: Boolean = false
)

class CreateRecipeWizardViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val recipeRepo = FirestoreRecipeDataRepository()
    private val storageRepo = StorageRepository()

    private val _state = MutableStateFlow(CreateRecipeWizardState())
    val state: StateFlow<CreateRecipeWizardState> = _state.asStateFlow()

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun resetAfterPublish() {
        _state.value = CreateRecipeWizardState()
    }

    fun ensureFreshForNew() {
        if (_state.value.editingRecipeId != null) {
            _state.value = CreateRecipeWizardState()
        }
    }

    fun startEdit(recipeId: String) {
        if (_state.value.editingRecipeId == recipeId && _state.value.draft.title.isNotBlank()) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingExisting = true, errorMessage = null, editingRecipeId = recipeId) }

                val doc = db.collection("recipes").document(recipeId).get().await()
                val post = doc.toObject(RecipePost::class.java)?.copy(id = doc.id)
                    ?: throw IllegalStateException("Recipe not found")

                val uid = auth.currentUser?.uid
                if (uid == null || post.authorId != uid) {
                    throw IllegalStateException("You can only edit your own recipes")
                }

                val draft = CreateRecipeDraft(
                    title = post.title,
                    description = post.description,
                    prepTimeMin = post.prepTimeMin,
                    cookTimeMin = post.cookTimeMin,
                    primaryCategory = post.primaryCategory,
                    categories = post.categories,

                    coverUri = null,
                    coverImageUrl = post.imageUrl,

                    ingredients = parseIngredientsSummary(post.ingredientsSummary),
                    steps = post.steps.map { step ->
                        StepDraft(
                            id = UUID.randomUUID().toString(),
                            text = step.text,
                            localImageUri = null,
                            uploadedImageUrl = step.imageUrl
                        )
                    }.ifEmpty { listOf(StepDraft()) }
                )

                _state.update { it.copy(draft = draft, isLoadingExisting = false, errorMessage = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingExisting = false, errorMessage = e.message ?: "Failed loading recipe") }
            }
        }
    }

    // ---------- Step 1 ----------
    fun setCoverUri(uri: Uri?) {
        _state.update { s -> s.copy(draft = s.draft.copy(coverUri = uri?.toString())) }
    }

    fun setBasicInfo(
        title: String,
        description: String,
        prepTimeMin: Int,
        cookTimeMin: Int,
        primaryCategory: String,
        categories: Collection<String>
    ) {
        _state.update { s ->
            s.copy(
                draft = s.draft.copy(
                    title = title.trim(),
                    description = description.trim(),
                    prepTimeMin = prepTimeMin,
                    cookTimeMin = cookTimeMin,
                    primaryCategory = primaryCategory.trim(),
                    categories = categories.toList()
                )
            )
        }
    }

    fun setBasicInfo(
        title: String,
        description: String,
        prepTimeMin: Int,
        cookTimeMin: Int,
        categories: Collection<String>
    ) {
        val primary = _state.value.draft.primaryCategory
            .ifBlank { categories.firstOrNull().orEmpty() }

        setBasicInfo(
            title = title,
            description = description,
            prepTimeMin = prepTimeMin,
            cookTimeMin = cookTimeMin,
            primaryCategory = primary,
            categories = categories
        )
    }

    fun validateStep1(): Boolean {
        val d = _state.value.draft
        val err = when {
            d.title.isBlank() -> "Please enter a title"
            d.primaryCategory.isBlank() -> "Please choose at least one category"
            (d.prepTimeMin + d.cookTimeMin) <= 0 -> "Please set preparation/cooking time"
            else -> null
        }
        _state.update { it.copy(errorMessage = err) }
        return err == null
    }

    // ---------- Step 2 ----------
    fun addIngredient() {
        _state.update { s ->
            s.copy(draft = s.draft.copy(ingredients = s.draft.ingredients + IngredientDraft()))
        }
    }

    fun removeIngredient(index: Int) {
        _state.update { s ->
            val list = s.draft.ingredients.toMutableList()
            if (index in list.indices) list.removeAt(index)
            if (list.isEmpty()) list.add(IngredientDraft())
            s.copy(draft = s.draft.copy(ingredients = list))
        }
    }

    fun updateIngredient(index: Int, field: IngredientField, value: String) {
        _state.update { s ->
            val list = s.draft.ingredients.toMutableList()
            if (index !in list.indices) return@update s

            val current = list[index]
            val updated = when (field) {
                IngredientField.NAME -> current.copy(name = value)
                IngredientField.AMOUNT -> current.copy(amount = value)
                IngredientField.UNIT -> current.copy(unit = value)
            }

            list[index] = updated
            s.copy(draft = s.draft.copy(ingredients = list))
        }
    }

    fun validateStep2(): Boolean {
        val d = _state.value.draft
        val hasAtLeastOne = d.ingredients.any { it.name.isNotBlank() }
        val err = if (!hasAtLeastOne) "Please add at least one ingredient" else null
        _state.update { it.copy(errorMessage = err) }
        return err == null
    }

    // ---------- Step 3 ----------
    fun addStep() {
        _state.update { s ->
            s.copy(draft = s.draft.copy(steps = s.draft.steps + StepDraft()))
        }
    }

    fun removeStep(index: Int) {
        _state.update { s ->
            val list = s.draft.steps.toMutableList()
            if (index in list.indices) list.removeAt(index)
            if (list.isEmpty()) list.add(StepDraft())
            s.copy(draft = s.draft.copy(steps = list))
        }
    }

    fun updateStepText(index: Int, value: String) {
        _state.update { s ->
            val list = s.draft.steps.toMutableList()
            if (index !in list.indices) return@update s
            list[index] = list[index].copy(text = value)
            s.copy(draft = s.draft.copy(steps = list))
        }
    }

    fun setStepImage(index: Int, uri: Uri?) {
        _state.update { s ->
            val list = s.draft.steps.toMutableList()
            if (index !in list.indices) return@update s
            list[index] = list[index].copy(localImageUri = uri?.toString(), uploadedImageUrl = null)
            s.copy(draft = s.draft.copy(steps = list))
        }
    }

    fun clearStepImage(index: Int) {
        _state.update { s ->
            val list = s.draft.steps.toMutableList()
            if (index !in list.indices) return@update s
            list[index] = list[index].copy(localImageUri = null, uploadedImageUrl = null)
            s.copy(draft = s.draft.copy(steps = list))
        }
    }

    fun validateStep3(): Boolean {
        val d = _state.value.draft
        val hasAtLeastOne = d.steps.any { it.text.isNotBlank() || !it.localImageUri.isNullOrBlank() || !it.uploadedImageUrl.isNullOrBlank() }
        val err = if (!hasAtLeastOne) "Please add at least one step" else null
        _state.update { it.copy(errorMessage = err) }
        return err == null
    }

    fun publishRecipe() {
        if (!validateStep1() || !validateStep2() || !validateStep3()) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isPublishing = true, errorMessage = null, publishSuccess = false) }

                val user = auth.currentUser ?: throw IllegalStateException("Not logged in")

                val editingId = _state.value.editingRecipeId
                val recipeId = editingId ?: UUID.randomUUID().toString()

                val existing: RecipePost? = if (editingId != null) {
                    val doc = db.collection("recipes").document(editingId).get().await()
                    doc.toObject(RecipePost::class.java)?.copy(id = doc.id)
                } else null

                if (editingId != null && existing?.authorId != user.uid) {
                    throw IllegalStateException("You can only edit your own recipes")
                }

                var coverUrl: String? = existing?.imageUrl
                _state.value.draft.coverUri?.let { uriStr ->
                    val res = storageRepo.uploadRecipeCoverImage(recipeId, Uri.parse(uriStr))
                    coverUrl = res.downloadUrl
                }

                val stepsUploaded = _state.value.draft.steps.map { step ->
                    if (step.localImageUri.isNullOrBlank()) step
                    else {
                        val res = storageRepo.uploadRecipeStepImage(
                            recipeId,
                            step.id,
                            Uri.parse(step.localImageUri)
                        )
                        step.copy(uploadedImageUrl = res.downloadUrl)
                    }
                }

                val validSteps = stepsUploaded
                    .filter { it.text.isNotBlank() || !it.uploadedImageUrl.isNullOrBlank() }

                val stepsForPost = validSteps.map { s ->
                    RecipeStep(
                        text = s.text,
                        imageUrl = s.uploadedImageUrl
                    )
                }

                val stepsSummaryText = validSteps
                    .mapIndexed { i, s -> "${i + 1}. ${s.text.ifBlank { "(step)" }}" }
                    .joinToString("\n")

                val d = _state.value.draft
                val ingredientsSummary = d.ingredients
                    .filter { it.name.isNotBlank() }
                    .joinToString("\n") { "${it.amount} ${it.unit} - ${it.name}".trim() }

                val post = RecipePost(
                    id = recipeId,
                    title = d.title,
                    description = d.description,
                    prepTimeMin = d.prepTimeMin,
                    cookTimeMin = d.cookTimeMin,
                    primaryCategory = d.primaryCategory,
                    categories = d.categories,

                    ingredientsSummary = ingredientsSummary,
                    stepsSummary = stepsSummaryText,
                    steps = stepsForPost,

                    imageUrl = coverUrl,
                    authorId = existing?.authorId ?: user.uid,
                    authorName = existing?.authorName ?: (user.displayName ?: "User"),

                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                    likes = existing?.likes ?: 0,
                    commentsCount = existing?.commentsCount ?: 0
                )

                if (editingId == null) {
                    recipeRepo.createRecipe(post)
                } else {
                    db.collection("recipes").document(recipeId).set(post).await()
                }

                _state.update { it.copy(isPublishing = false, publishSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isPublishing = false, errorMessage = e.message ?: "Publish failed") }
            }
        }
    }

    private fun parseIngredientsSummary(summary: String?): List<IngredientDraft> {
        if (summary.isNullOrBlank()) return listOf(IngredientDraft())

        val lines = summary.lines().map { it.trim() }.filter { it.isNotBlank() }
        val parsed = lines.map { line ->
            val parts = line.split("-").map { it.trim() }
            val left = parts.getOrNull(0).orEmpty()
            val name = parts.getOrNull(1).orEmpty().ifBlank { line }

            val leftTokens = left.split(" ").filter { it.isNotBlank() }
            val amount = leftTokens.getOrNull(0).orEmpty()
            val unit = leftTokens.getOrNull(1).orEmpty().ifBlank { "g" }

            IngredientDraft(name = name, amount = amount, unit = unit)
        }

        return if (parsed.isNotEmpty()) parsed else listOf(IngredientDraft())
    }
}