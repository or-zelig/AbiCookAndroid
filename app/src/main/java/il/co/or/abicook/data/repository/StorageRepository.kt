package il.co.or.abicook.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

data class UploadResult(
    val downloadUrl: String,
    val path: String
)

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    init {
        val optBucket = FirebaseApp.getInstance().options.storageBucket
        val sdkBucket = storage.reference.bucket
        Log.d("StorageRepo", "options.storageBucket=$optBucket | sdkBucket=$sdkBucket")

        storage.maxUploadRetryTimeMillis = 60_000
        storage.maxOperationRetryTimeMillis = 60_000
    }

    suspend fun uploadRecipeCover(recipeId: String, localUri: Uri): UploadResult {
        val ref = storage.reference
            .child("recipes")
            .child(recipeId)
            .child("cover.jpg")

        val snap = ref.putFile(localUri).await()

        val url = try {
            snap.storage.downloadUrl.await().toString()
        } catch (e: Exception) {
            delay(300)
            snap.storage.downloadUrl.await().toString()
        }

        return UploadResult(downloadUrl = url, path = ref.path)
    }

    suspend fun uploadStepImage(recipeId: String, stepId: String, localUri: Uri): UploadResult {
        val ref = storage.reference
            .child("recipes")
            .child(recipeId)
            .child("steps")
            .child("$stepId.jpg")

        val snap = ref.putFile(localUri).await()

        val url = try {
            snap.storage.downloadUrl.await().toString()
        } catch (e: Exception) {
            delay(300)
            snap.storage.downloadUrl.await().toString()
        }

        return UploadResult(downloadUrl = url, path = ref.path)
    }

    suspend fun uploadRecipeCoverImage(recipeId: String, localUri: Uri): UploadResult =
        uploadRecipeCover(recipeId, localUri)

    suspend fun uploadRecipeStepImage(recipeId: String, stepId: String, localUri: Uri): UploadResult =
        uploadStepImage(recipeId, stepId, localUri)
}
