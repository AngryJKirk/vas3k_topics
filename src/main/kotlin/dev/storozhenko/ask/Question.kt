package dev.storozhenko.ask

data class Question(
    val topic: Topic,
    val title: String,
    val text: String,
    val author: Pair<Long, String>
)
