package com.example.aos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cropName: String = ""

    companion object {
        fun newInstance(cropName: String): PhotoFragment {
            val fragment = PhotoFragment()
            val args = Bundle()
            args.putString("cropName", cropName)
            fragment.arguments = args
            return fragment
        }

        private val REQUIRED_PERMISSIONS = mutableListOf(
            android.Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

        // 타이틀 설정
        view.findViewById<TextView>(R.id.tvCropTitle).text = "${cropName} 잎 촬영"

        // 촬영 버튼
        view.findViewById<ImageButton>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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

    @SuppressLint("MissingPermission")
    private fun getLocationAndStart(imageUrl: String, localUri: android.net.Uri) {
        val fusedClient = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(requireContext())

        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                // 역지오코딩 → addressDo, addressSi 추출
                val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.KOREA)
                var addressDo = ""
                var addressSi = ""

                if (lat != 0.0) {
                    try {
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            addressDo = addr.adminArea ?: ""      // 예: "전라남도"
                            addressSi = addr.locality             // 예: "광주시"
                                ?: addr.subAdminArea ?: ""
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // ── 3) ResultActivity로 모든 데이터 전달 ────────
                val diagType   = "DISEASE"   // TODO: 실제 모델 결과
                val label      = "노균병"    // TODO: 실제 모델 결과
                val confidence = 92          // TODO: 실제 모델 결과

                val intent = android.content.Intent(requireContext(), ResultActivity::class.java).apply {
                    putExtra("imageUri",   localUri.toString())  // 화면 표시용 (로컬)
                    putExtra("imageUrl",   imageUrl)             // Firestore 저장용 (Storage URL)
                    putExtra("cropName",   cropName)
                    putExtra("diagType",   diagType)
                    putExtra("label",      label)
                    putExtra("confidence", confidence)
                    putExtra("latitude",   lat)
                    putExtra("longitude",  lng)
                    putExtra("addressDo",  addressDo)
                    putExtra("addressSi",  addressSi)
                }
                startActivity(intent)
            }
            .addOnFailureListener {
                // 위치 수집 실패해도 그냥 진행
                val intent = android.content.Intent(requireContext(), ResultActivity::class.java).apply {
                    putExtra("imageUri",   localUri.toString())
                    putExtra("imageUrl",   imageUrl)
                    putExtra("cropName",   cropName)
                    putExtra("diagType",   "DISEASE")
                    putExtra("label",      "노균병")
                    putExtra("confidence", 92)
                    putExtra("latitude",   0.0)
                    putExtra("longitude",  0.0)
                    putExtra("addressDo",  "")
                    putExtra("addressSi",  "")
                }
                startActivity(intent)
            }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = java.io.File(
            requireContext().cacheDir,
            "${cropName}_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: android.net.Uri.fromFile(photoFile)

                    // ── 1) Firebase Storage 업로드 ─────────────────────
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val fileName = "${uid}_${cropName}_${System.currentTimeMillis()}.jpg"
                    val storageRef = Firebase.storage.reference
                        .child("diagnoses/$fileName")

                    storageRef.putFile(savedUri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->

                                // ── 2) 위치 수집 후 ResultActivity 시작 ───
                                getLocationAndStart(downloadUri.toString(), savedUri)
                            }
                        }
                        .addOnFailureListener {
                            // Storage 실패 시 imageUrl 없이라도 진행 (빈 문자열)
                            getLocationAndStart("", savedUri)
                        }
                }

                override fun onError(exc: ImageCaptureException) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "촬영 실패: ${exc.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}