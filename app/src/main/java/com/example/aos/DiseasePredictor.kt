package com.example.aos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min


data class PredictionResult(
	val className: String,
	val confidence: Float,
)


class DiseasePredictor(private val context: Context) {
	companion object {
		private const val DEFAULT_IMAGE_SIZE = 224
		private const val DEFAULT_SAM_INPUT_SIZE = 512
		private val NORM_MEAN_RGB = floatArrayOf(0.485f, 0.456f, 0.406f)
		private val NORM_STD_RGB = floatArrayOf(0.229f, 0.224f, 0.225f)
		private val SAM_MEAN_RGB = floatArrayOf(123.675f, 116.28f, 103.53f)
		private val SAM_STD_RGB = floatArrayOf(58.395f, 57.12f, 57.375f)
	}

	private var secondStageModule: Module? = null
	private var samEncoderModule: Module? = null
	private var samDecoderModule: Module? = null
	private var classNames: List<String> = emptyList()
	private var embeddingShape: IntArray = intArrayOf(5, 512)
	private val embeddingMap: MutableMap<String, FloatArray> = mutableMapOf()

	fun initModels(
		secondStageAsset: String = "second_stage.pte",
		samEncoderAsset: String = "sam2.1_t_encoder.pte",
		samDecoderAsset: String = "sam2.1_t_box_decoder_512.pte",
		classNamesAsset: String = "class_names.json",
		cropsAsset: String = "crops.json",
		embeddingShapeAsset: String = "embedding_shape.json",
		embeddingDirAsset: String = "embeddings",
	) {
		secondStageModule = Module.load(assetFilePath(context, secondStageAsset))
		samEncoderModule = Module.load(assetFilePath(context, samEncoderAsset))
		samDecoderModule = Module.load(assetFilePath(context, samDecoderAsset))
		classNames = loadJsonArray(classNamesAsset)
		embeddingShape = loadEmbeddingShape(embeddingShapeAsset)
		loadEmbeddings(cropsAsset, embeddingDirAsset)
	}

	fun predictWithSam(bitmap: Bitmap, bboxXyxy: FloatArray, cropName: String): PredictionResult {
		val masked = applySamMask(bitmap, bboxXyxy, DEFAULT_SAM_INPUT_SIZE)
		return predictMasked(masked, cropName)
	}

	fun predictMasked(bitmap: Bitmap, cropName: String): PredictionResult {
		val module = secondStageModule ?: throw IllegalStateException("Second-stage module not loaded")
		val cropKey = cropName.trim().lowercase()
		val embedding = embeddingMap[cropKey]
			?: throw IllegalArgumentException("Missing embedding for crop: $cropKey")

		val imageTensor = bitmapToTensor(bitmap, DEFAULT_IMAGE_SIZE)
		val textTensor = Tensor.fromBlob(
			embedding,
			longArrayOf(1, embeddingShape[0].toLong(), embeddingShape[1].toLong()),
		)

		val outputs = module.forward(
			EValue.from(imageTensor),
			EValue.from(textTensor),
		)
		val scores = outputs[0].toTensor().getDataAsFloatArray()
		val probs = softmax(scores)
		val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
		val className = classNames.getOrNull(maxIdx) ?: "unknown"
		return PredictionResult(className, probs[maxIdx])
	}

	private fun bitmapToTensor(bitmap: Bitmap, imageSize: Int): Tensor {
		val resized = if (bitmap.width == imageSize && bitmap.height == imageSize) {
			bitmap
		} else {
			Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
		}

		val floatArray = FloatArray(1 * 3 * imageSize * imageSize)
		val pixels = IntArray(imageSize * imageSize)
		resized.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
		val channelSize = imageSize * imageSize

		for (i in pixels.indices) {
			val c = pixels[i]
			val r = (Color.red(c) / 255.0f - NORM_MEAN_RGB[0]) / NORM_STD_RGB[0]
			val g = (Color.green(c) / 255.0f - NORM_MEAN_RGB[1]) / NORM_STD_RGB[1]
			val b = (Color.blue(c) / 255.0f - NORM_MEAN_RGB[2]) / NORM_STD_RGB[2]

			floatArray[i] = r
			floatArray[i + channelSize] = g
			floatArray[i + channelSize * 2] = b
		}

		return Tensor.fromBlob(floatArray, longArrayOf(1, 3, imageSize.toLong(), imageSize.toLong()))
	}

