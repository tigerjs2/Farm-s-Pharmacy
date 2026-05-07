package com.example.aos

data class TodoItem(
    val title: String = "",
    val memo: String = "",
    var isDone: Boolean = false
)