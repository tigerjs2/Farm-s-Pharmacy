package com.example.aos

data class TodoItem(
    var id: String = "",
    val uid: String = "",
    val date: String = "",
    val title: String = "",
    val memo: String = "",
    var isDone: Boolean = false
)
