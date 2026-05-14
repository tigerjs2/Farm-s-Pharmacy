package com.example.aos

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class TodoItem(
    @get:Exclude
    @set:Exclude
    var id: String = "",

    val userId: String = "",
    val date: String = "",
    val title: String = "",
    val memo: String = "",

    @get:PropertyName("isDone")
    @set:PropertyName("isDone")
    var isDone: Boolean = false
)
