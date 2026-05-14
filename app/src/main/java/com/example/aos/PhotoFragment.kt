package com.example.aos

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PhotoFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cropName: String = ""
    private lateinit var predictor: DiseasePredictor

    private val guideLeftRatio = 0.19f
    private val guideTopRatio = 0.22f
    private val guideRightRatio = 0.81f
    private val guideBottomRatio = 0.78f

    companion object {
        fun newInstance(cropName: String): PhotoFragment {
            val fragment = PhotoFragment()
            val args = Bundle()
            args.putString("cropName", cropName)
            fragment.arguments = args
            return fragment
        }

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) startCamera()
            else Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cropName = arguments?.getString("cropName") ?: ""
        previewView = view.findViewById(R.id.previewView)
        view.findViewById<TextView>(R.id.tvCropTitle).text = "${cropName} 잎 촬영"

        predictor = DiseasePredictor(requireContext()).apply {
            initModels()
        }

        view.findViewById<ImageButton>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @Suppress("DEPRECATION")
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val timestamp = System.currentTimeMillis()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${cropName}_${timestamp}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Farm-s-Pharmacy")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return

                    val progress = ProgressDialog(requireContext()).apply {
                        setMessage("진단 중...")
                        setCancelable(false)
                        show()
                    }

                    lifecycleScope.launch {
                        try {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                            val inferenceResult = withContext(Dispatchers.Default) {
                                val bitmap = loadBitmapFromUri(savedUri)
                                val bbox = computeGuideBoxOnBitmap(bitmap.width, bitmap.height)
                                val cropKey = mapCropNameToKey(cropName)
                                val result = predictor.predictWithSam(bitmap, bbox, cropKey)
                                val mapped = mapPrediction(result.className, cropName)
                                val confidence = (result.confidence * 100.0f).roundToInt()
                                InferenceResult(
                                    diagType = mapped.diagType,
                                    label = mapped.label,
                                    confidence = confidence,
                                    sickKeyLookup = mapped.sickKeyLookup
                                )
                            }

                            // 1. Firebase Storage 업로드
                            val imageUrl = withContext(Dispatchers.IO) {
                                val storageRef = FirebaseStorage.getInstance()
                                    .reference
                                    .child("diagnoses/$uid/${timestamp}.jpg")
                                val stream = requireContext().contentResolver.openInputStream(savedUri)
                                    ?: return@withContext null
                                stream.use { storageRef.putStream(it).await() }
                                storageRef.downloadUrl.await().toString()
                            }

                            // 2. SVC01 호출 → sickKey 획득 (DISEASE일 때만)
                            val sickKey = if (inferenceResult.diagType == "DISEASE" && inferenceResult.sickKeyLookup.isNotBlank()) {
                                withContext(Dispatchers.IO) {
                                    DiseaseApiService.getSickKey(cropName, inferenceResult.sickKeyLookup)
                                }
                            } else null

                            // 3. Firestore Diagnoses 저장
                            withContext(Dispatchers.IO) {
                                if (uid.isNotEmpty()) {
                                    val doc = hashMapOf(
                                        "userId"       to uid,
                                        "cropType"     to cropName,
                                        "diseaseName"  to inferenceResult.label,
                                        "confidence"   to inferenceResult.confidence.toDouble() / 100.0,
                                        "imageUrl"     to (imageUrl ?: ""),
                                        "sickKey"      to (sickKey ?: ""),
                                        "timestamp"    to Timestamp.now(),
                                        "isCounted"    to false,
                                        "isHandled"    to false,
                                        "memoTitle"    to "",
                                        "memoContent"  to "",
                                        "location"     to null,
                                        "addressDo"    to null,
                                        "addressSi"    to null
                                    )
                                    FirebaseFirestore.getInstance()
                                        .collection("Diagnoses")
                                        .add(doc)
                                        .await()
                                }
                            }

                            // 4. SharedPreferences 저장 (HistoryActivity 호환 유지)
                            val newItem = HistoryItem(
                                id          = timestamp.toInt(),
                                imageUri    = savedUri.toString(),
                                diseaseName = inferenceResult.label,
                                confidence  = inferenceResult.confidence,
                                date        = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date()),
                                cropName    = cropName,
                                diagType    = inferenceResult.diagType,
                                sickKey     = sickKey ?: ""
                            )
                            val prefs = requireContext().getSharedPreferences("HistoryPrefs", Context.MODE_PRIVATE)
                            val gson = Gson()
                            val current = gson.fromJson(
                                prefs.getString("history_items", "[]"),
                                Array<HistoryItem>::class.java
                            ).toMutableList()
                            current.add(0, newItem)
                            prefs.edit().putString("history_items", gson.toJson(current)).apply()

                            progress.dismiss()

                            // 5. ResultActivity 이동 (sickKey 추가)
                            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                                putExtra("imageUri",   savedUri.toString())
                                putExtra("imageUrl",   imageUrl ?: "")
                                putExtra("cropName",   cropName)
                                putExtra("diagType",   inferenceResult.diagType)
                                putExtra("label",      inferenceResult.label)
                                putExtra("confidence", inferenceResult.confidence)
                                putExtra("sickKey",    sickKey ?: "")
                            }
                            startActivity(intent)

                        } catch (e: Exception) {
                            progress.dismiss()
                            Toast.makeText(requireContext(), "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "촬영 실패: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private data class MappedPrediction(
        val diagType: String,
        val label: String,
        val sickKeyLookup: String,
    )

    private data class InferenceResult(
        val diagType: String,
        val label: String,
        val confidence: Int,
        val sickKeyLookup: String,
    )

    private fun mapCropNameToKey(name: String): String {
        return when (name.trim()) {
            "오이" -> "cucumber"
            "딸기" -> "strawberry"
            "파프리카" -> "paprika"
            "포도" -> "grape"
            "고추" -> "pepper"
            "토마토" -> "tomato"
            else -> name.trim().lowercase()
        }
    }

    private fun mapPrediction(className: String, inputCropKorean: String): MappedPrediction {
        val parts = className.split("_")
        val crop = parts.firstOrNull().orEmpty()
        val condition = parts.drop(1).joinToString("_")
        val predictedCropKorean = when (crop) {
            "cucumber" -> "오이"
            "strawberry" -> "딸기"
            "paprika", "paprica" -> "파프리카"
            "grape" -> "포도"
            "pepper" -> "고추"
            "tomato" -> "토마토"
            else -> ""
        }

        if (predictedCropKorean.isEmpty() || predictedCropKorean != inputCropKorean) {
            return MappedPrediction(
                diagType = "UNKNOWN",
                label = "",
                sickKeyLookup = "",
            )
        }

        if (condition == "healthy") {
            return MappedPrediction(
                diagType = "NORMAL",
                label = "정상",
                sickKeyLookup = "",
            )
        }

        val diseaseLabel = when (condition) {
            "downy" -> "노균병"
            "powdery" -> "흰가루병"
            "graymold" -> "잿빛곰팡이병"
            else -> condition
        }

        return MappedPrediction(
            diagType = "DISEASE",
            label = diseaseLabel,
            sickKeyLookup = diseaseLabel,
        )
    }

    private fun computeGuideBoxOnBitmap(bitmapWidth: Int, bitmapHeight: Int): FloatArray {
        val previewWidth = max(previewView.width, 1)
        val previewHeight = max(previewView.height, 1)

        val left = previewWidth * guideLeftRatio
        val top = previewHeight * guideTopRatio
        val right = previewWidth * guideRightRatio
        val bottom = previewHeight * guideBottomRatio

        val scaleX = bitmapWidth.toFloat() / previewWidth
        val scaleY = bitmapHeight.toFloat() / previewHeight

        val x1 = (left * scaleX).coerceIn(0f, bitmapWidth.toFloat())
        val y1 = (top * scaleY).coerceIn(0f, bitmapHeight.toFloat())
        val x2 = (right * scaleX).coerceIn(0f, bitmapWidth.toFloat())
        val y2 = (bottom * scaleY).coerceIn(0f, bitmapHeight.toFloat())

        return floatArrayOf(
            min(x1, x2),
            min(y1, y2),
            max(x1, x2),
            max(y1, y2),
        )
    }

    private fun loadBitmapFromUri(uri: android.net.Uri): android.graphics.Bitmap {
        val stream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Failed to open image stream")
        stream.use {
            return android.graphics.BitmapFactory.decodeStream(it)
        }
    }
}