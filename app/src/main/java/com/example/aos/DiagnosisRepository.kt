package com.example.aos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Diagnosis(
    val docId: String,
    val userId: String,
    val cropType: String,
    val diseaseName: String,
    val diagType: String,
    val confidencePercent: Int,
    val imageUrl: String,
    val storagePath: String,
    val sickKey: String,
    val timestampMillis: Long,
    val isHandled: Boolean
)

object DiagnosisRepository {

    private const val COLLECTION = "Diagnoses"

    /**
     * 현재 로그인 사용자의 Diagnoses를 Firestore에서 불러오면서,
     * Storage에 원본 이미지가 사라진 문서는 Firestore에서 함께 삭제한다.
     */
    suspend fun syncAndLoadForCurrentUser(): List<Diagnosis> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        return syncAndLoadForUser(uid)
    }

    suspend fun syncAndLoadForUser(uid: String): List<Diagnosis> = withContext(Dispatchers.IO) {
        val snapshot = try {
            FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .whereEqualTo("userId", uid)
                .get()
                .await()
        } catch (e: Exception) {
            return@withContext emptyList<Diagnosis>()
        }

        coroutineScope {
            snapshot.documents.map { doc ->
                async {
                    val diagnosis = doc.toDiagnosisOrNull() ?: return@async null

                    val storagePath = diagnosis.storagePath
                        .ifBlank { extractStoragePathFromUrl(diagnosis.imageUrl) }

                    if (storagePath.isBlank()) {
                        return@async diagnosis
                    }

                    val exists = storageObjectExists(storagePath)
                    if (!exists) {
                        runCatching {
                            FirebaseFirestore.getInstance()
                                .collection(COLLECTION)
                                .document(doc.id)
                                .delete()
                                .await()
                        }
                        null
                    } else {
                        diagnosis
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Firestore의 isHandled 필드를 업데이트한다.
     * docId를 직접 사용하므로 빠르고 정확하다.
     */
    suspend fun updateHandled(docId: String, isHandled: Boolean) {
        if (docId.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection(COLLECTION)
                    .document(docId)
                    .update("isHandled", isHandled)
                    .await()
            }
        }
    }

    private suspend fun storageObjectExists(path: String): Boolean {
        return try {
            FirebaseStorage.getInstance().reference.child(path).metadata.await()
            true
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) false
            else true
        } catch (e: Exception) {
            true
        }
    }

    fun extractStoragePathFromUrl(url: String): String {
        if (url.isBlank()) return ""
        val marker = "/o/"
        val idx = url.indexOf(marker)
        if (idx < 0) return ""
        val after = url.substring(idx + marker.length)
        val pathPart = after.substringBefore("?")
        return try {
            URLDecoder.decode(pathPart, "UTF-8")
        } catch (e: Exception) {
            pathPart
        }
    }

    fun storagePathFor(uid: String, timestampMillis: Long): String =
        "diagnoses/$uid/$timestampMillis.jpg"

    private fun com.google.firebase.firestore.DocumentSnapshot.toDiagnosisOrNull(): Diagnosis? {
        val userId = getString("userId") ?: return null
        val timestamp = getTimestamp("timestamp")?.toDate()?.time ?: return null
        val confidenceDouble = getDouble("confidence") ?: 0.0
        return Diagnosis(
            docId = id,
            userId = userId,
            cropType = getString("cropType") ?: "",
            diseaseName = getString("diseaseName") ?: "",
            diagType = getString("diagType") ?: inferDiagType(getString("diseaseName")),
            confidencePercent = (confidenceDouble * 100).toInt(),
            imageUrl = getString("imageUrl") ?: "",
            storagePath = getString("storagePath") ?: "",
            sickKey = getString("sickKey") ?: "",
            timestampMillis = timestamp,
            isHandled = getBoolean("isHandled") ?: false
        )
    }

    private fun inferDiagType(diseaseName: String?): String {
        val name = diseaseName?.trim().orEmpty()
        return when {
            name.isEmpty() -> "UNKNOWN"
            name == "정상" -> "NORMAL"
            else -> "DISEASE"
        }
    }

    /** UI 레이어(HistoryItem/CalendarHistoryStats)와 호환되는 변환. */
    fun Diagnosis.toHistoryItem(): HistoryItem {
        val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            .format(Date(timestampMillis))
        return HistoryItem(
            id = timestampMillis.toInt(),
            docId = docId,              // Firestore 문서 ID 전달
            imageUri = imageUrl,
            diseaseName = diseaseName,
            confidence = confidencePercent,
            date = dateStr,
            cropName = cropType,
            diagType = diagType,
            sickKey = sickKey,
            isTreated = isHandled
        )
    }
}