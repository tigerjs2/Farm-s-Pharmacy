package com.example.aos

import android.Manifest
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
                    val savedUri = output.savedUri
                        ?: android.net.Uri.fromFile(photoFile)

                    // TODO: 실제 AI 추론 결과로 교체
                    val diagType   = "DISEASE"         // "NORMAL" | "DISEASE" | "UNKNOWN"
                    val label      = "노균병"
                    val confidence = 92

                    val intent = android.content.Intent(requireContext(), ResultActivity::class.java).apply {
                        putExtra("imageUri",   savedUri.toString())
                        putExtra("cropName",   cropName)
                        putExtra("diagType",   diagType)
                        putExtra("label",      label)
                        putExtra("confidence", confidence)
                    }
                    startActivity(intent)
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