package dev.storozhenko.ask.models

data class Question(
    val topic: Topic,
    val title: String,
    val text: String,
    val author: Pair<Long, String>
) {

    override fun toString(): String {
        return "Топик: ${topic.topicName}\nЗаголовок: $title\nТекст: $text"
    }
}
