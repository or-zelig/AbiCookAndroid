package il.co.or.abicook.presentation.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import il.co.or.abicook.data.repository.FirestoreRecipeDataRepository
import il.co.or.abicook.data.repository.StorageRepository
import il.co.or.abicook.domain.model.RecipePost
import il.co.or.abicook.domain.model.RecipeStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val localImageUri: String? = null,   // content://...
    val uploadedImageUrl: String? = null // https://...
)

data class CreateRecipeDraft(
    val title: String = "",
    val description: String = "",
    val prepTimeMin: Int = 0,
    val cookTimeMin: Int = 0,
    val primaryCategory: String = "",
    val categories: List<String> = emptyList(),

    val coverUri: String? = null, // content://...
    val coverImageUrl: String? = null,

    val ingredients: List<IngredientDraft> = listOf(IngredientDraft()),
    val steps: List<StepDraft> = listOf(StepDraft())
)

data class CreateRecipeWizardState(
    val draft: CreateRecipeDraft = CreateRecipeDraft(),
    val isPublishing: Boolean = false,
    val publishSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateRecipeWizardViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val recipeRepo = FirestoreRecipeDataRepository()
    private val storageRepo = StorageRepository()

    private val _state = MutableStateFlow(CreateRecipeWizardState())
    val state: StateFlow<CreateRecipeWizardState> = _state.asStateFlow()

    // ---------- Common UI helpers ----------
    fun clearError() = _state.update { it.copy(errorMessage = null) }
    fun consumePublishSuccess() = _state.update { it.copy(publishSuccess = false) }

    // ✅ חדש: איפוס draft (רק יצירת מתכון)
    fun resetDraft() {
        _state.value = CreateRecipeWizardState()
    }

    // ✅ חדש: איפוס מלא (כרגע זה אותו דבר כמו resetDraft, אבל נשאר ברור סמנטית ל-Logout)
    fun resetAll() {
        resetDraft()
    }

    /**
     * ✅ חדש: אחרי publish – לאפס את הטופס + לנקות publishSuccess
     * (כדי שהפעם הבאה שתפתח את ה-wizard תהיה נקייה)
     */
    fun resetAfterPublish() {
        _state.value = CreateRecipeWizardState(
            draft = CreateRecipeDraft(),
            isPublishing = false,
            publishSuccess = false,
            errorMessage = null
        )
    }

    // ---------- Step 1: basic info ----------
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

    // ---------- Step 2: ingredients ----------
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

    // ---------- Step 3: steps ----------
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
        val hasAtLeastOne = d.steps.any { it.text.isNotBlank() || !it.localImageUri.isNullOrBlank() }
        val err = if (!hasAtLeastOne) "Please add at least one step" else null
        _state.update { it.copy(errorMessage = err) }
        return err == null
    }

    // ---------- Publish ----------
    fun publishRecipe() {
        if (!validateStep1() || !validateStep2() || !validateStep3()) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isPublishing = true, errorMessage = null, publishSuccess = false) }

                val user = auth.currentUser ?: throw IllegalStateException("Not logged in")
                val recipeId = UUID.randomUUID().toString()

                // 1) Upload cover if exists
                var coverUrl: String? = null
                _state.value.draft.coverUri?.let { uriStr ->
                    val res = storageRepo.uploadRecipeCoverImage(recipeId, Uri.parse(uriStr))
                    coverUrl = res.downloadUrl
                }

                // 2) Upload step images
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

                // ✅ keep steps that have text OR image
                val validSteps = stepsUploaded
                    .filter { it.text.isNotBlank() || !it.uploadedImageUrl.isNullOrBlank() }

                // ✅ steps list for details (with images)
                val stepsForPost = validSteps.map { s ->
                    RecipeStep(
                        text = s.text,
                        imageUrl = s.uploadedImageUrl
                    )
                }

                // ✅ summary text fallback
                val stepsSummaryText = validSteps
                    .mapIndexed { i, s -> "${i + 1}. ${s.text.ifBlank { "(step)" }}" }
                    .joinToString("\n")

                val d = _state.value.draft
                val post = RecipePost(
                    id = recipeId,
                    title = d.title,
                    description = d.description,
                    prepTimeMin = d.prepTimeMin,
                    cookTimeMin = d.cookTimeMin,
                    primaryCategory = d.primaryCategory,
                    categories = d.categories,

                    ingredientsSummary = d.ingredients
                        .filter { it.name.isNotBlank() }
                        .joinToString("\n") { "${it.amount} ${it.unit} - ${it.name}" },

                    stepsSummary = stepsSummaryText,
                    steps = stepsForPost,              // ✅ THIS is the important part

                    imageUrl = coverUrl,
                    authorId = user.uid,
                    authorName = user.displayName ?: "User",
                    createdAtMillis = System.currentTimeMillis(),
                    likes = 0,
                    commentsCount = 0
                )

                // Save to Firestore
                recipeRepo.createRecipe(post)

                _state.update { it.copy(isPublishing = false, publishSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isPublishing = false, errorMessage = e.message ?: "Publish failed") }
            }
        }
    }
}
