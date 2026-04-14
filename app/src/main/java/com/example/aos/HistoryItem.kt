package com.example.aos

data class HistoryItem(
    val id: Int,
    val imageResId: Int = 0,
    val imageUri: String? = null,
    val diseaseName: String,
    val confidence: Int,
    val date: String,
    val cropName: String = "",
    val diagType: String = "DISEASE"
)