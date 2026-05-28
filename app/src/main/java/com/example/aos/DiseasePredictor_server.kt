@file:Suppress("unused")

package com.example.aos

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Retrofit API interface for disease prediction backend.
 *
 * Expected multipart form fields (based on main.py /predict endpoint):
 * - image: UploadFile (binary image)
 * - crop: str (crop name, e.g., "tomato", "potato")
 * - bbox: str (bounding box format "x1,y1,x2,y2")
 */
interface DiseasePredictApi {

    @Multipart
    @POST("/predict")
    suspend fun predict(
        @Part image: MultipartBody.Part,
        @Part("crop") crop: okhttp3.RequestBody,
        @Part("bbox") bbox: okhttp3.RequestBody
    ): Response<PredictResponse>
}

/**
 * Single prediction result from the backend.
 * Will be populated based on actual backend response.
 */
data class ServerPredictionResult(
    val className: String? = null,
    val confidence: Float? = null
)

/**
 * Main API response model for /predict endpoint.
 * Structure will be confirmed after first successful test.
 */
data class PredictResponse(
    val success: Boolean = false,
    val message: String? = null,
    val crop: String? = null,
    val bbox: String? = null,
    val predictions: List<ServerPredictionResult> = emptyList(),
    
    // Fallback fields for common response patterns
    val disease: String? = null,
    val confidence: Float? = null,
    val description: String? = null
)

/**
 * Disease predictor server client.
 * Communicates with FastAPI backend for disease classification.
 *
 * Usage:
 * val result = DiseasePredictor_server.predict(
 *     context = this,
 *     bitmap = capturedBitmap,
 *     cropName = "tomato"
 * )
 */
class DiseasePredictor_server {
    companion object {
        private const val TAG = "DiseasePredictorServer"
        
        // ⚠️ UPDATE THIS URL when ngrok tunnel changes
        const val BACKEND_URL = "https://trio-handed-promptly.ngrok-free.dev/"

        private val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "OkHttp: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BACKEND_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        private val api: DiseasePredictApi by lazy {
            retrofit.create(DiseasePredictApi::class.java)
        }

        /**
         * Sends captured bitmap + crop name to backend for disease prediction.
         *
         * bbox is automatically calculated as the full image:
         * "0,0,width-1,height-1"
         *
         * @param context Android context (required for temporary file creation)
         * @param bitmap Captured image bitmap from camera
         * @param cropName Crop name selected by user (e.g., "tomato", "potato")
         * @return PredictResponse containing predictions from backend
         * @throws IOException Network or file I/O errors
         * @throws IllegalArgumentException Invalid input parameters
         * @throws HttpException Server returned error status
         */
        suspend fun predict(
            context: Context,
            bitmap: Bitmap,
            cropName: String
        ): PredictResponse = withContext(Dispatchers.IO) {
            
            // Validate inputs
            require(cropName.isNotBlank()) {
                "Crop name must not be empty."
            }

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                throw IllegalArgumentException(
                    "Bitmap is invalid: width=${bitmap.width}, height=${bitmap.height}. " +
                    "Both must be greater than 0."
                )
            }

            // Calculate bbox for full image
            val bbox = "0,0,${bitmap.width - 1},${bitmap.height - 1}"
            Log.d(
                TAG,
                "Preparing prediction request. cropName='$cropName', " +
                "bitmap=${bitmap.width}x${bitmap.height}, bbox='$bbox'"
            )

            var imageFile: File? = null

            try {
                // Convert bitmap to temporary JPEG file
                imageFile = bitmapToFile(context, bitmap)
                Log.d(TAG, "Bitmap file created: ${imageFile.absolutePath}")

                // Create multipart image part
                val imageRequestBody = imageFile
                    .asRequestBody("image/jpeg".toMediaType())

                val imagePart = MultipartBody.Part.createFormData(
                    name = "image",
                    filename = imageFile.name,
                    body = imageRequestBody
                )

                // Create form field for crop name
                val cropBody = cropName
                    .trim()
                    .lowercase()
                    .toRequestBody("text/plain".toMediaType())

                // Create form field for bbox
                val bboxBody = bbox
                    .toRequestBody("text/plain".toMediaType())

                Log.d(TAG, "Sending POST request to /predict...")

                // Call backend API
                val response = api.predict(
                    image = imagePart,
                    crop = cropBody,
                    bbox = bboxBody
                )

                // Handle response
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.i(TAG, "✓ Prediction successful. Message: ${body.message}")
                        return@withContext body
                    } else {
                        Log.e(TAG, "✗ Empty response body from server")
                        throw IOException("Server returned empty response body.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(
                        TAG,
                        "✗ HTTP ${response.code()}: ${response.message()}\n$errorBody"
                    )
                    throw HttpException(response)
                }

            } catch (e: HttpException) {
                Log.e(TAG, "✗ HTTP Exception: ${e.code()} ${e.message()}", e)
                throw IOException(
                    "Server error (${e.code()}). Please check your connection and try again.",
                    e
                )
            } catch (e: IOException) {
                Log.e(TAG, "✗ Network/IO Exception: ${e.message}", e)
                throw IOException(
                    "Network error: ${e.message}. Check your internet connection.",
                    e
                )
            } catch (e: Exception) {
                Log.e(TAG, "✗ Unexpected Exception: ${e.message}", e)
                throw Exception("Unexpected error: ${e.message}", e)
            } finally {
                // Clean up temporary file
                if (imageFile != null && imageFile.exists()) {
                    val deleted = imageFile.delete()
                    Log.d(TAG, "Temp file cleanup: deleted=$deleted, path=${imageFile.absolutePath}")
                }
            }
        }

        /**
         * Converts bitmap to a temporary JPEG file for multipart upload.
         * File is stored in app cache directory and should be cleaned up after use.
         */
        private fun bitmapToFile(context: Context, bitmap: Bitmap): File {
            val fileName = "predict_${UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, fileName)

            try {
                FileOutputStream(file).use { outputStream ->
                    val compressed = bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,  // Quality: 95%
                        outputStream
                    )
                    outputStream.flush()

                    if (!compressed) {
                        throw IOException("Failed to compress bitmap to JPEG format.")
                    }
                }
                Log.d(TAG, "Bitmap compressed to JPEG: ${file.absolutePath} (${file.length()} bytes)")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to convert bitmap to file", e)
                throw IOException("Bitmap conversion failed: ${e.message}", e)
            }

            return file
        }

        /**
         * Helper to create text/plain request bodies.
         */
        private fun String.toRequestBody(mediaType: okhttp3.MediaType) =
            okhttp3.RequestBody.create(mediaType, this)
    }
}