	private fun bitmapToSamTensor(bitmap: Bitmap, imageSize: Int): Tensor {
		val resized = if (bitmap.width == imageSize && bitmap.height == imageSize) {
			bitmap
		} else {
			Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
		}

		val floatArray = FloatArray(1 * 3 * imageSize * imageSize)
		val pixels = IntArray(imageSize * imageSize)
		resized.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
		val channelSize = imageSize * imageSize

		for (i in pixels.indices) {
			val c = pixels[i]
			val r = (Color.red(c).toFloat() - SAM_MEAN_RGB[0]) / SAM_STD_RGB[0]
			val g = (Color.green(c).toFloat() - SAM_MEAN_RGB[1]) / SAM_STD_RGB[1]
			val b = (Color.blue(c).toFloat() - SAM_MEAN_RGB[2]) / SAM_STD_RGB[2]

			floatArray[i] = r
			floatArray[i + channelSize] = g
			floatArray[i + channelSize * 2] = b
		}

		return Tensor.fromBlob(floatArray, longArrayOf(1, 3, imageSize.toLong(), imageSize.toLong()))
	}

	private fun applySamMask(bitmap: Bitmap, bboxXyxy: FloatArray, samInputSize: Int): Bitmap {
		val encoder = samEncoderModule ?: throw IllegalStateException("SAM encoder not loaded")
		val decoder = samDecoderModule ?: throw IllegalStateException("SAM decoder not loaded")

		val samTensor = bitmapToSamTensor(bitmap, samInputSize)
		val encoderOutputs = encoder.forward(EValue.from(samTensor))
		val (imageEmbed, featS0, featS1) = selectSamFeatures(encoderOutputs, samInputSize)
		val scaledBox = scaleBbox(bboxXyxy, bitmap.width, bitmap.height, samInputSize, samInputSize)
		val boxTensor = Tensor.fromBlob(scaledBox, longArrayOf(1, 1, 4))

		val decoderOutputs = decoder.forward(
			EValue.from(imageEmbed),
			EValue.from(featS0),
			EValue.from(featS1),
			EValue.from(boxTensor),
		)
		val maskTensor = decoderOutputs[0].toTensor()
		return applyMaskToBitmap(bitmap, maskTensor)
	}

	private fun selectSamFeatures(outputs: Array<EValue>, samInputSize: Int): Triple<Tensor, Tensor, Tensor> {
		val tensors = outputs.map { it.toTensor() }
		var imageEmbed: Tensor? = null
		var featS0: Tensor? = null
		var featS1: Tensor? = null
		val expectedS0 = if (samInputSize >= 1024) 256 else 128
		val expectedS1 = if (samInputSize >= 1024) 128 else 64

		for (t in tensors) {
			val shape = t.shape()
			if (shape.size == 4) {
				val h = shape[2].toInt()
				val w = shape[3].toInt()
				if (h == expectedS0 && w == expectedS0) {
					featS0 = t
					continue
				}
				if (h == expectedS1 && w == expectedS1) {
					featS1 = t
					continue
				}
			}
			if (imageEmbed == null) {
				imageEmbed = t
			}
		}

		val embed = imageEmbed ?: throw IllegalStateException("SAM encoder output missing image embedding")
		val s0 = featS0 ?: throw IllegalStateException("SAM encoder output missing ${expectedS0}x${expectedS0} feature")
		val s1 = featS1 ?: throw IllegalStateException("SAM encoder output missing ${expectedS1}x${expectedS1} feature")
		return Triple(embed, s0, s1)
	}

	private fun applyMaskToBitmap(bitmap: Bitmap, maskTensor: Tensor): Bitmap {
		val shape = maskTensor.shape()
		if (shape.size < 2) {
			throw IllegalStateException("Invalid mask tensor shape")
		}
		val h = shape[shape.size - 2].toInt()
		val w = shape[shape.size - 1].toInt()
		val mask = maskTensor.getDataAsFloatArray()
		val maskPixels = IntArray(w * h)
		for (i in maskPixels.indices) {
			val keep = mask.getOrNull(i)?.let { it > 0f } ?: false
			maskPixels[i] = if (keep) Color.WHITE else Color.BLACK
		}
		val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
		maskBitmap.setPixels(maskPixels, 0, w, 0, 0, w, h)
		val scaledMask = Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, false)

