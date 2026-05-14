package com.example.aos

data class TodoItem(
    var id: String = "",
    val userId: String = "",
    val date: String = "",
    val title: String = "",
    val memo: String = "",
    var isDone: Boolean = false
)
