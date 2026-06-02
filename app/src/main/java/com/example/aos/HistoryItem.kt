package com.example.aos

data class HistoryItem(
    val id: Int,
    val docId: String = "",          // Firestore 문서 ID (isHandled 업데이트용)
    val imageResId: Int = 0,
    val imageUri: String? = null,
    val diseaseName: String,
    val confidence: Int,
    val date: String,
    val cropName: String = "",
    val diagType: String = "DISEASE",
    val sickKey: String = "",
    val isTreated: Boolean = false
)