		val srcPixels = IntArray(bitmap.width * bitmap.height)
		val outPixels = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(srcPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val maskScaled = IntArray(bitmap.width * bitmap.height)
		scaledMask.getPixels(maskScaled, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val bg = Color.rgb(128, 128, 128)

		for (i in srcPixels.indices) {
			val keep = Color.red(maskScaled[i]) > 0
			outPixels[i] = if (keep) srcPixels[i] else bg
		}

		val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
		out.setPixels(outPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		return out
	}

	private fun scaleBbox(
		bbox: FloatArray,
		srcW: Int,
		srcH: Int,
		dstW: Int,
		dstH: Int,
	): FloatArray {
		val scaleX = dstW.toFloat() / max(srcW, 1)
		val scaleY = dstH.toFloat() / max(srcH, 1)
		val x1 = bbox[0] * scaleX
		val y1 = bbox[1] * scaleY
		val x2 = bbox[2] * scaleX
		val y2 = bbox[3] * scaleY
		return floatArrayOf(
			min(max(x1, 0f), dstW.toFloat() - 1f),
			min(max(y1, 0f), dstH.toFloat() - 1f),
			min(max(x2, 0f), dstW.toFloat()),
			min(max(y2, 0f), dstH.toFloat()),
		)
	}

	private fun softmax(logits: FloatArray): FloatArray {
		val maxLogit = logits.maxOrNull() ?: 0f
		val expValues = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
		val sum = expValues.sum().coerceAtLeast(1e-8f)
		return expValues.map { it / sum }.toFloatArray()
	}

	private fun loadEmbeddings(cropsAsset: String, embeddingDirAsset: String) {
		val crops = loadJsonArray(cropsAsset)
		for (crop in crops) {
			val path = "$embeddingDirAsset/$crop.npy"
			val (data, shape) = loadNpyFloat(path)
			if (shape.size != 2) {
				throw IllegalStateException("Invalid embedding shape in $path")
			}
			embeddingMap[crop.lowercase()] = data
		}
	}

	private fun loadJsonArray(assetName: String): List<String> {
		val text = readAssetText(assetName)
		val array = JSONArray(text)
		val list = ArrayList<String>(array.length())
		for (i in 0 until array.length()) {
			list.add(array.getString(i))
		}
		return list
	}

	private fun loadEmbeddingShape(assetName: String): IntArray {
		val text = readAssetText(assetName)
		val obj = JSONObject(text)
		val numQueries = obj.getInt("num_queries")
		val embedDim = obj.getInt("embed_dim")
		return intArrayOf(numQueries, embedDim)
	}

	private fun loadNpyFloat(assetName: String): Pair<FloatArray, IntArray> {
		val bytes = readAssetBytes(assetName)
		val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
		val magic = ByteArray(6)
		buffer.get(magic)
		val versionMajor = buffer.get().toInt()
		buffer.get()
		val headerLen = if (versionMajor <= 1) {
			buffer.short.toInt() and 0xFFFF
		} else {
			buffer.int
		}
		val headerBytes = ByteArray(headerLen)
		buffer.get(headerBytes)
		val header = String(headerBytes)

		if (!header.contains("'<f4'")) {
			throw IllegalStateException("Only float32 little-endian npy is supported")
		}

		val shape = parseShape(header)
		val count = shape.fold(1) { acc, v -> acc * v }
		val out = FloatArray(count)
		for (i in 0 until count) {
			out[i] = buffer.float
		}
		return Pair(out, shape)
	}

	private fun parseShape(header: String): IntArray {
		val shapeRegex = "\\((.*?)\\)".toRegex()
		val match = shapeRegex.find(header) ?: throw IllegalStateException("Shape not found in npy header")
		val inside = match.groupValues.getOrNull(1) ?: ""
		val parts = inside.split(",").mapNotNull {
			val trimmed = it.trim()
			if (trimmed.isEmpty()) null else trimmed.toIntOrNull()
		}
		return parts.toIntArray()
	}

	private fun readAssetText(assetName: String): String {
		return readAssetBytes(assetName).toString(Charsets.UTF_8)
	}

	private fun readAssetBytes(assetName: String): ByteArray {
		context.assets.open(assetName).use { input ->
			return readAllBytes(input)
		}
	}

	private fun readAllBytes(input: InputStream): ByteArray {
		val buffer = ByteArrayOutputStream()
		val data = ByteArray(4096)
		while (true) {
			val count = input.read(data)
			if (count <= 0) break
			buffer.write(data, 0, count)
		}
		return buffer.toByteArray()
	}

	private fun assetFilePath(context: Context, assetName: String): String {
		val file = context.getFileStreamPath(assetName)
		if (file.exists() && file.length() > 0) {
			return file.absolutePath
		}
		context.assets.open(assetName).use { input ->
			file.outputStream().use { output ->
				input.copyTo(output)
			}
		}
		return file.absolutePath
	}
}
